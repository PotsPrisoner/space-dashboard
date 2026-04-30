# BioSpace Monitor v10

Real-time ANS (Autonomic Nervous System) burden monitor for Android.

## Features
- **ANS Burden Score** 0-100% with Green/Yellow/Red/Blue alert system
- **Biometrics** — S7 watch BLE + manual input fallback
- **Space Weather** — NOAA SWPC + NASA DONKI (Kp, IMF Bz, CME, flares, GST, IPS, HSS, MPC, RBE, SEP, Schumann, Hemispheric Power)
- **Weather** — Open-Meteo (pressure, humidity, heat index, UV, AQI) — GPS or manual location
- **Symptoms** — Dysautonomia symptom logger correlated to live space weather
- **Community Chat** — Public Firebase real-time chat showing each user's live Kp and burden %
- **AI Report** — Gemini 2.5 Flash clinical or general health report

## Download APK
Go to [Releases](../../releases/tag/latest) and download `app-debug.apk`

## Setup
1. Replace `app/google-services.json` with your real Firebase config
2. Get a free Gemini API key at aistudio.google.com and enter it in the Report tab

## Build
Push to main branch — GitHub Actions builds automatically.
