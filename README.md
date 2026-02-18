# Yaqazah - يقظة 🚗💤
> **Real-time Driver Monitoring & Sleep Prediction System**

[![Flutter](https://img.shields.io/badge/Mobile-Flutter-blue)](https://flutter.dev/)
[![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-green)](https://spring.io/projects/spring-boot)
[![TensorFlow Lite](https://img.shields.io/badge/AI-TFLite-orange)](https://www.tensorflow.org/lite)
[![PostgreSQL](https://img.shields.io/badge/DB-PostgreSQL-336791)](https://www.postgresql.org/)

## 📖 Overview
**Yaqazah** is a cross-platform mobile and web solution designed to tackle the global challenge of driver fatigue and distraction. Unlike traditional systems that require expensive hardware, Yaqazah leverages **computer vision** and **lightweight deep learning** on standard smartphones to monitor driver behavior in real-time.

The system shifts the safety paradigm from **reactive detection** (alarming after sleep) to **proactive prevention** (predicting sleepiness before it happens).

---

## 🌟 Key Features

### 📱 For Drivers (Mobile App)
* **Real-Time Monitoring:** Detects drowsiness (closed eyes), distraction (looking away), and yawning using the front-facing camera.
* **Sleep Prediction:** Uses LSTM models to analyze behavioral trends and predict sleepiness probability before it becomes critical.
* **Smart Alerts:** Customizable audio, visual, and vibration alerts triggered by safety thresholds.
* **Offline Capability:** Runs entirely on-device (Edge Computing) to ensure privacy and functionality in low-connectivity areas.
* **Session Reports:** View driving history and export safety reports (PDF/CSV).

### 💻 For Fleet Managers (Web Dashboard)
* **Fleet Visibility:** Monitor driver status and identify high-risk behaviors.
* **Analytics Dashboard:** Visualize daily, weekly, and monthly safety statistics.
* **Driver Management:** Register drivers, manage profiles, and search fleet records.

---

## 🏗️ System Architecture
Yaqazah follows a **Layered Architecture** ensuring modularity and scalability.

1.  **Presentation Layer:** Flutter (Mobile) & Web Dashboard.
2.  **Business Logic Layer:** Microservices for Sessions, Users, Analytics, Alerts, and Reporting.
3.  **Data Layer:** PostgreSQL (Persistent storage) & Redis (Caching).

### AI Pipeline
The AI component utilizes a dual-layer approach:
1.  **Extraction Layer (CV):** Uses **MediaPipe** for facial landmarks (EAR/MAR) and **YOLOv8** for object detection (smoking/phone use).
2.  **Classification Layer (ML):** Uses **LSTM** (Long Short-Term Memory) networks to capture temporal behavior patterns for accurate sleep prediction.

---

## 🛠️ Tech Stack

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Mobile Frontend** | Flutter | Cross-platform UI for Android/iOS. |
| **Backend** | Spring Boot | Scalable RESTful APIs & business logic. |
| **Database** | PostgreSQL | Relational database for user & session data. |
| **Caching** | Redis | In-memory store for fast analytics retrieval. |
| **AI Inference** | TensorFlow Lite | Lightweight model deployment on mobile. |
| **Computer Vision** | MediaPipe | Real-time facial landmark detection. |

---

## 👥 Team

**Faculty of Computing and Artificial Intelligence, Cairo University**
**Department of Computer Science**

### Supervised By
* **Dr. Sabah Sayed**
* **TA. Amany Mohamed Hesham**

### Implemented By
| Name | ID |
| :--- | :--- |
| **Yousef Ashraf Showman** | 20221197 |
| **Touka Mohamed Korany** | 20220094 |
| **Yasmine Sherif Mohamed** | 20220377 |
| **Mohamed Aber Hassan** | 20220292 |
| **Muhammad Hasan Farouk** | 20221120 |

---

## 🚀 Getting
