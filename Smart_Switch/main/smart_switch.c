#include <stdbool.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "driver/ledc.h"
#include "esp_event.h"
#include "esp_http_server.h"
#include "esp_log.h"
#include "esp_netif.h"
#include "esp_wifi.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "nvs_flash.h"

#define SERVO_COUNT 2
#define SERVO_MIN_PULSE_US 500
#define SERVO_MAX_PULSE_US 2500
#define SERVO_FREQUENCY_HZ 50
#define SERVO_DUTY_RESOLUTION LEDC_TIMER_16_BIT

static const char *TAG = "smart_switch";
static const ledc_channel_t s_channels[SERVO_COUNT] = {LEDC_CHANNEL_0, LEDC_CHANNEL_1};
static int s_angles[SERVO_COUNT];
/* The servo returns to neutral after a press, so retain the requested light
 * state separately from the physical servo angle. */
static bool s_light_on[SERVO_COUNT];

static int servo_on_angle(int servo)
{
    return servo == 0 ? CONFIG_SMART_SWITCH_ON_ANGLE : CONFIG_SMART_SWITCH_SERVO2_ON_ANGLE;
}

static int servo_off_angle(int servo)
{
    return servo == 0 ? CONFIG_SMART_SWITCH_OFF_ANGLE : CONFIG_SMART_SWITCH_SERVO2_OFF_ANGLE;
}

static int servo_neutral_angle(int servo)
{
    return servo == 0 ? CONFIG_SMART_SWITCH_NEUTRAL_ANGLE :
                        CONFIG_SMART_SWITCH_SERVO2_NEUTRAL_ANGLE;
}

static uint32_t servo_pulse_to_duty(uint32_t pulse_us)
{
    const uint32_t max_duty = (1U << 16) - 1U;
    return (pulse_us * max_duty * SERVO_FREQUENCY_HZ) / 1000000U;
}

static void servo_set_angle(int servo, int angle)
{
    if (angle < 0) {
        angle = 0;
    } else if (angle > 180) {
        angle = 180;
    }

    uint32_t pulse_us = SERVO_MIN_PULSE_US +
        ((SERVO_MAX_PULSE_US - SERVO_MIN_PULSE_US) * (uint32_t)angle / 180U);
    ESP_ERROR_CHECK(ledc_set_duty(LEDC_LOW_SPEED_MODE, s_channels[servo],
                                  servo_pulse_to_duty(pulse_us)));
    ESP_ERROR_CHECK(ledc_update_duty(LEDC_LOW_SPEED_MODE, s_channels[servo]));
    s_angles[servo] = angle;
    ESP_LOGI(TAG, "Servo %d angle: %d degrees", servo + 1, angle);
}

static void servo_press_switch(int servo, int angle)
{
    servo_set_angle(servo, angle);
    vTaskDelay(pdMS_TO_TICKS(CONFIG_SMART_SWITCH_ACTUATION_HOLD_MS));
    servo_set_angle(servo, servo_neutral_angle(servo));
}

static void init_servos(void)
{
    const int pins[SERVO_COUNT] = {
        CONFIG_SMART_SWITCH_SERVO_GPIO,
        CONFIG_SMART_SWITCH_SERVO2_GPIO,
    };
    ledc_timer_config_t timer_config = {
        .speed_mode = LEDC_LOW_SPEED_MODE,
        .duty_resolution = SERVO_DUTY_RESOLUTION,
        .timer_num = LEDC_TIMER_0,
        .freq_hz = SERVO_FREQUENCY_HZ,
        .clk_cfg = LEDC_AUTO_CLK,
    };
    ESP_ERROR_CHECK(ledc_timer_config(&timer_config));

    for (int servo = 0; servo < SERVO_COUNT; ++servo) {
        ledc_channel_config_t channel_config = {
            .gpio_num = pins[servo],
            .speed_mode = LEDC_LOW_SPEED_MODE,
            .channel = s_channels[servo],
            .intr_type = LEDC_INTR_DISABLE,
            .timer_sel = LEDC_TIMER_0,
            .duty = 0,
            .hpoint = 0,
        };
        ESP_ERROR_CHECK(ledc_channel_config(&channel_config));
        servo_set_angle(servo, servo_neutral_angle(servo));
    }
}

static const char *servo_state(int servo)
{
    return s_light_on[servo] ? "on" : "off";
}

static esp_err_t send_json_status(httpd_req_t *req)
{
    char response[192];
    snprintf(response, sizeof(response),
             "{\"servo1\":{\"angle\":%d,\"state\":\"%s\"},"
             "\"servo2\":{\"angle\":%d,\"state\":\"%s\"}}",
             s_angles[0], servo_state(0), s_angles[1], servo_state(1));
    httpd_resp_set_type(req, "application/json");
    return httpd_resp_sendstr(req, response);
}

static esp_err_t status_handler(httpd_req_t *req)
{
    return send_json_status(req);
}

static bool query_servo(httpd_req_t *req, char *query, size_t query_size, int *servo)
{
    char value[4];
    if (httpd_req_get_url_query_str(req, query, query_size) != ESP_OK ||
        httpd_query_key_value(query, "servo", value, sizeof(value)) != ESP_OK) {
        httpd_resp_send_err(req, HTTPD_400_BAD_REQUEST, "Add servo=1 or servo=2");
        return false;
    }
    char *end = NULL;
    long number = strtol(value, &end, 10);
    if (*value == '\0' || *end != '\0' || number < 1 || number > SERVO_COUNT) {
        httpd_resp_send_err(req, HTTPD_400_BAD_REQUEST, "Servo must be 1 or 2");
        return false;
    }
    *servo = (int)number - 1;
    return true;
}

static esp_err_t servo_handler(httpd_req_t *req)
{
    char query[64];
    char value[8];
    int servo;
    if (!query_servo(req, query, sizeof(query), &servo)) {
        return ESP_FAIL;
    }
    if (httpd_query_key_value(query, "angle", value, sizeof(value)) != ESP_OK) {
        httpd_resp_send_err(req, HTTPD_400_BAD_REQUEST,
                            "Use /api/servo?servo=1&angle=0..180");
        return ESP_FAIL;
    }
    char *end = NULL;
    long angle = strtol(value, &end, 10);
    if (*value == '\0' || *end != '\0' || angle < 0 || angle > 180) {
        httpd_resp_send_err(req, HTTPD_400_BAD_REQUEST, "Angle must be 0..180");
        return ESP_FAIL;
    }
    servo_set_angle(servo, (int)angle);
    return send_json_status(req);
}

static esp_err_t switch_handler(httpd_req_t *req)
{
    char query[64];
    char state[8];
    int servo;
    if (!query_servo(req, query, sizeof(query), &servo)) {
        return ESP_FAIL;
    }
    if (httpd_query_key_value(query, "state", state, sizeof(state)) != ESP_OK) {
        httpd_resp_send_err(req, HTTPD_400_BAD_REQUEST,
                            "Use /api/switch?servo=1&state=on|off");
        return ESP_FAIL;
    }
    if (strcmp(state, "on") == 0) {
        servo_press_switch(servo, servo_on_angle(servo));
        s_light_on[servo] = true;
    } else if (strcmp(state, "off") == 0) {
        servo_press_switch(servo, servo_off_angle(servo));
        s_light_on[servo] = false;
    } else {
        httpd_resp_send_err(req, HTTPD_400_BAD_REQUEST, "State must be on or off");
        return ESP_FAIL;
    }
    return send_json_status(req);
}

static void start_webserver(void)
{
    httpd_handle_t server = NULL;
    httpd_config_t config = HTTPD_DEFAULT_CONFIG();
    ESP_ERROR_CHECK(httpd_start(&server, &config));
    const httpd_uri_t status = {.uri = "/api/status", .method = HTTP_GET, .handler = status_handler};
    const httpd_uri_t servo = {.uri = "/api/servo", .method = HTTP_GET, .handler = servo_handler};
    const httpd_uri_t sw = {.uri = "/api/switch", .method = HTTP_GET, .handler = switch_handler};
    ESP_ERROR_CHECK(httpd_register_uri_handler(server, &status));
    ESP_ERROR_CHECK(httpd_register_uri_handler(server, &servo));
    ESP_ERROR_CHECK(httpd_register_uri_handler(server, &sw));
}

static void wifi_event_handler(void *arg, esp_event_base_t event_base,
                               int32_t event_id, void *event_data)
{
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        ESP_ERROR_CHECK(esp_wifi_connect());
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
        const wifi_event_sta_disconnected_t *event =
            (const wifi_event_sta_disconnected_t *)event_data;
        ESP_LOGW(TAG, "Wi-Fi disconnected (reason=%d); retrying", event->reason);
        ESP_ERROR_CHECK(esp_wifi_connect());
    } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
        const ip_event_got_ip_t *event = (const ip_event_got_ip_t *)event_data;
        ESP_LOGI(TAG, "Open http://" IPSTR " in a browser", IP2STR(&event->ip_info.ip));
        start_webserver();
    }
}

static void init_wifi(void)
{
    ESP_ERROR_CHECK(esp_netif_init());
    ESP_ERROR_CHECK(esp_event_loop_create_default());
    esp_netif_create_default_wifi_sta();
    wifi_init_config_t init_config = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&init_config));
    ESP_ERROR_CHECK(esp_event_handler_instance_register(WIFI_EVENT, ESP_EVENT_ANY_ID,
                                                         &wifi_event_handler, NULL, NULL));
    ESP_ERROR_CHECK(esp_event_handler_instance_register(IP_EVENT, IP_EVENT_STA_GOT_IP,
                                                         &wifi_event_handler, NULL, NULL));
    wifi_config_t wifi_config = {0};
    strlcpy((char *)wifi_config.sta.ssid, CONFIG_SMART_SWITCH_WIFI_SSID,
            sizeof(wifi_config.sta.ssid));
    strlcpy((char *)wifi_config.sta.password, CONFIG_SMART_SWITCH_WIFI_PASSWORD,
            sizeof(wifi_config.sta.password));
    wifi_config.sta.threshold.authmode = WIFI_AUTH_WPA2_PSK;
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &wifi_config));
    ESP_ERROR_CHECK(esp_wifi_start());
}

void app_main(void)
{
    esp_err_t nvs_result = nvs_flash_init();
    if (nvs_result == ESP_ERR_NVS_NO_FREE_PAGES || nvs_result == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        nvs_result = nvs_flash_init();
    }
    ESP_ERROR_CHECK(nvs_result);
    init_servos();
    init_wifi();
}
