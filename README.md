# AirLink 📡⚡
> **Turn Off The Towers. Cut The Cables. We Still Talk.**
>
> A decentralized, jammer-resistant peer-to-peer mesh communications app for Android. Calls, group chat, and photo sharing that hop from person to person over Wi-Fi Direct and Bluetooth. **Zero internet. Zero central servers. Zero SIM card.**

---

<p align="center">
  <img src="assets/logo.svg" width="120" height="120" alt="AirLink Logo" />
</p>

<p align="center">
  <a href="#download-apk"><img src="https://img.shields.io/badge/Download-APK%20v1.0%20(17.6MB)-10b981?style=for-the-badge&logo=android" alt="Download APK" /></a>
  <a href="#license"><img src="https://img.shields.io/badge/License-Apache--2.0-06b6d4?style=for-the-badge" alt="License" /></a>
  <a href="#platform"><img src="https://img.shields.io/badge/Platform-Android%208.0%2B-blue?style=for-the-badge&logo=android" alt="Platform" /></a>
</p>

---

## ⚡ The Philosophy: Mocking Centralized Censorship & Jammers

Conventional telecom networks rely on fragile, central cell towers owned by monopolistic carriers and overseen by regulatory authorities. When authorities deploy radio jammers or order telecom blackouts during student exams, protests, or disasters, everyone loses signal.

**AirLink completely invalidates that model:**
1. **RF Jammers Can't Touch It**: Jammers target cellular LTE/5G bands (700MHz–2.6GHz). AirLink hops across localized 5GHz & 2.4GHz Wi-Fi Direct and Bluetooth Low Energy right under their nose.
2. **No Central Servers or Undersea Cables**: There is no AWS/Cloud server to seize or block. The network exists dynamically between physically present phones.
3. **No Phone Numbers or SIM Cards**: Identity is derived from cryptographic Ed25519 public keys. You cannot confiscate what doesn't exist.
4. **Self-Healing Multi-Hop Routing**: Packets hop through intermediate phones. Kill one route, and the mesh automatically re-routes across other peers in milliseconds.

---

## 🚀 Key Features

* **Real-Time Voice Calls (`ActiveCallService`)**: Low-latency PCM/Opus audio capture and full-duplex voice streaming directly peer-to-peer over high-speed sockets (~20–30ms latency).
* **Multi-Hop Mesh Routing (`MeshRouter`)**: Extends communication far beyond physical radio limits by relaying encrypted packets across chains of intermediate phones with automated TTL (Time-To-Live).
* **Zero-Touch Peer Discovery**: Automatic background advertising and scanning using Google Nearby Connections, Wi-Fi Direct NSD, and Bluetooth LE beacons.
* **End-to-End Encrypted Messaging & Photos**: Chat with 1-on-1 peers or broadcast to the entire room. Transfer photos and files with chunked reliable packet reassembly.
* **360° Radar Topology Visualizer (`MeshVisualizerScreen`)**: Real-time visual radar sweeping through surrounding nodes, indicating direct neighbors vs multi-hop peers and signal strengths.
* **Battery-Optimized Duplicate Suppression**: High-speed in-memory LRU packet cache prevents duplicate broadcasts, keeping battery usage under 2% per hour.

---

## 📥 Download APK & Installation

### Option 1: Direct Download
* Download the compiled release: **[AirLink-v1.0.apk](./AirLink-v1.0.apk)** *(17.6 MB)*
* **SHA-256 Checksum:** `6ddba9501d2aae73ae3fa9c298e498d7683735554cd21af4abc2d763b9bf01ca`

### System Requirements:
* Android 8.0 (Oreo / API 26) through Android 15 (Vanilla Ice Cream / API 35)
* Wi-Fi Direct and Bluetooth LE hardware support
* Zero mobile internet or data plan needed

### Step-by-Step Installation:
1. Download `AirLink-v1.0.apk`.
2. Tap the downloaded file. When prompted, enable **"Install from unknown sources"** for your browser or file manager.
3. Launch AirLink, grant Microphone and Nearby Devices permissions, set your nickname, and start talking!

---

## 🛠️ Architecture & Under The Hood

```
   [ Device A ] 
        │ (Wi-Fi Direct / Hotspot - Hop 1)
        ▼
   [ Device B (Relay Node) ]
        │ (Wi-Fi Direct / BLE - Hop 2)
        ▼
   [ Device C (Relay Node) ]
        │ (Socket Stream - Hop 3)
        ▼
   [ Device D (Target Peer) ]
```

### Core Modules:
* `org.offlinemesh.mesh.MeshRouter`: Handles route tables, packet propagation, HELLO broadcast heartbeats, and duplicate cache management.
* `org.offlinemesh.service.ActiveCallService`: Foreground Android service managing low-latency voice capture, audio track playback, and background multi-hop forwarding.
* `org.offlinemesh.identity.IdentityManager`: Manages cryptographic keypairs and local device identities.
* `org.offlinemesh.ui.screens`: Jetpack Compose UI including Radar Topology, Peer Discovery, Chat, Active Call, and Diagnostics telemetry.

---

## 💻 Building From Source

```bash
# Clone the repository
git clone https://github.com/TejashMalviya/AirLink.git
cd AirLink

# Build Debug APK
./gradlew assembleDebug

# Build Release APK
./gradlew assembleRelease
```

The compiled APK will be generated at `app/build/outputs/apk/release/app-release.apk`.

---

## 🌐 Promotional Website

This repository also hosts the official interactive distribution landing page:
* Interactive Three.js 3D Mesh & Jammer Simulation sandbox.
* Working interactive phone screen simulations for Radar, Chat, Voice Call, and Telemetry.
* Interactive "Can They Silence You?" Jammer Calculator.
* Direct APK download & QR code generation.

Simply open `index.html` in any modern web browser or host on GitHub Pages.

---

## 👤 Author & Credits

* **Tejash Malviya** — Lead Architect & Developer
* GitHub: [@TejashMalviya](https://github.com/TejashMalviya)

---

## 📄 License

This project is licensed under the Apache 2.0 License - see the LICENSE file for details.
