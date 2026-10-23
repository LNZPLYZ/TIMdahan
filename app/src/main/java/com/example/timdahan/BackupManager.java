package com.example.timdahan;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BackupManager {

    private static final String FIREBASE_DATABASE_URL = "https://timdahan-d27a6-default-rtdb.asia-southeast1.firebasedatabase.app";

    public interface CloudCallback {
        void onSuccess(String message);
        void onError(String errorMessage);
    }

    public interface CloudCheckCallback {
        void onResult(boolean exists, String errorMessage);
    }

    // Helper method to ensure email address is lowercase and trimmed (canonical email)
    public static String getCanonicalEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase();
    }

    // Helper method to convert canonical email to unique Firebase Database child key
    public static String getCanonicalUserKey(String email) {
        String canonical = getCanonicalEmail(email);
        return canonical.replace(".", "_").replace("@", "_at_");
    }

    private static FirebaseDatabase getFirebaseDatabaseInstance() {
        return FirebaseDatabase.getInstance(FIREBASE_DATABASE_URL);
    }

    // Export current user's products AND sales history to a formatted JSON string
    public static String exportDataToJson(Context context, int userId) {
        try {
            DatabaseHelper db = new DatabaseHelper(context);
            UserSession session = new UserSession(context);

            String canonicalEmail = getCanonicalEmail(session.getEmail());

            JSONObject backupObject = new JSONObject();
            backupObject.put("app", "TIMdahan");
            backupObject.put("version", 3);
            backupObject.put("store_name", session.getStoreName());
            backupObject.put("email", canonicalEmail);
            backupObject.put("password", db.getUserPasswordByEmail(canonicalEmail));

            // 1. Export Products
            List<Product> products = db.getAllProducts(userId);
            JSONArray productsArray = new JSONArray();

            for (Product p : products) {
                JSONObject pObj = new JSONObject();
                pObj.put("barcode", p.getBarcode() != null ? p.getBarcode() : "");
                pObj.put("name", p.getName());
                pObj.put("category", p.getCategory());
                pObj.put("price", p.getPrice());
                pObj.put("stock_quantity", p.getStockQuantity());
                pObj.put("minimum_stock", p.getMinimumStock());
                productsArray.put(pObj);
            }
            backupObject.put("products", productsArray);

            // 2. Export Sales & Sale Items
            List<SaleTransaction> transactions = db.getAllTransactions(userId);
            JSONArray salesArray = new JSONArray();

            for (SaleTransaction txn : transactions) {
                JSONObject sObj = new JSONObject();
                sObj.put("transaction_number", txn.getTransactionNumber());
                sObj.put("sale_date", txn.getSaleDate());
                sObj.put("total_amount", txn.getTotalAmount());

                List<SaleDetailItem> items = db.getTransactionDetails(txn.getSaleId(), userId);
                JSONArray itemsArray = new JSONArray();

                for (SaleDetailItem item : items) {
                    JSONObject iObj = new JSONObject();
                    iObj.put("product_name", item.getProductName());
                    iObj.put("quantity", item.getQuantity());
                    iObj.put("unit_price", item.getUnitPrice());
                    iObj.put("subtotal", item.getSubtotal());
                    itemsArray.put(iObj);
                }

                sObj.put("items", itemsArray);
                salesArray.put(sObj);
            }
            backupObject.put("sales", salesArray);

            return backupObject.toString(2); // Formatted JSON String
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class RestoreResult {
        public boolean success;
        public String errorMessage;

        public RestoreResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    // Restore products and sales into SQLite atomically without stock replay or duplicates
    public static RestoreResult restoreDataFromJsonDetailed(Context context, int userId, String jsonString) {
        DatabaseHelper helper = new DatabaseHelper(context);
        SQLiteDatabase db = helper.getWritableDatabase();

        try {
            JSONObject backupObject = new JSONObject(jsonString);

            if (!backupObject.has("app") || !"TIMdahan".equals(backupObject.getString("app"))) {
                return new RestoreResult(false, "Invalid backup file format");
            }

            // Check if backup file belongs to a different email address
            UserSession session = new UserSession(context);
            String currentEmail = getCanonicalEmail(session.getEmail());
            String backupEmail = getCanonicalEmail(backupObject.optString("email", ""));

            if (!backupEmail.isEmpty() && !currentEmail.isEmpty() && !backupEmail.equalsIgnoreCase(currentEmail)) {
                return new RestoreResult(false, "Backup file belongs to another account (" + backupEmail + ")");
            }

            JSONArray productsArray = backupObject.optJSONArray("products");
            JSONArray salesArray = backupObject.optJSONArray("sales");

            int productCount = productsArray != null ? productsArray.length() : 0;
            int salesCount = salesArray != null ? salesArray.length() : 0;

            if (productCount == 0 && salesCount == 0) {
                return new RestoreResult(false, "No backup data found for this account. Please create a backup first.");
            }

            db.beginTransaction();

            Map<String, Integer> productNameToIdMap = new HashMap<>();

            // 1. Restore Products (Upsert to prevent duplicate product rows)
            if (productsArray != null) {
                for (int i = 0; i < productsArray.length(); i++) {
                    JSONObject pObj = productsArray.getJSONObject(i);
                    String barcode = pObj.optString("barcode", "");
                    String name = pObj.optString("name", "");
                    String category = pObj.optString("category", "Others");
                    double price = pObj.optDouble("price", 0.0);
                    int stock = pObj.optInt("stock_quantity", 0);
                    int minStock = pObj.optInt("minimum_stock", 5);

                    if (!name.isEmpty()) {
                        Product existingProduct = helper.getProductByName(name, userId);
                        int pId;
                        if (existingProduct != null) {
                            existingProduct.setPrice(price);
                            existingProduct.setStockQuantity(stock);
                            existingProduct.setMinimumStock(minStock);
                            if (!barcode.isEmpty()) existingProduct.setBarcode(barcode);
                            existingProduct.setCategory(category);
                            helper.updateProduct(existingProduct, userId);
                            pId = existingProduct.getId();
                        } else {
                            Product newProduct = new Product(barcode, name, category, price, stock, minStock);
                            long insertedId = helper.addProduct(newProduct, userId);
                            pId = (int) insertedId;
                        }
                        productNameToIdMap.put(name.toLowerCase(), pId);
                    }
                }
            }

            // 2. Restore Sales & Sale Items (Without re-deducting stock)
            if (salesArray != null) {
                for (int i = 0; i < salesArray.length(); i++) {
                    JSONObject sObj = salesArray.getJSONObject(i);
                    int txnNum = sObj.optInt("transaction_number", 0);
                    String saleDate = sObj.optString("sale_date", "");
                    double totalAmount = sObj.optDouble("total_amount", 0.0);

                    // Check if identical sale already exists to prevent duplicate transactions
                    if (!helper.checkSaleExists(userId, saleDate, totalAmount)) {
                        long saleId = helper.restoreSaleHeader(userId, txnNum, saleDate, totalAmount, db);

                        if (saleId != -1 && sObj.has("items")) {
                            JSONArray itemsArray = sObj.getJSONArray("items");
                            for (int j = 0; j < itemsArray.length(); j++) {
                                JSONObject iObj = itemsArray.getJSONObject(j);
                                String pName = iObj.optString("product_name", "");
                                int qty = iObj.optInt("quantity", 1);
                                double unitPrice = iObj.optDouble("unit_price", 0.0);
                                double subtotal = iObj.optDouble("subtotal", unitPrice * qty);

                                Integer pId = productNameToIdMap.get(pName.toLowerCase());
                                if (pId == null) {
                                    // If product not found in map, check DB or create placeholder
                                    Product dbProduct = helper.getProductByName(pName, userId);
                                    if (dbProduct != null) {
                                        pId = dbProduct.getId();
                                    } else {
                                        Product placeholder = new Product("", pName, "Others", unitPrice, 0, 5);
                                        pId = (int) helper.addProduct(placeholder, userId);
                                    }
                                }

                                helper.restoreSaleItem(saleId, pId, qty, unitPrice, subtotal, db);
                            }
                        }
                    }
                }
            }

            db.setTransactionSuccessful();
            return new RestoreResult(true, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new RestoreResult(false, "Corrupted or invalid backup data");
        } finally {
            if (db.inTransaction()) {
                db.endTransaction();
            }
        }
    }

    public static boolean restoreDataFromJson(Context context, int userId, String jsonString) {
        return restoreDataFromJsonDetailed(context, userId, jsonString).success;
    }

    // ==========================================
    // FIREBASE CLOUD BACKUP & RESTORE & SYNC
    // ==========================================

    // Check if an account with this email already exists in Firebase Realtime Database
    public static void checkCloudAccountExists(String rawEmail, CloudCheckCallback callback) {
        String email = getCanonicalEmail(rawEmail);
        String userKey = getCanonicalUserKey(email);

        if (userKey.isEmpty()) {
            callback.onResult(false, "Invalid email");
            return;
        }

        DatabaseReference cloudRef = getFirebaseDatabaseInstance()
                .getReference("backups")
                .child(userKey);

        cloudRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                callback.onResult(snapshot.exists(), null);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onResult(false, error.getMessage());
            }
        });
    }

    // Upload user store data to Firebase Realtime Database
    public static void uploadToCloud(Context context, int userId, CloudCallback callback) {
        String jsonPayload = exportDataToJson(context, userId);
        if (jsonPayload == null) {
            callback.onError("Failed to generate backup data");
            return;
        }

        UserSession session = new UserSession(context);
        String email = getCanonicalEmail(session.getEmail());

        if (email.isEmpty()) {
            callback.onError("User not logged in");
            return;
        }

        String userKey = getCanonicalUserKey(email);

        DatabaseReference cloudRef = getFirebaseDatabaseInstance()
                .getReference("backups")
                .child(userKey);

        cloudRef.setValue(jsonPayload)
                .addOnSuccessListener(aVoid -> callback.onSuccess("Cloud Backup Successful!"))
                .addOnFailureListener(e -> callback.onError("Cloud Backup Failed: " + e.getMessage()));
    }

    // Download user store data from Firebase Realtime Database
    public static void downloadFromCloud(Context context, int userId, CloudCallback callback) {
        UserSession session = new UserSession(context);
        String email = getCanonicalEmail(session.getEmail());

        if (email.isEmpty()) {
            callback.onError("User not logged in");
            return;
        }

        String userKey = getCanonicalUserKey(email);

        DatabaseReference cloudRef = getFirebaseDatabaseInstance()
                .getReference("backups")
                .child(userKey);

        cloudRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String jsonPayload = snapshot.getValue(String.class);
                    RestoreResult result = restoreDataFromJsonDetailed(context, userId, jsonPayload);

                    if (result.success) {
                        callback.onSuccess("Cloud Restore Successful!");
                    } else {
                        callback.onError(result.errorMessage != null ? result.errorMessage : "Corrupted cloud backup file");
                    }
                } else {
                    callback.onError("No cloud backup found for account: " + email);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError("Cloud Restore Failed: " + error.getMessage());
            }
        });
    }

    // Automatic Cloud Login & Sync for new phone
    public static void loginAndSyncFromCloud(Context context, String rawEmail, String inputPassword, CloudCallback callback) {
        String email = getCanonicalEmail(rawEmail);
        String userKey = getCanonicalUserKey(email);

        DatabaseReference cloudRef = getFirebaseDatabaseInstance()
                .getReference("backups")
                .child(userKey);

        cloudRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        String jsonPayload = snapshot.getValue(String.class);
                        JSONObject backupObject = new JSONObject(jsonPayload);

                        String savedPassword = backupObject.optString("password", "");
                        String storeName = backupObject.optString("store_name", "Sari-Sari Store");

                        if (!savedPassword.isEmpty() && !savedPassword.equals(inputPassword)) {
                            callback.onError("Invalid Password. Please check your credentials.");
                            return;
                        }

                        // Register account in local SQLite on new phone
                        DatabaseHelper db = new DatabaseHelper(context);
                        if (!db.checkEmailExists(email)) {
                            db.addUser(storeName, email, inputPassword);
                        }

                        int userId = db.getUserIdByEmail(email);

                        // Restore all store products and sales into new phone's SQLite
                        boolean restored = restoreDataFromJson(context, userId, jsonPayload);

                        if (restored) {
                            UserSession session = new UserSession(context);
                            session.createSession(userId, storeName, email);
                            callback.onSuccess("Cloud Account Synced Successfully!");
                        } else {
                            callback.onError("Failed to restore cloud store data");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        callback.onError("Error reading cloud account data");
                    }
                } else {
                    callback.onError("Account not found on this phone or Cloud. Please Sign Up.");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError("Cloud Sync Connection Error: " + error.getMessage());
            }
        });
    }
}