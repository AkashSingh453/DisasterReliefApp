# 🌍 P2P Disaster Relief System

![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Backend-brightgreen.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)
![Status](https://img.shields.io/badge/Status-Active-success.svg)

A comprehensive, offline-first Peer-to-Peer (P2P) Disaster Relief System designed to operate in low or zero-connectivity environments. This project leverages mesh networking and on-device AI to facilitate SOS broadcasting, resource coordination, and offline mapping during critical emergencies when traditional network infrastructure fails.

## 📸 App Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/590eb13d-8490-4c45-8aa4-223840abdb7c" width="30%" />
  <img src="https://github.com/user-attachments/assets/f6c0361e-721c-403a-adb7-ba3fef1fbdd0" width="30%" />
  <img src="https://github.com/user-attachments/assets/c56894c4-3221-4f6a-b6c2-8186736f549d" width="30%" />
  <br />
  <img src="https://github.com/user-attachments/assets/92230ebb-1c9c-4b98-915d-4eada1852ed2" width="30%" />
  <img src="https://github.com/user-attachments/assets/99152d34-ae1e-4416-93db-c4ec3ad29486" width="30%" />
  <img src="https://github.com/user-attachments/assets/a11d9f85-2563-4c82-a347-16164b9850ca" width="30%" />
  <br />
  <img src="https://github.com/user-attachments/assets/7ab55883-4999-4b81-9dc7-c8abae08082f" width="30%" />
  <img src="https://github.com/user-attachments/assets/f3f5fd57-afaa-40ec-9a8a-49ed772ade7e" width="30%" />
  <img src="https://github.com/user-attachments/assets/bb639124-275e-40ff-9294-43ed8995d3e6" width="30%" />
</p>

## 🚀 Key Features

*   **P2P Mesh Networking:** Uses Google Nearby Connections to create a resilient, decentralized mesh network allowing nearby devices to communicate without internet access.
*   **Offline First & Auto-Sync:** All critical data (SOS requests, locations) is stored locally using Room Database and automatically syncs with the central server when internet connectivity is restored.
*   **Offline Mapping:** Integrated with OSMDroid to provide fully functional maps and location tracking even when offline.
*   **On-Device AI Assistant:** Powered by MediaPipe GenAI and the Qwen model, offering intelligent, on-device assistance for first aid and emergency guidelines without requiring a cloud connection.
*   **Centralized Backend (Ktor):** A robust Ktor-based backend server with PostgreSQL for centralizing synced data, providing a macro-level view of relief efforts.

---

## 🛠️ Tech Stack

### 📱 Android Application (`DisasterReliefApp`)
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose & Material 3
*   **Architecture:** MVVM with Clean Architecture principles
*   **Dependency Injection:** Dagger Hilt
*   **Local Database:** Room
*   **Mesh Networking:** Google Nearby Connections API
*   **Networking / Sync:** Retrofit & OkHttp
*   **Offline Maps:** OSMDroid
*   **On-Device AI:** MediaPipe GenAI Engine (Qwen model)
*   **Asynchronous Programming:** Kotlin Coroutines & Flows

### 🖥️ Backend Server (`DisasterReliefServer`)
*   **Framework:** Ktor Server (Netty)
*   **Language:** Kotlin
*   **Database ORM:** JetBrains Exposed
*   **Database:** PostgreSQL (H2 for local testing)
*   **Connection Pooling:** HikariCP
*   **Serialization:** Kotlinx Serialization

---

## 🏗️ System Architecture

The system consists of two primary components operating in tandem:

1.  **The Edge Network (Android Apps):** Users' smartphones form a decentralized mesh network. If a user creates an SOS alert, it is broadcasted to nearby peers. The message hops from device to device until it reaches a node with internet connectivity.
2.  **The Central Hub (Ktor Backend):** Once any node in the mesh network gains internet access, it syncs the locally aggregated mesh data (like active SOS requests) up to the centralized PostgreSQL database via REST APIs.

---

## 💻 Getting Started

### Prerequisites
*   Android Studio (Latest version recommended)
*   JDK 17+
*   PostgreSQL installed locally or via Docker

### 1. Setting up the Backend Server
1. Navigate to the `DisasterReliefServer` directory.
2. Ensure you have a PostgreSQL instance running. Update the database credentials in the application configuration if necessary.
3. Run the server:
   ```bash
   ./gradlew run
   ```
   The server will start on `http://localhost:8080` (or as configured).

### 2. Setting up the Android App
1. Open the `DisasterReliefApp` directory in Android Studio.
2. Open `app/build.gradle.kts` and update the `SYNC_BASE_URL` to point to your local server's IP address (e.g., your machine's local Wi-Fi IP).
   ```kotlin
   buildConfigField("String", "SYNC_BASE_URL", "\"http://YOUR_LOCAL_IP:8080/\"")
   ```
3. Build and run the application on a physical Android device or emulator (Note: Nearby Connections testing requires at least two physical devices or extensive emulator configuration).

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request or open an Issue for bug reports and feature requests.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
