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

## 🛠️ Tech Stack

- **Language:** Java 11
- **Target / Compile SDK:** Android 15 (API 37) | **Min SDK:** Android 7.0 (API 24)
- **Database:** SQLite (`timdahan.db` v3) & Firebase Realtime Database
- **Hardware Interop:** Google ML Kit Code Scanner
- **Architecture:** Native Android XML, Custom Views, Material Design 3

---

## 📦 How to Build and Run

1. Clone this repository:
   ```bash
   git clone https://github.com/YOUR_USERNAME/TIMdahan.git
   ```
2. Open the project in **Android Studio**.
3. Build and run on an Android Device or Emulator (`Shift + F10`).
