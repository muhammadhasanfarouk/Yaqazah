# Yaqazah - يقظة 🚗💤
> **Real-time Driver Monitoring & Sleep Prediction System**

![Flutter](https://img.shields.io/badge/Mobile-Flutter-blue)
![Spring Boot](https://img.shields.io/badge/Backend-Spring%20Boot-green)
![TensorFlow Lite](https://img.shields.io/badge/AI-TFLite-orange)
![PostgreSQL](https://img.shields.io/badge/DB-PostgreSQL-336791)

## 📖 Overview
[cite_start]**Yaqazah** is a real-time driver monitoring system designed to predict drowsiness using computer vision and lightweight deep learning[cite: 22]. [cite_start]By analyzing facial landmarks and ocular metrics on standard mobile devices, Yaqazah eliminates the need for specialized hardware[cite: 23].

[cite_start]The system shifts the safety paradigm from **reactive detection** to **proactive prevention**, identifying the onset of fatigue before it reaches a critical stage[cite: 24, 53].

---

## 🌟 Key Features

### 📱 For Drivers (Mobile Application)
* [cite_start]**Real-Time Monitoring:** Detects drowsiness (eye closure), distraction (looking away), and yawning using the front camera[cite: 143, 144].
* [cite_start]**Sleep Prediction:** Utilizing LSTM models to capture temporal behavior patterns and predict sleepiness probability[cite: 147, 563].
* [cite_start]**Smart Alerts:** Customizable alerts (Sound, Visual, Vibration) triggered when safety thresholds are exceeded[cite: 133, 148].
* [cite_start]**Offline Capability:** Runs deep learning models locally on-device (Edge Computing) without requiring internet connectivity[cite: 32, 74].
* [cite_start]**Session Reports:** Export driving history and safety reports as PDF or CSV[cite: 136].

### 💻 For Fleet Managers (Web Dashboard)
* [cite_start]**Fleet Visibility:** Search for drivers and view fleet-wide statistics[cite: 139, 141].
* [cite_start]**Analytics Dashboard:** Visualize daily, weekly, and monthly safety data to optimize risk management[cite: 135, 124].
* [cite_start]**Report Management:** Filter and search reports by date, driver, or alert type[cite: 140].

---

## 🏗️ System Architecture
[cite_start]Yaqazah follows a **Layered Architecture**[cite: 105]:

1.  [cite_start]**Presentation Layer:** Driver UI (Mobile) and Admin UI (Web)[cite: 106].
2.  [cite_start]**Business Logic Layer:** Includes Session, User, Analytics, Detection, Alert, and Report services[cite: 109].
3.  [cite_start]**Data Layer:** Utilizes **PostgreSQL** for persistent storage and **Redis** for caching frequently accessed data[cite: 118, 63, 66].

### AI Pipeline
[cite_start]The system uses a dual-layer AI approach[cite: 547]:
1.  **Extraction Layer (CV):**
    * [cite_start]**MediaPipe:** For real-time facial landmark detection (EAR/MAR)[cite: 554].
    * [cite_start]**YOLOv8:** For detecting external distractions like phone usage or smoking[cite: 556].
2.  **Classification Layer (ML):**
    * [cite_start]**LSTM (Long Short-Term Memory):** Used to remember behavior over time and predict fatigue progression[cite: 563].
    * [cite_start]**TFLite:** Models are compressed to run efficiently on mobile devices[cite: 73].

---

## 🛠️ Tech Stack

| Component | Technology | Description |
| :--- | :--- | :--- |
| **Mobile Framework** | Flutter | [cite_start]Cross-platform UI for Android/iOS[cite: 57]. |
| **Backend Framework** | Spring Boot | [cite_start]Scalable RESTful APIs & business logic[cite: 60]. |
| **Database** | PostgreSQL | [cite_start]Robust relational database[cite: 63]. |
| **Caching** | Redis | [cite_start]In-memory store for fast retrieval[cite: 66]. |
| **AI/ML** | TensorFlow Lite | [cite_start]Deployment on mobile devices[cite: 73]. |
| **Computer Vision** | MediaPipe & YOLOv8 | [cite_start]Facial analysis and object detection[cite: 554, 556]. |

---

## 👥 Team

[cite_start]**Faculty of Computing and Artificial Intelligence, Cairo University** [cite: 1, 2]
[cite_start]**Department of Computer Science** [cite: 4]

### Supervised By
* **Dr. [cite_start]Sabah Sayed** [cite: 7]
* **TA. [cite_start]Amany Mohamed Hesham** [cite: 8]

### Implemented By
* [cite_start]**Yousef Ashraf Showman** [cite: 12]
* [cite_start]**Touka Mohamed Korany** [cite: 12]
* [cite_start]**Yasmine Sherif Mohamed** [cite: 12]
* [cite_start]**Mohamed Aber Hassan** [cite: 12]
* [cite_start]**Muhammad Hasan Farouk** [cite: 12]

---

## 📄 License
[cite_start]This project is a Graduation Project for the Academic Year 2025-2026[cite: 13, 14].
