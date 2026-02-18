# Yaqazah - يقظة 🚗💤
> **Real-time Driver Monitoring & Sleep Prediction System**

![Flutter](https://img.shields.io/badge/Mobile-Flutter-blue)
![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-green)
![TensorFlow Lite](https://img.shields.io/badge/AI-TFLite-orange)
![PostgreSQL](https://img.shields.io/badge/DB-PostgreSQL-336791)

## 📖 Overview
[cite_start]**Yaqazah** is a real-time driver monitoring system that predicts drowsiness using computer vision and lightweight deep learning[cite: 22]. [cite_start]By analyzing facial landmarks and ocular metrics on standard mobile devices, Yaqazah eliminates the need for specialized hardware[cite: 23].

[cite_start]The system shifts the safety paradigm from reactive detection to proactive prevention, identifying the onset of fatigue before it reaches a critical stage[cite: 24].

---

## 🌟 Key Features

### 📱 For Drivers (Mobile Application)
* [cite_start]**Real-Time Monitoring:** Monitor the driver's face using the front camera to detect eye closure, yawning, and head movement[cite: 143, 144].
* [cite_start]**Sleep Prediction:** Utilizes LSTM models to predict sleepiness probability[cite: 147, 563].
* [cite_start]**Smart Alerts:** Trigger alerts when thresholds are exceeded, with customizable types (sound, vibration, visual)[cite: 133, 148].
* [cite_start]**Offline Capability:** Runs deep learning models on mobile devices without internet connectivity[cite: 74, 75].
* [cite_start]**Session Reports:** Export reports (PDF/CSV) and view driving history[cite: 134, 136].

### 💻 For Fleet Managers (Web Dashboard)
* [cite_start]**Fleet Visibility:** Search for drivers by username or email[cite: 139].
* [cite_start]**Analytics Dashboard:** View fleet statistics and analytics[cite: 141].
* [cite_start]**Report Management:** Filter and search reports by date, driver, or alert type[cite: 140].

---

## 🏗️ System Architecture
[cite_start]Yaqazah follows a layered architecture[cite: 105]:

1.  [cite_start]**Presentation Layer:** Driver UI (Mobile) and Admin UI (Web) [cite: 106-108].
2.  [cite_start]**Business Logic Layer:** Includes Session, User, Analytics, Detection, Alert, and Report services [cite: 109-116].
3.  [cite_start]**Data Layer:** Utilizes **PostgreSQL** for persistent storage and **Redis** for caching frequently accessed data[cite: 63, 66, 118].

### AI Pipeline
[cite_start]The system uses a dual-layer AI approach[cite: 547]:
1.  **Extraction Layer (CV):**
    * [cite_start]**MediaPipe:** For real-time facial landmark detection (EAR/MAR)[cite: 554].
    * [cite_start]**YOLOv8:** For detecting external distractions like mobile use or smoking[cite: 556].
2.  **Classification Layer (ML):**
    * [cite_start]**LSTM (Long Short-Term Memory):** Used to remember behavior over a sequence of time and capture gradual fatigue progression[cite: 563].
    * [cite_start]**TFLite:** Models are optimized for offline mobile inference[cite: 560, 570].

---

## 🛠️ Tech Stack

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Mobile Framework** | Flutter | [cite_start]Cross-platform UI for Android/iOS[cite: 57]. |
| **Backend Framework** | Spring Boot | [cite_start]Scalable RESTful APIs & business logic[cite: 60, 61]. |
| **Database** | PostgreSQL | [cite_start]Robust relational database for system data[cite: 63]. |
| **Caching** | Redis | [cite_start]In-memory store for fast retrieval[cite: 66]. |
| **AI/ML** | TensorFlow Lite | [cite_start]Deployment on mobile devices[cite: 73]. |
| **Computer Vision** | MediaPipe | [cite_start]Real-time facial landmark detection[cite: 554]. |

---

## 📄 License
[cite_start]This project is a Graduation Project for the Academic Year 2025-2026[cite: 13, 14].
