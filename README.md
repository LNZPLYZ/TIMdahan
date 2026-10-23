# TIMdahan: Sari-Sari Store Inventory & Point of Sale System

**TIMdahan** is an offline-first native Android application designed to help Filipino Sari-Sari store owners manage product inventory, execute point-of-sale (POS) checkout transactions, track daily sales analytics, and sync store data to the cloud.

---

## 📱 API Demonstration & Screenshots

Below are screenshots demonstrating the application's core APIs in both **Successful States** and **Error / Empty States**:

### 1. Google ML Kit Barcode Scanner API
> Used for on-device barcode scanning during inventory product creation and POS checkout.

| Successful Scan (Inventory & Checkout) | Error / Unregistered Barcode State |
| :---: | :---: |
| <img src="screenshots/barcode_success.png" width="300" /> <br/><br/> <img src="screenshots/barcode_success1.png" width="300" /> | <img src="screenshots/barcode_error.png" width="300" /> |
| *Barcode scanned and product field populated* | *Unregistered barcode prompt / Not found error* |

---

### 2. Firebase Realtime Database Cloud Backup & Sync API
> Used for cloud store database backup, account restore, and multi-device synchronization.

| Successful Cloud Backup & Account Sync | Cloud Restore Error / Not Found State |
| :---: | :---: |
| <img src="screenshots/firebase_success.png" width="300" /> <br/><br/> <img src="screenshots/firebase_success1.png" width="300" /> | <img src="screenshots/firebase_error.png" width="300" /> |
| *Cloud backup and account sync successful toasts* | *No cloud backup found for account error prompt* |

---

### 3. Analytics & Inventory Database API (`DatabaseHelper`)
> Used for SQLite offline persistence, POS sales transaction logging, and real-time report generation.

| Active Sales & Analytics State | Empty State (No Transactions) |
| :---: | :---: |
| <img src="screenshots/reports_active.png" width="300" /> | <img src="screenshots/reports_empty.png" width="300" /> |
| *Sales trend line graph and best sellers ranking* | *Empty state screen when no sales transactions exist* |

---

## 🚀 Key Features

- **Offline-First Inventory Management:** Product CRUD operations, low-stock threshold alerts, and category filtering.
- **On-Device Barcode Scanner:** Integrated Google ML Kit Code Scanner (`play-services-code-scanner`) for zero-latency scanning.
- **Point of Sale (POS) Checkout:** Auto-complete product selection, automatic stock deduction, and atomic SQLite sales logging.
- **Sales Analytics & Reports:** Interactive custom line chart (`SalesChartView`), period filters (Today, 7D, 30D, Custom), and best-seller product rankings.
- **Cloud Backup & Restore:** Firebase Realtime Database integration for store database backup and account restore.

---

## 🛠️ Tech Stack & Requirements

- **Language:** Java 11
- **Target / Compile SDK:** Android 15 (API 37) | **Min SDK:** Android 7.0 (API 24)
- **Database:** Local SQLite (`timdahan.db` v3) & Firebase Realtime Database
- **Hardware Interop:** Google ML Kit Code Scanner
- **Architecture:** Native Android XML, Custom Views, Material Design 3

---

## ⚙️ Setup Steps

### 1. Prerequisites
* **Android Studio** (Ladybug / 2024.2.1 or newer recommended)
* **JDK 11** or higher configured in Android Studio
* Android Device or Emulator running **Android 7.0 (API Level 24)** or higher

### 2. Installation
1. Clone this repository to your local machine:
   ```bash
   git clone https://github.com/YOUR_USERNAME/TIMdahan.git
   ```
2. Open **Android Studio**, select **Open**, and navigate to the cloned `TIMdahan` directory.
3. Allow Gradle to perform project sync and download required dependencies.
4. Select your connected Android device or emulator and press **Run 'app'** (`Shift + F10`).

---

## 🔑 Instructions for Adding API Keys & Configuration Files

### 1. Firebase Realtime Database Setup (`google-services.json`)
The application uses Firebase Realtime Database for optional cloud backup and account restoration across devices.

1. Go to the [Firebase Console](https://console.firebase.google.com/) and create a new Firebase project (or select an existing project).
2. Register a new Android App with the package name:  
   `com.example.timdahan`
3. Download the generated `google-services.json` configuration file.
4. Place the `google-services.json` file inside the `app/` folder of the project:  
   `TIMdahan/app/google-services.json`
5. In your Firebase Console, navigate to **Build $\rightarrow$ Realtime Database**, create a database instance, and configure Database Security Rules:
   ```json
   {
     "rules": {
       ".read": "auth != null",
       ".write": "auth != null"
     }
   }
   ```

---

### 2. Google Play Services ML Kit Barcode Scanner API
The project uses **Google Code Scanner API** (`com.google.android.gms:play-services-code-scanner:16.1.0`) powered by Google ML Kit.

* **No Manual API Key Required:** The scanner uses Google Play Services to run machine learning models on-device. No secret API key string needs to be hardcoded in code.
* **Automatic Model Download:** The model dependency is configured in `AndroidManifest.xml` via:
  ```xml
  <meta-data
      android:name="com.google.mlkit.vision.DEPENDENCIES"
      android:value="barcode" />
  ```
  Google Play Services automatically downloads the ML Kit barcode model upon app installation.
