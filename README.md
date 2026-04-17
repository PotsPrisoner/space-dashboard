# BioSpace Monitor — Android App

A full-featured Android app converted from the BioSpace Monitor v8 HTML dashboard.  
**Dark space-weather UI. All functions from the original HTML. Works immediately on install.**

---

## Features

| Panel | Description |
|-------|-------------|
| **SPACE** | Kp index, solar wind speed/density/temp, NOAA storm scales (G/S/R), X-ray flux/flare class, hemispheric auroral power — all with live sparklines |
| **IMF** | Full IMF panel: Bz, Bt, Bx, By with 7-day sparklines, phi/theta, directional badges and interpretations |
| **SR** | Schumann Resonance engine — f₁ frequency, drift, Q-factor, amplitude, intensity — derived from live TEC+Bz+solar wind. Animated waveform, harmonic→ANS biological table |
| **ANS** | Autonomic Nervous System load engine. Probabilistic symptom predictions (tachycardia, fatigue, brain fog, sleep disruption, etc.) with expandable mechanism detail |
| **ENV** | Local weather via Open-Meteo: temp, humidity, barometric pressure (24hr trend chart), wind. Autonomic trigger risk assessment (heat load, baro drop rate, dewpoint gap, humidity stress) |
| **CME** | NASA DONKI CME tracker — 7-day coronal mass ejection events with speed, type, ANS influence pathway explanations |
| **IMG** | Live solar imagery grid — SDO AIA 171/304/193/094Å, HMI intensitygram/magnetogram, LASCO C2/C3 coronagraphs |
| **ASSESS** | Integrated body burden assessment: 0–100 score, driver breakdown, clinical narrative, management protocol |
| **ALERTS** | Live NOAA SWPC space weather alert feed |
| **CHAT** | Local in-app chat channel with auto Kp update messages |

---

## Quick Start — Push to GitHub and Download APK

### 1. Create GitHub repo
```bash
git init
git add .
git commit -m "Initial BioSpace Monitor Android app"
git remote add origin https://github.com/YOUR_USERNAME/BioSpaceMonitor.git
git push -u origin main
```

### 2. GitHub Actions builds it automatically
Go to your repo → **Actions** tab → watch the build.  
When it finishes (≈5 min), click the run → **Artifacts** → download `BioSpaceMonitor-debug`.

### 3. Install on your phone
- Enable **Settings → Security → Install unknown apps** for your browser/Files app
- Open the downloaded `.apk` and tap Install

---

## Build Locally with Termux

```bash
# Install dependencies in Termux
pkg update && pkg install -y openjdk-17 git wget unzip

# Clone your repo
git clone https://github.com/YOUR_USERNAME/BioSpaceMonitor.git
cd BioSpaceMonitor

# Download gradle wrapper jar (required once)
wget -q "https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar" \
  -O gradle/wrapper/gradle-wrapper.jar

# Make gradlew executable
chmod +x gradlew

# Download Android SDK command-line tools
mkdir -p ~/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdtools.zip
unzip /tmp/cmdtools.zip -d ~/android-sdk/cmdline-tools
mv ~/android-sdk/cmdline-tools/cmdline-tools ~/android-sdk/cmdline-tools/latest

export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Accept licenses and install SDK
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-35" "build-tools;34.0.0"

# Build
./gradlew assembleDebug

# APK is at:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## Data Sources

| Source | Data |
|--------|------|
| `services.swpc.noaa.gov` | Kp, solar wind plasma, IMF/Bz, X-ray flux, alerts, hemispheric power |
| `kauai.ccmc.gsfc.nasa.gov` | NASA DONKI CME events |
| `api.open-meteo.com` | Local weather (temp, humidity, pressure, wind) |
| `geocoding-api.open-meteo.com` | City name → coordinates |
| `sdo.gsfc.nasa.gov` | Live SDO solar imagery |
| `soho.nascom.nasa.gov` | LASCO C2/C3 coronagraph imagery |

All APIs are **free, no key required** (NASA DONKI uses `DEMO_KEY` which allows 30 req/hour).

---

## Auto-refresh Schedule

| Data | Interval |
|------|----------|
| Space weather, IMF, X-ray, alerts | 60 seconds |
| Hemispheric power | 120 seconds |
| CME events | 5 minutes |
| SR/ANS computed engine | 4 seconds |

---

## Notes

- **Min Android version**: API 26 (Android 8.0 Oreo)
- **Location**: GPS or city search. Defaults to West Monroe, Louisiana if neither is set.
- **No account required, no ads, no tracking.**
- The Schumann Resonance metrics are **computed** from live NOAA data using established coupling models — not a dedicated SR sensor feed (no public real-time SR API exists).
