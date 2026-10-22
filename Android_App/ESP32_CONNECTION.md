# ESP32 room-light connection

The Android app sends the two physical room-light commands to the PC relay
server, which forwards them to the ESP32.

## 1. Start the relay server

From `Desktop/Smart_Home_PJ/Smart_Switch/server`, run:

```powershell
python .\server.py --esp32 http://ESP32_IP --port 8080
```

## 2. Configure the Android app

Add the relay PC's LAN address to the project's `local.properties` file:

```properties
roomSwitchServerUrl=http://192.168.0.10:8080
```

Replace the example IP address with the PC's actual IPv4 address. The value is
machine-specific and `local.properties` is not committed to Git. Rebuild and
install the app after changing it.

The Android device, relay PC, and ESP32 must be on the same Wi-Fi network.
Allow incoming TCP port 8080 through the PC firewall when required.

## Device mapping

| App device | Relay request | ESP32 servo |
| --- | --- | --- |
| Room light 1 | `switch=room-light-1` | Servo 1 / GPIO 18 |
| Room light 2 | `switch=room-light-2` | Servo 2 / GPIO 19 |

The app requests `/api/room/status` on startup to synchronize those two light
states. Its first two room-light toggles and the all-lights action send real
commands; the remaining sample devices are still UI-only mock devices.
