# ESP32 스마트 스위치

ESP32 WROOM과 서보모터를 Wi-Fi 웹 API로 제어하는 ESP-IDF 프로젝트입니다.

## 배선

| 서보모터 | ESP32 |
| --- | --- |
| 서보 1 신호선 | GPIO 18 |
| 서보 2 신호선 | GPIO 19 |
| 전원 | 별도 5 V 전원 |
| GND | ESP32 GND와 외부 전원 GND를 함께 연결 |

서보모터 전원을 ESP32의 3.3 V 핀에서 공급하지 마세요.

## 설정 및 빌드

1. VS Code에서 이 폴더를 엽니다.
2. `Ctrl+Shift+P` → `Tasks: Run Task` → `ESP-IDF: Build`를 실행합니다.
3. 처음 생성된 `sdkconfig`에서 Wi-Fi 정보를 바꾸려면 ESP-IDF 터미널에서 `idf.py menuconfig`를 실행하고 `Smart switch settings`를 선택합니다.

터미널 빌드:

```powershell
.\scripts\build.ps1
```

업로드(예: COM3):

```powershell
.\scripts\build.ps1 -Port COM3
```

시리얼 모니터:

```powershell
.\scripts\monitor.ps1 -Port COM3
```

## 앱 연동 API

ESP32가 Wi-Fi에 연결된 뒤 시리얼 모니터에 표시되는 IP로 요청합니다.

- `GET /api/status` : 두 서보의 현재 상태
- `GET /api/switch?servo=1&state=on` : 지정 서보 켜기
- `GET /api/switch?servo=2&state=off` : 지정 서보 끄기
- `GET /api/servo?servo=1&angle=0..180` : 지정 서보 각도 직접 지정

브라우저에서 `http://ESP32_IP/`에 접속하면 테스트용 제어 화면이 나타납니다.
