# Luigi

Home automation system built with Kotlin/Ktor that manages Zigbee smart devices (lights, switches, curtains, sensors) via Zigbee2MQTT over MQTT.

## Build & Run

```bash
./gradlew build          # Build fat JAR (output: build/libs/luigi-all.jar)
./gradlew run            # Run locally
./gradlew test           # Run tests
```

**Deploy to production:**
```bash
./deploy.sh              # build → docker build/push → nomad job restart
```

Note: `deploy.sh` uses `nomad job restart` with `-on-error=ask` which requires an interactive terminal. If running non-interactively, run manually:
```bash
nomad job restart -on-error=fail luigi
```

## Environment

Set via `LUIGI_ENVIRONMENT` env var:
- `production` — real MQTT broker (Paho), real devices
- `development` (default) — `MockMqttClient`, two virtual light groups

Key env vars:
- `LUIGI_ENVIRONMENT` — `production` or `development`
- `OPENWEATHER_API_KEY` — required in production (injected by Nomad)

## Architecture

### Device Layer (`io.pranjal.home.devices`)
- `ZigbeeDevice` — base class; renames device in Z2M via `zigbee2mqtt/bridge/request/device/rename` on init (using IEEE address → friendly name)
- `Devices` — registry of all physical devices; watches `zigbee2mqtt/bridge/state` and re-initializes all devices when Z2M restarts
- Device types: `HueBulb`, `HueDimmer`, `InovelliSwitch`, `VirtualLight`/`VirtualDimmer` (dev/test)

### Lights Layer (`io.pranjal.home.lights`)
- `LightsGroup` — groups lights with shared brightness/color-temp schedules and switch inputs
- Schedules interpolate through time-based waypoints (defined in `Schedules.kt`)
- State persisted via retained MQTT messages to `luigi/lightgroup/{name}/internalstate`

### MQTT (`io.pranjal.mqtt`)
- `MqttClient` interface with `PahoMqttClient` (production) and `MockMqttClient` (dev/test)
- Device commands → `zigbee2mqtt/{name}/set`
- Device events ← `zigbee2mqtt/{name}/action`

### Home Assistant integration (`io.pranjal.home.homeassistant`)
- Publishes MQTT discovery to `homeassistant/light/luigi/{group}/config`
- Syncs state to/from `luigi/lightgroup/{name}/state`
- HA commands use lenient JSON (`ignoreUnknownKeys = true`) — HA sends extra fields like `color_mode` that must be tolerated
- Bad HA payloads are caught and logged; they must NOT propagate exceptions out of `homeAssistantListener` or the entire coroutine scope (render loop, switch listeners, etc.) will be cancelled

## Infrastructure
- Runs as a Nomad job (`luigi.hcl`) in a Docker container
- MQTT broker (mosquitto) reached via Consul Connect sidecar at `localhost:1883`
- HTTP server on port 5555 (exposed via Traefik with TLS)
- Docker base: `amazoncorretto:22-headless`

## Local MQTT access (for debugging)

The production MQTT broker is accessible from dev machines via Consul Connect:
```bash
consul connect proxy -service op-pranjal -upstream mosquitto:1883 &
# broker now available at localhost:1883
mosquitto_sub -h localhost -p 1883 -t 'luigi/#' -v
mosquitto_pub -h localhost -p 1883 -t 'luigi/lightgroup/Kitchen/set' -m '{"state":"ON","brightness":254}'
```

## Coroutine structure

All `LightsGroup` coroutines (render loop, switch listener, HA listener) run as children of the `runBlocking` scope in `launchLightsThread`. An unhandled exception in any child will cancel the entire scope and call `exitProcess(1)`. Always wrap error-prone collect blocks in try-catch.
