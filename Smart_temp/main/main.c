#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "driver/i2c_master.h"
#include "esp_check.h"
#include "esp_err.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"

/*
 * 배선
 * ESP32 GPIO21 -> OLED SDA, ENS160+AHT21 SDA
 * ESP32 GPIO22 -> OLED SCL, ENS160+AHT21 SCL
 * ESP32 3V3    -> OLED VCC, ENS160+AHT21 VCC
 * ESP32 GND    -> OLED GND, ENS160+AHT21 GND
 */
#define I2C_PORT            I2C_NUM_0
#define I2C_SDA_GPIO        GPIO_NUM_21
#define I2C_SCL_GPIO        GPIO_NUM_22
#define I2C_FREQ_HZ         400000
#define I2C_TIMEOUT_MS      1000

#define OLED_I2C_ADDRESS    0x3C
#define AHT21_I2C_ADDRESS   0x38

#define OLED_WIDTH          128
#define OLED_HEIGHT         64
#define OLED_BUFFER_SIZE    (OLED_WIDTH * OLED_HEIGHT / 8)

static const char *TAG = "smart_temp";
static uint8_t oled_buffer[OLED_BUFFER_SIZE];
static i2c_master_bus_handle_t i2c_bus;
static i2c_master_dev_handle_t oled_device;
static i2c_master_dev_handle_t aht21_device;

static esp_err_t i2c_write(i2c_master_dev_handle_t device, const uint8_t *data, size_t length)
{
    return i2c_master_transmit(device, data, length, I2C_TIMEOUT_MS);
}

static esp_err_t i2c_init(void)
{
    const i2c_master_bus_config_t bus_config = {
        .i2c_port = I2C_PORT,
        .sda_io_num = I2C_SDA_GPIO,
        .scl_io_num = I2C_SCL_GPIO,
        .clk_source = I2C_CLK_SRC_DEFAULT,
        .glitch_ignore_cnt = 7,
        .flags.enable_internal_pullup = true,
    };
    const i2c_device_config_t oled_config = {
        .dev_addr_length = I2C_ADDR_BIT_LEN_7,
        .device_address = OLED_I2C_ADDRESS,
        .scl_speed_hz = I2C_FREQ_HZ,
    };
    const i2c_device_config_t aht21_config = {
        .dev_addr_length = I2C_ADDR_BIT_LEN_7,
        .device_address = AHT21_I2C_ADDRESS,
        .scl_speed_hz = I2C_FREQ_HZ,
    };

    ESP_RETURN_ON_ERROR(i2c_new_master_bus(&bus_config, &i2c_bus), TAG, "I2C bus init failed");
    ESP_RETURN_ON_ERROR(i2c_master_bus_add_device(i2c_bus, &oled_config, &oled_device),
                        TAG, "OLED device init failed");
    return i2c_master_bus_add_device(i2c_bus, &aht21_config, &aht21_device);
}

static esp_err_t oled_command(uint8_t command)
{
    const uint8_t data[] = {0x00, command};
    return i2c_write(oled_device, data, sizeof(data));
}

static esp_err_t oled_init(void)
{
    const uint8_t init_commands[] = {
        0xAE,       // display off
        0xD5, 0x80, // clock divide
        0xA8, 0x3F, // multiplex ratio: 64 rows
        0xD3, 0x00, // display offset
        0x40,       // start line
        0x8D, 0x14, // charge pump on
        0x20, 0x00, // horizontal memory addressing
        0xA1,       // segment remap
        0xC8,       // COM scan direction remapped
        0xDA, 0x12, // COM pins for 128x64
        0x81, 0xCF, // contrast
        0xD9, 0xF1, // pre-charge
        0xDB, 0x40, // VCOM detect
        0xA4,       // use RAM content
        0xA6,       // normal (not inverted)
        0xAF        // display on
    };

    for (size_t i = 0; i < sizeof(init_commands); ++i) {
        ESP_RETURN_ON_ERROR(oled_command(init_commands[i]), TAG, "OLED init failed");
    }
    return ESP_OK;
}

static void oled_clear(void)
{
    memset(oled_buffer, 0, sizeof(oled_buffer));
}

static void oled_pixel(int x, int y, bool on)
{
    if (x < 0 || x >= OLED_WIDTH || y < 0 || y >= OLED_HEIGHT) {
        return;
    }

    const size_t index = (size_t)x + ((size_t)y / 8U) * OLED_WIDTH;
    const uint8_t bit = (uint8_t)(1U << (y & 7));
    if (on) {
        oled_buffer[index] |= bit;
    } else {
        oled_buffer[index] &= (uint8_t)~bit;
    }
}

static void oled_filled_rect(int x, int y, int width, int height)
{
    for (int py = y; py < y + height; ++py) {
        for (int px = x; px < x + width; ++px) {
            oled_pixel(px, py, true);
        }
    }
}

static esp_err_t oled_show(void)
{
    const uint8_t address_commands[] = {0x21, 0, OLED_WIDTH - 1, 0x22, 0, 7};
    for (size_t i = 0; i < sizeof(address_commands); ++i) {
        ESP_RETURN_ON_ERROR(oled_command(address_commands[i]), TAG, "OLED address failed");
    }

    // Control byte 0x40 + 16 bytes per I2C transfer. Keeping transfers small
    // works reliably with common SSD1306 modules.
    uint8_t transfer[17] = {0x40};
    for (size_t offset = 0; offset < sizeof(oled_buffer); offset += 16) {
        memcpy(&transfer[1], &oled_buffer[offset], 16);
        ESP_RETURN_ON_ERROR(i2c_write(oled_device, transfer, sizeof(transfer)),
                            TAG, "OLED data write failed");
    }
    return ESP_OK;
}

/* Seven-segment renderer: sufficient for temperature and relative humidity. */
enum {
    SEG_A = 1 << 0, SEG_B = 1 << 1, SEG_C = 1 << 2, SEG_D = 1 << 3,
    SEG_E = 1 << 4, SEG_F = 1 << 5, SEG_G = 1 << 6,
};

static uint8_t digit_segments(int digit)
{
    static const uint8_t map[] = {
        SEG_A | SEG_B | SEG_C | SEG_D | SEG_E | SEG_F,
        SEG_B | SEG_C,
        SEG_A | SEG_B | SEG_D | SEG_E | SEG_G,
        SEG_A | SEG_B | SEG_C | SEG_D | SEG_G,
        SEG_B | SEG_C | SEG_F | SEG_G,
        SEG_A | SEG_C | SEG_D | SEG_F | SEG_G,
        SEG_A | SEG_C | SEG_D | SEG_E | SEG_F | SEG_G,
        SEG_A | SEG_B | SEG_C,
        SEG_A | SEG_B | SEG_C | SEG_D | SEG_E | SEG_F | SEG_G,
        SEG_A | SEG_B | SEG_C | SEG_D | SEG_F | SEG_G,
    };
    return (digit >= 0 && digit <= 9) ? map[digit] : 0;
}

static void draw_digit(int x, int y, int scale, int digit)
{
    const uint8_t segments = digit_segments(digit);
    const int length = scale * 3;
    const int thickness = scale;
    if (segments & SEG_A) oled_filled_rect(x + thickness, y, length, thickness);
    if (segments & SEG_B) oled_filled_rect(x + length + thickness, y + thickness, thickness, length);
    if (segments & SEG_C) oled_filled_rect(x + length + thickness, y + length + 2 * thickness, thickness, length);
    if (segments & SEG_D) oled_filled_rect(x + thickness, y + 2 * length + 2 * thickness, length, thickness);
    if (segments & SEG_E) oled_filled_rect(x, y + length + 2 * thickness, thickness, length);
    if (segments & SEG_F) oled_filled_rect(x, y + thickness, thickness, length);
    if (segments & SEG_G) oled_filled_rect(x + thickness, y + length + thickness, length, thickness);
}

static void draw_number(int x, int y, int scale, int value, bool decimal)
{
    char text[8];
    snprintf(text, sizeof(text), "%d", value);
    // 실제 세그먼트 폭(4 * scale)보다 한 칸 더 넓게 배치해
    // 2와 7처럼 이웃 숫자가 붙어 보이는 현상을 방지한다.
    const int digit_width = scale * 6;
    for (size_t i = 0; i < strlen(text); ++i) {
        draw_digit(x + (int)i * digit_width, y, scale, text[i] - '0');
    }
    if (decimal) {
        oled_filled_rect(x + (int)strlen(text) * digit_width, y + scale * 7, scale, scale);
    }
}

static void draw_celsius_symbol(int x, int y, int scale)
{
    // degree sign
    oled_filled_rect(x, y, scale * 2, scale);
    oled_filled_rect(x, y + scale, scale, scale);
    oled_filled_rect(x + scale, y + scale * 2, scale, scale);
    // C
    oled_filled_rect(x + scale * 4, y, scale * 3, scale);
    oled_filled_rect(x + scale * 3, y + scale, scale, scale * 5);
    oled_filled_rect(x + scale * 4, y + scale * 6, scale * 3, scale);
}

static esp_err_t aht21_init(void)
{
    const uint8_t soft_reset = 0xBA;
    ESP_RETURN_ON_ERROR(i2c_write(aht21_device, &soft_reset, 1), TAG, "AHT21 reset failed");
    vTaskDelay(pdMS_TO_TICKS(25));

    const uint8_t initialize[] = {0xBE, 0x08, 0x00};
    ESP_RETURN_ON_ERROR(i2c_write(aht21_device, initialize, sizeof(initialize)),
                        TAG, "AHT21 calibration command failed");
    vTaskDelay(pdMS_TO_TICKS(10));
    return ESP_OK;
}

static esp_err_t aht21_read(float *temperature_c, float *humidity_rh)
{
    const uint8_t trigger[] = {0xAC, 0x33, 0x00};
    uint8_t data[7];
    ESP_RETURN_ON_ERROR(i2c_write(aht21_device, trigger, sizeof(trigger)),
                        TAG, "AHT21 measurement trigger failed");
    vTaskDelay(pdMS_TO_TICKS(85));

    ESP_RETURN_ON_ERROR(i2c_master_receive(aht21_device, data, sizeof(data),
                                            I2C_TIMEOUT_MS),
                        TAG, "AHT21 read failed");
    if (data[0] & 0x80) {
        return ESP_ERR_INVALID_STATE; // still measuring
    }
    if (!(data[0] & 0x08)) {
        return ESP_ERR_INVALID_STATE; // not calibrated
    }

    const uint32_t raw_humidity = ((uint32_t)data[1] << 12) |
                                  ((uint32_t)data[2] << 4) |
                                  ((uint32_t)data[3] >> 4);
    const uint32_t raw_temperature = ((uint32_t)(data[3] & 0x0F) << 16) |
                                     ((uint32_t)data[4] << 8) | data[5];

    *humidity_rh = (float)raw_humidity * 100.0f / 1048576.0f;
    *temperature_c = (float)raw_temperature * 200.0f / 1048576.0f - 50.0f;
    return ESP_OK;
}

static void draw_readings(float temperature, float humidity)
{
    // 온도: 1/10도 단위, 습도: 정수 단위
    const int temp_tenths = (int)lroundf(temperature * 10.0f);
    const int temp_integer = temp_tenths / 10;
    const int temp_decimal = abs(temp_tenths % 10);
    const int humidity_integer = (int)lroundf(humidity);

    oled_clear();

    // Large display: 23.4 C
    const int temperature_y = 5;
    const int humidity_y = 43;

    draw_number(4, temperature_y, 4, temp_integer, true);
    int text_width = ((temp_integer >= 10) ? 2 : 1) * 24;
    draw_digit(4 + text_width + 4, temperature_y, 4, temp_decimal);
    draw_celsius_symbol(4 + text_width + 27, temperature_y + 4, 3);

    // Lower line: relative humidity, rendered as 7-segment number + percent bars.
    draw_number(4, humidity_y, 2, humidity_integer, false);
    const int humidity_width = (humidity_integer >= 100) ? 30 : (humidity_integer >= 10 ? 20 : 10);
    oled_filled_rect(8 + humidity_width, humidity_y + 2, 4, 4);
    oled_filled_rect(18 + humidity_width, humidity_y + 12, 4, 4);
    for (int i = 0; i < 10; ++i) {
        oled_pixel(12 + humidity_width + i, humidity_y + 16 - i, true);
    }
}

void app_main(void)
{
    ESP_ERROR_CHECK(i2c_init());
    ESP_ERROR_CHECK(oled_init());
    ESP_ERROR_CHECK(aht21_init());

    ESP_LOGI(TAG, "Ready. AHT21=0x%02X, OLED=0x%02X", AHT21_I2C_ADDRESS, OLED_I2C_ADDRESS);

    while (true) {
        float temperature = 0.0f;
        float humidity = 0.0f;
        const esp_err_t result = aht21_read(&temperature, &humidity);

        if (result == ESP_OK) {
            ESP_LOGI(TAG, "Temperature: %.1f C, Humidity: %.1f %%", temperature, humidity);
            draw_readings(temperature, humidity);
            if (oled_show() != ESP_OK) {
                ESP_LOGE(TAG, "OLED write failed");
            }
        } else {
            ESP_LOGE(TAG, "AHT21 read failed: %s", esp_err_to_name(result));
        }
        vTaskDelay(pdMS_TO_TICKS(2000));
    }
}
