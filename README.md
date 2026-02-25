# Yaqazah - يقظة 🚗💤

> **Real-time Driver Monitoring & Sleep Prediction System**

![Flutter](https://img.shields.io/badge/Mobile-Flutter-blue)
![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-green)
![TensorFlow Lite](https://img.shields.io/badge/AI-TFLite-orange)
![PostgreSQL](https://img.shields.io/badge/DB-PostgreSQL-336791)

## 📖 Overview

**Yaqazah** is a real-time driver monitoring system that predicts drowsiness using computer vision and lightweight deep
learning.  
By analyzing facial landmarks and ocular metrics on standard mobile devices, Yaqazah eliminates the need for specialized
hardware.

The system shifts road safety from reactive detection to proactive prevention by identifying early signs of fatigue
before they become critical.

---

## 🌟 Key Features

### 📱 For Drivers (Mobile Application)

- **Real-Time Monitoring:** Detects eye closure, yawning, and head movement using the front camera.
- **Sleep Prediction:** Uses LSTM models to estimate drowsiness probability.
- **Smart Alerts:** Customizable alerts (sound, vibration, visual).
- **Offline Capability:** Runs fully offline using on-device AI inference.
- **Session Reports:** Export driving sessions as PDF/CSV with full history tracking.

### 💻 For Fleet Managers (Web Dashboard)

- **Fleet Visibility:** Search drivers by username or email.
- **Analytics Dashboard:** View fleet-level statistics and trends.
- **Report Management:** Filter and analyze reports by date, driver, or alert type.

---

## 🏗️ System Architecture

Yaqazah follows a layered architecture:

1. **Presentation Layer:** Mobile Driver App + Web Admin Dashboard
2. **Business Logic Layer:** Session, User, Analytics, Detection, Alert, and Reporting services
3. **Data Layer:** PostgreSQL for persistent storage and Redis for caching

### AI Pipeline

The system uses a dual-layer AI pipeline:

**Extraction Layer (Computer Vision):**

- MediaPipe — facial landmarks, EAR & MAR
- YOLOv8 — external distraction detection (mobile usage, smoking)

**Classification Layer (Machine Learning):**

- LSTM — temporal modeling of fatigue progression
- TensorFlow Lite — optimized mobile inference

---

## 🛠️ Tech Stack

| Component       | Technology      |
|-----------------|-----------------|
| Mobile          | Flutter         |
| Backend         | Spring Boot     |
| Database        | PostgreSQL      |
| Caching         | Redis           |
| AI/ML           | TensorFlow Lite |
| Computer Vision | MediaPipe       |
