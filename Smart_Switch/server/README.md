# 내부망 방 스위치 서버

ESP32의 제어 API를 호출하고, `위 스위치`와 `아래 스위치` 이름으로 웹 화면을 제공합니다.

## 실행

먼저 ESP32 시리얼 로그에서 IP 주소를 확인한 뒤 서버 PC에서 실행합니다.

```powershell
python .\server.py --esp32 http://ESP32_IP --port 8080
```

브라우저에서 다음 주소로 접속합니다.

```text
http://서버PC_IP:8080
```

서버 PC와 ESP32가 같은 내부망에 있어야 합니다.
