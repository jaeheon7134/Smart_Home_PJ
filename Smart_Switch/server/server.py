"""Internal-network control server for the ESP32 room switch.

Run with:
    python server.py --esp32 http://192.168.0.50 --port 8080
"""

from __future__ import annotations

import argparse
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError, URLError
from urllib.parse import parse_qs, urlencode, urlparse
from urllib.request import Request, urlopen


HTML = """<!doctype html>
<html lang="ko"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>방 스위치</title>
<style>
body{font-family:system-ui,sans-serif;max-width:520px;margin:40px auto;padding:0 16px}
.card{border:1px solid #ddd;border-radius:12px;padding:18px;margin:14px 0}
button{font-size:1rem;padding:10px 18px;margin-right:8px;cursor:pointer}
.state{margin-top:12px;color:#555}.error{color:#b00020}
</style></head><body>
<h1>방 스위치</h1>
<div class="card"><h2>위 스위치</h2>
<button onclick="setSwitch('upper','on')">켜기</button>
<button onclick="setSwitch('upper','off')">끄기</button>
<div id="upper" class="state">확인 중...</div></div>
<div class="card"><h2>아래 스위치</h2>
<button onclick="setSwitch('lower','on')">켜기</button>
<button onclick="setSwitch('lower','off')">끄기</button>
<div id="lower" class="state">확인 중...</div></div>
<script>
async function refresh(){
  try { const r=await fetch('/api/room/status'); const j=await r.json();
    for(const n of ['upper','lower']) document.getElementById(n).textContent=
      `${j[n].angle}도 (${j[n].state})`;
  } catch(e) { for(const n of ['upper','lower']) document.getElementById(n).textContent='ESP32 연결 실패'; }
}
async function setSwitch(which,state){
  const r=await fetch(`/api/room/switch?switch=${which}&state=${state}`);
  if(!r.ok) alert(await r.text());
  refresh();
}
refresh(); setInterval(refresh, 5000);
</script><!-- DEBUG_CONTROLS --></body></html>"""

# Change this one line to False to remove the debug sliders and their API.
ENABLE_DEBUG_CONTROLS = True

DEBUG_CONTROLS_HTML = """
<div class="card"><h2>Debug: direct servo angle</h2>
<p>Upper: <output id="upper-angle">0</output>&deg;</p>
<input id="upper-slider" type="range" min="0" max="180" value="0"
       oninput="setAngleLabel('upper', this.value)" onchange="setAngle('upper', this.value)">
<p>Lower: <output id="lower-angle">0</output>&deg;</p>
<input id="lower-slider" type="range" min="0" max="180" value="0"
       oninput="setAngleLabel('lower', this.value)" onchange="setAngle('lower', this.value)">
</div>
<script>
function setAngleLabel(which, value) {
  document.getElementById(`${which}-angle`).textContent = value;
}
async function setAngle(which, angle) {
  const r = await fetch(`/api/room/servo?switch=${which}&angle=${angle}`);
  if (!r.ok) alert(await r.text());
  refresh();
}
async function refreshDebugSliders() {
  try {
    const r = await fetch('/api/room/status'); const j = await r.json();
    for (const which of ['upper', 'lower']) {
      const angle = j[which].angle;
      document.getElementById(`${which}-slider`).value = angle;
      setAngleLabel(which, angle);
    }
  } catch (e) {}
}
refreshDebugSliders(); setInterval(refreshDebugSliders, 5000);
</script>"""

class Handler(BaseHTTPRequestHandler):
    esp32_base = ""

    def send_json(self, status: int, value: object) -> None:
        body = json.dumps(value, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:
        parsed = urlparse(self.path)
        if parsed.path == "/":
            debug_html = DEBUG_CONTROLS_HTML if ENABLE_DEBUG_CONTROLS else ""
            body = HTML.replace("<!-- DEBUG_CONTROLS -->", debug_html).encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return

        if parsed.path == "/api/room/status":
            self.proxy_status()
            return

        if parsed.path == "/api/room/switch":
            self.proxy_switch(parse_qs(parsed.query))
            return

        if ENABLE_DEBUG_CONTROLS and parsed.path == "/api/room/servo":
            self.proxy_servo(parse_qs(parsed.query))
            return

        self.send_json(404, {"error": "not found"})

    def proxy_status(self) -> None:
        try:
            data = self.call_esp32("/api/status")
            self.send_json(200, {
                "upper": data["servo1"],
                "lower": data["servo2"],
            })
        except Exception as exc:
            self.send_json(502, {"error": f"ESP32 연결 실패: {exc}"})

    def proxy_switch(self, query: dict[str, list[str]]) -> None:
        switch = query.get("switch", [""])[0]
        state = query.get("state", [""])[0]
        # Room light 1 always drives servo 1; room light 2 always drives servo 2.
        # Keep upper/lower aliases so the original browser page remains compatible.
        servo = {
            "room-light-1": "1",
            "light1": "1",
            "upper": "1",
            "room-light-2": "2",
            "light2": "2",
            "lower": "2",
        }.get(switch)
        if servo is None or state not in {"on", "off"}:
            self.send_json(400, {"error": "switch는 upper/lower, state는 on/off여야 합니다."})
            return
        try:
            data = self.call_esp32("/api/switch?" + urlencode({"servo": servo, "state": state}))
            self.send_json(200, data)
        except Exception as exc:
            self.send_json(502, {"error": f"ESP32 연결 실패: {exc}"})

    def proxy_servo(self, query: dict[str, list[str]]) -> None:
        switch = query.get("switch", [""])[0]
        angle = query.get("angle", [""])[0]
        servo = {
            "room-light-1": "1", "light1": "1", "upper": "1",
            "room-light-2": "2", "light2": "2", "lower": "2",
        }.get(switch)
        if servo is None or not angle.isdecimal() or not 0 <= int(angle) <= 180:
            self.send_json(400, {"error": "Use light1/light2 and an angle from 0 to 180"})
            return
        try:
            data = self.call_esp32("/api/servo?" + urlencode({"servo": servo, "angle": angle}))
            self.send_json(200, data)
        except Exception as exc:
            self.send_json(502, {"error": f"ESP32 connection failed: {exc}"})

    def call_esp32(self, path: str) -> dict:
        request = Request(self.esp32_base + path, headers={"Accept": "application/json"})
        with urlopen(request, timeout=3) as response:
            return json.loads(response.read().decode("utf-8"))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--esp32", required=True, help="ESP32 base URL, e.g. http://192.168.0.50")
    parser.add_argument("--host", default="0.0.0.0")
    parser.add_argument("--port", type=int, default=8080)
    args = parser.parse_args()
    Handler.esp32_base = args.esp32.rstrip("/")
    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"방 스위치 서버: http://127.0.0.1:{args.port}")
    print(f"ESP32 연결 대상: {Handler.esp32_base}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
