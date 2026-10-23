package com.example.timdahan;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "timdahan.db";
    private static final int DATABASE_VERSION = 3; // Bumped version for transaction_number per user

    // Table Names
    public static final String TABLE_USERS = "users";
    public static final String TABLE_PRODUCTS = "products";
    public static final String TABLE_SALES = "sales";
    public static final String TABLE_SALE_ITEMS = "sale_items";

    // Products Columns
    public static final String COL_PRODUCT_ID = "product_id";
    public static final String COL_PRODUCT_USER_ID = "user_id";
    public static final String COL_PRODUCT_BARCODE = "barcode";
    public static final String COL_PRODUCT_NAME = "product_name";
    public static final String COL_PRODUCT_CATEGORY = "category";
    public static final String COL_PRODUCT_PRICE = "price";
    public static final String COL_PRODUCT_STOCK = "stock_quantity";
    public static final String COL_PRODUCT_MIN_STOCK = "minimum_stock";

    // Users Columns
    public static final String COL_USER_ID = "user_id";
    public static final String COL_USER_STORE_NAME = "store_name";
    public static final String COL_USER_EMAIL = "email";
    public static final String COL_USER_PASSWORD = "password";

    // Sales Columns
    public static final String COL_SALE_ID = "sale_id";
    public static final String COL_SALE_USER_ID = "user_id";
    public static final String COL_SALE_USER_TXN_NUM = "transaction_number";
    public static final String COL_SALE_DATE = "sale_date";
    public static final String COL_SALE_TOTAL = "total_amount";

    // Sale Items Columns
    public static final String COL_ITEM_ID = "sale_item_id";
    public static final String COL_ITEM_SALE_ID = "sale_id";
    public static final String COL_ITEM_PRODUCT_ID = "product_id";
    public static final String COL_ITEM_QUANTITY = "quantity";
    public static final String COL_ITEM_UNIT_PRICE = "unit_price";
    public static final String COL_ITEM_SUBTOTAL = "subtotal";

    public static class CategoryCount {
        public String categoryName;
        public int count;

        public CategoryCount(String categoryName, int count) {
            this.categoryName = categoryName;
            this.count = count;
        }
    }

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create Users Table
        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USER_STORE_NAME + " TEXT NOT NULL, "
                + COL_USER_EMAIL + " TEXT UNIQUE NOT NULL, "
                + COL_USER_PASSWORD + " TEXT NOT NULL)";
        db.execSQL(CREATE_USERS_TABLE);

        // Create Products Table (Linked to user_id)
        String CREATE_PRODUCTS_TABLE = "CREATE TABLE " + TABLE_PRODUCTS + " ("
                + COL_PRODUCT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_PRODUCT_USER_ID + " INTEGER NOT NULL, "
                + COL_PRODUCT_BARCODE + " TEXT, "
                + COL_PRODUCT_NAME + " TEXT NOT NULL, "
                + COL_PRODUCT_CATEGORY + " TEXT, "
                + COL_PRODUCT_PRICE + " REAL NOT NULL, "
                + COL_PRODUCT_STOCK + " INTEGER NOT NULL DEFAULT 0, "
                + COL_PRODUCT_MIN_STOCK + " INTEGER NOT NULL DEFAULT 5, "
                + "FOREIGN KEY(" + COL_PRODUCT_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "))";
        db.execSQL(CREATE_PRODUCTS_TABLE);

        // Create Sales Table (Linked to user_id, with transaction_number)
        String CREATE_SALES_TABLE = "CREATE TABLE " + TABLE_SALES + " ("
                + COL_SALE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_SALE_USER_ID + " INTEGER NOT NULL, "
                + COL_SALE_USER_TXN_NUM + " INTEGER NOT NULL DEFAULT 1, "
                + COL_SALE_DATE + " TEXT DEFAULT (DATETIME('now', 'localtime')), "
                + COL_SALE_TOTAL + " REAL NOT NULL, "
                + "FOREIGN KEY(" + COL_SALE_USER_ID + ") REFERENCES " + TABLE_USERS + "(" + COL_USER_ID + "))";
        db.execSQL(CREATE_SALES_TABLE);

        // Create Sale Items Table
        String CREATE_SALE_ITEMS_TABLE = "CREATE TABLE " + TABLE_SALE_ITEMS + " ("
                + COL_ITEM_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ITEM_SALE_ID + " INTEGER NOT NULL, "
                + COL_ITEM_PRODUCT_ID + " INTEGER NOT NULL, "
                + COL_ITEM_QUANTITY + " INTEGER NOT NULL, "
                + COL_ITEM_UNIT_PRICE + " REAL NOT NULL, "
                + COL_ITEM_SUBTOTAL + " REAL NOT NULL, "
                + "FOREIGN KEY(" + COL_ITEM_SALE_ID + ") REFERENCES " + TABLE_SALES + "(" + COL_SALE_ID + "), "
                + "FOREIGN KEY(" + COL_ITEM_PRODUCT_ID + ") REFERENCES " + TABLE_PRODUCTS + "(" + COL_PRODUCT_ID + "))";
        db.execSQL(CREATE_SALE_ITEMS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_SALES + " ADD COLUMN " + COL_SALE_USER_TXN_NUM + " INTEGER NOT NULL DEFAULT 1;");

                // Assign sequential transaction numbers per user for existing sales
                Cursor cursor = db.rawQuery("SELECT " + COL_SALE_ID + ", " + COL_SALE_USER_ID + " FROM " + TABLE_SALES + " ORDER BY " + COL_SALE_USER_ID + " ASC, " + COL_SALE_ID + " ASC", null);
                if (cursor.moveToFirst()) {
                    int lastUserId = -1;
                    int currentTxnNum = 0;
                    do {
                        int sId = cursor.getInt(0);
                        int uId = cursor.getInt(1);
                        if (uId != lastUserId) {
                            lastUserId = uId;
                            currentTxnNum = 1;
                        } else {
                            currentTxnNum++;
                        }
                        ContentValues cv = new ContentValues();
                        cv.put(COL_SALE_USER_TXN_NUM, currentTxnNum);
                        db.update(TABLE_SALES, cv, COL_SALE_ID + " = ?", new String[]{String.valueOf(sId)});
                    } while (cursor.moveToNext());
                }
                cursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ==========================================
    // USER AUTHENTICATION CRUD OPERATIONS
    // ==========================================

    // Register a new user
    public long addUser(String storeName, String email, String password) {
        String canonicalEmail = BackupManager.getCanonicalEmail(email);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_USER_STORE_NAME, storeName);
        values.put(COL_USER_EMAIL, canonicalEmail);
        values.put(COL_USER_PASSWORD, password);

        return db.insert(TABLE_USERS, null, values);
    }

    // Check if email and password match
    public boolean checkUser(String email, String password) {
        String canonicalEmail = BackupManager.getCanonicalEmail(email);
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COL_USER_ID};
        String selection = COL_USER_EMAIL + " = ?" + " AND " + COL_USER_PASSWORD + " = ?";
        String[] selectionArgs = {canonicalEmail, password};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    // Get User ID by Email
    public int getUserIdByEmail(String email) {
        String canonicalEmail = BackupManager.getCanonicalEmail(email);
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COL_USER_ID};
        String selection = COL_USER_EMAIL + " = ?";
        String[] selectionArgs = {canonicalEmail};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int userId = -1;
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID));
        }
        cursor.close();
        return userId;
    }

    // Get Store Name by Email
    public String getStoreNameByEmail(String email) {
        String canonicalEmail = BackupManager.getCanonicalEmail(email);
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COL_USER_STORE_NAME};
        String selection = COL_USER_EMAIL + " = ?";
        String[] selectionArgs = {canonicalEmail};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        String storeName = "Sari-Sari Store";
        if (cursor.moveToFirst()) {
            storeName = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_STORE_NAME));
        }
        cursor.close();
        return storeName;
    }

    // Get User Password by Email
    public String getUserPasswordByEmail(String email) {
        String canonicalEmail = BackupManager.getCanonicalEmail(email);
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COL_USER_PASSWORD};
        String selection = COL_USER_EMAIL + " = ?";
        String[] selectionArgs = {canonicalEmail};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        String pass = "";
        if (cursor.moveToFirst()) {
            pass = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_PASSWORD));
        }
        cursor.close();
        return pass;
    }

    // Check if email is already registered
    public boolean checkEmailExists(String email) {
        String canonicalEmail = BackupManager.getCanonicalEmail(email);
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COL_USER_ID};
        String selection = COL_USER_EMAIL + " = ?";
        String[] selectionArgs = {canonicalEmail};

        Cursor cursor = db.query(TABLE_USERS, columns, selection, selectionArgs, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count > 0;
    }

    // ==========================================
    // PRODUCT CRUD OPERATIONS (USER ISOLATED)
    // ==========================================

    // Insert a new product into SQLite for a specific user
    public long addProduct(Product product, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PRODUCT_USER_ID, userId);
        values.put(COL_PRODUCT_BARCODE, product.getBarcode());
        values.put(COL_PRODUCT_NAME, product.getName());
        values.put(COL_PRODUCT_CATEGORY, product.getCategory());
        values.put(COL_PRODUCT_PRICE, product.getPrice());
        values.put(COL_PRODUCT_STOCK, product.getStockQuantity());
        values.put(COL_PRODUCT_MIN_STOCK, product.getMinimumStock());

        return db.insert(TABLE_PRODUCTS, null, values);
    }

    // Find a product by exact name for a specific user
    public Product getProductByName(String name, int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COL_PRODUCT_NAME + " = ? AND " + COL_PRODUCT_USER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{name, String.valueOf(userId)});

        Product product = null;
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_ID));
            String barcode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_BARCODE));
            String category = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_CATEGORY));
            double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PRODUCT_PRICE));
            int stock = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_STOCK));
            int minStock = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_MIN_STOCK));

            product = new Product(id, barcode, name, category, price, stock, minStock);
        }
        cursor.close();
        return product;
    }

    // Get all products from SQLite for a specific user
    public List<Product> getAllProducts(int userId) {
        List<Product> productList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COL_PRODUCT_USER_ID + " = ? ORDER BY " + COL_PRODUCT_NAME + " ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_ID));
                String barcode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_BARCODE));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_NAME));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_CATEGORY));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PRODUCT_PRICE));
                int stock = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_STOCK));
                int minStock = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_MIN_STOCK));

                Product product = new Product(id, barcode, name, category, price, stock, minStock);
                productList.add(product);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return productList;
    }

    // Get category names and product counts for category chips filter
    public List<CategoryCount> getCategoryCounts(int userId) {
        List<CategoryCount> list = new ArrayList<>();

        // 1. Total products count for "All"
        int totalProducts = getProductsCount(userId);
        list.add(new CategoryCount("All", totalProducts));

        // 2. Distinct categories used by user's products
        String query = "SELECT COALESCE(NULLIF(TRIM(" + COL_PRODUCT_CATEGORY + "), ''), 'Others') AS cat_name, COUNT(*) AS cat_count"
                + " FROM " + TABLE_PRODUCTS
                + " WHERE " + COL_PRODUCT_USER_ID + " = ?"
                + " GROUP BY cat_name"
                + " ORDER BY cat_name ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                String cat = cursor.getString(0);
                int count = cursor.getInt(1);
                list.add(new CategoryCount(cat, count));
            } while (cursor.moveToNext());
        }
        cursor.close();

        return list;
    }

    // Find a product by barcode for a specific user
    public Product getProductByBarcode(String barcode, int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COL_PRODUCT_BARCODE + " = ? AND " + COL_PRODUCT_USER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{barcode, String.valueOf(userId)});

        Product product = null;
        if (cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_ID));
            String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_NAME));
            String category = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_CATEGORY));
            double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PRODUCT_PRICE));
            int stock = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_STOCK));
            int minStock = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_MIN_STOCK));

            product = new Product(id, barcode, name, category, price, stock, minStock);
        }
        cursor.close();
        return product;
    }

    // Get total number of products for a specific user
    public int getProductsCount(int userId) {
        String countQuery = "SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COL_PRODUCT_USER_ID + " = ?";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, new String[]{String.valueOf(userId)});
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    // Get low stock products for a specific user
    public List<Product> getLowStockProducts(int userId) {
        List<Product> productList = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_PRODUCTS + " WHERE " + COL_PRODUCT_USER_ID + " = ? AND " + COL_PRODUCT_STOCK + " <= " + COL_PRODUCT_MIN_STOCK;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_ID));
                String barcode = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_BARCODE));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_NAME));
                String category = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_CATEGORY));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_PRODUCT_PRICE));
                int stock = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_STOCK));
                int minStock = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PRODUCT_MIN_STOCK));

                Product product = new Product(id, barcode, name, category, price, stock, minStock);
                productList.add(product);
            } while (cursor.moveToNext());
        }

        cursor.close();
        return productList;
    }

    // Update an existing product for a specific user
    public int updateProduct(Product product, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PRODUCT_BARCODE, product.getBarcode());
        values.put(COL_PRODUCT_NAME, product.getName());
        values.put(COL_PRODUCT_CATEGORY, product.getCategory());
        values.put(COL_PRODUCT_PRICE, product.getPrice());
        values.put(COL_PRODUCT_STOCK, product.getStockQuantity());
        values.put(COL_PRODUCT_MIN_STOCK, product.getMinimumStock());

        return db.update(TABLE_PRODUCTS, values, COL_PRODUCT_ID + " = ? AND " + COL_PRODUCT_USER_ID + " = ?",
                new String[]{String.valueOf(product.getId()), String.valueOf(userId)});
    }

    // Delete a product for a specific user
    public void deleteProduct(int productId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PRODUCTS, COL_PRODUCT_ID + " = ? AND " + COL_PRODUCT_USER_ID + " = ?",
                new String[]{String.valueOf(productId), String.valueOf(userId)});
    }

    // ==========================================
    // SALES CRUD OPERATIONS & PER-USER TRANSACTION NUMBER
    // ==========================================

    // Get next sequential transaction number for a specific user
    public int getNextTransactionNumber(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COALESCE(MAX(" + COL_SALE_USER_TXN_NUM + "), 0) + 1 FROM " + TABLE_SALES + " WHERE " + COL_SALE_USER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        int nextNum = 1;
        if (cursor.moveToFirst()) {
            nextNum = cursor.getInt(0);
        }
        cursor.close();
        return nextNum;
    }

    // Record a new sale transaction and automatically reduce inventory stock
    public boolean recordSale(int userId, int productId, int quantity, double unitPrice, double totalAmount) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            int txnNum = getNextTransactionNumber(userId);

            // 1. Insert into Sales Table with user-specific transaction_number
            ContentValues saleValues = new ContentValues();
            saleValues.put(COL_SALE_USER_ID, userId);
            saleValues.put(COL_SALE_USER_TXN_NUM, txnNum);
            saleValues.put(COL_SALE_TOTAL, totalAmount);
            long saleId = db.insert(TABLE_SALES, null, saleValues);

            if (saleId == -1) {
                return false;
            }

            // 2. Insert into Sale Items Table
            ContentValues itemValues = new ContentValues();
            itemValues.put(COL_ITEM_SALE_ID, saleId);
            itemValues.put(COL_ITEM_PRODUCT_ID, productId);
            itemValues.put(COL_ITEM_QUANTITY, quantity);
            itemValues.put(COL_ITEM_UNIT_PRICE, unitPrice);
            itemValues.put(COL_ITEM_SUBTOTAL, totalAmount);
            long itemId = db.insert(TABLE_SALE_ITEMS, null, itemValues);

            if (itemId == -1) {
                return false;
            }

            // 3. Automatically Deduct Stock from Products Table
            String updateStockQuery = "UPDATE " + TABLE_PRODUCTS
                    + " SET " + COL_PRODUCT_STOCK + " = " + COL_PRODUCT_STOCK + " - ?"
                    + " WHERE " + COL_PRODUCT_ID + " = ? AND " + COL_PRODUCT_USER_ID + " = ?";
            db.execSQL(updateStockQuery, new Object[]{quantity, productId, userId});

            db.setTransactionSuccessful();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            db.endTransaction();
        }
    }

    // Get today's sales total amount for a specific user
    public double getTodaysSalesTotal(int userId) {
        double total = 0.0;
        String query = "SELECT SUM(" + COL_SALE_TOTAL + ") FROM " + TABLE_SALES
                + " WHERE " + COL_SALE_USER_ID + " = ? AND DATE(" + COL_SALE_DATE + ") = DATE('now', 'localtime')";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }

        cursor.close();
        return total;
    }

    // Get today's sales transaction count for a specific user
    public int getTodaysSalesCount(int userId) {
        int count = 0;
        String query = "SELECT COUNT(*) FROM " + TABLE_SALES
                + " WHERE " + COL_SALE_USER_ID + " = ? AND DATE(" + COL_SALE_DATE + ") = DATE('now', 'localtime')";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        return count;
    }

    // Get weekly sales total for a specific user (last 7 days)
    public double getWeeklySalesTotal(int userId) {
        double total = 0.0;
        String query = "SELECT SUM(" + COL_SALE_TOTAL + ") FROM " + TABLE_SALES
                + " WHERE " + COL_SALE_USER_ID + " = ? AND DATE(" + COL_SALE_DATE + ") >= DATE('now', 'localtime', '-7 days')";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }

        cursor.close();
        return total;
    }

    // Get monthly sales total for a specific user (last 30 days)
    public double getMonthlySalesTotal(int userId) {
        double total = 0.0;
        String query = "SELECT SUM(" + COL_SALE_TOTAL + ") FROM " + TABLE_SALES
                + " WHERE " + COL_SALE_USER_ID + " = ? AND DATE(" + COL_SALE_DATE + ") >= DATE('now', 'localtime', '-30 days')";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }

        cursor.close();
        return total;
    }

    // Get top-selling products for a specific user
    public List<BestSellerItem> getBestSellers(int userId) {
        List<BestSellerItem> bestSellers = new ArrayList<>();
        String query = "SELECT COALESCE(p." + COL_PRODUCT_NAME + ", 'Deleted Item') AS p_name,"
                + " SUM(si." + COL_ITEM_QUANTITY + ") AS total_units,"
                + " SUM(si." + COL_ITEM_SUBTOTAL + ") AS total_revenue"
                + " FROM " + TABLE_SALE_ITEMS + " si"
                + " JOIN " + TABLE_SALES + " s ON si." + COL_ITEM_SALE_ID + " = s." + COL_SALE_ID
                + " LEFT JOIN " + TABLE_PRODUCTS + " p ON si." + COL_ITEM_PRODUCT_ID + " = p." + COL_PRODUCT_ID
                + " WHERE s." + COL_SALE_USER_ID + " = ?"
                + " GROUP BY si." + COL_ITEM_PRODUCT_ID
                + " ORDER BY total_units DESC, total_revenue DESC LIMIT 5";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                int units = cursor.getInt(1);
                double revenue = cursor.getDouble(2);
                bestSellers.add(new BestSellerItem(name, units, revenue));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return bestSellers;
    }

    // ==========================================
    // FILTERED REPORT RANGE QUERIES
    // ==========================================

    public double getFilteredSalesTotal(int userId, String startDate, String endDate) {
        double total = 0.0;
        String query = "SELECT SUM(" + COL_SALE_TOTAL + ") FROM " + TABLE_SALES
                + " WHERE " + COL_SALE_USER_ID + " = ?"
                + " AND DATE(" + COL_SALE_DATE + ") >= DATE(?)"
                + " AND DATE(" + COL_SALE_DATE + ") <= DATE(?)";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), startDate, endDate});

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }

        cursor.close();
        return total;
    }

    public int getFilteredSalesCount(int userId, String startDate, String endDate) {
        int count = 0;
        String query = "SELECT COUNT(*) FROM " + TABLE_SALES
                + " WHERE " + COL_SALE_USER_ID + " = ?"
                + " AND DATE(" + COL_SALE_DATE + ") >= DATE(?)"
                + " AND DATE(" + COL_SALE_DATE + ") <= DATE(?)";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), startDate, endDate});

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        return count;
    }

    public int getFilteredUnitsSold(int userId, String startDate, String endDate) {
        int count = 0;
        String query = "SELECT COALESCE(SUM(si." + COL_ITEM_QUANTITY + "), 0) FROM " + TABLE_SALE_ITEMS + " si"
                + " JOIN " + TABLE_SALES + " s ON si." + COL_ITEM_SALE_ID + " = s." + COL_SALE_ID
                + " WHERE s." + COL_SALE_USER_ID + " = ?"
                + " AND DATE(s." + COL_SALE_DATE + ") >= DATE(?)"
                + " AND DATE(s." + COL_SALE_DATE + ") <= DATE(?)";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), startDate, endDate});

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }

        cursor.close();
        return count;
    }

    public List<BestSellerItem> getFilteredBestSellers(int userId, String startDate, String endDate) {
        List<BestSellerItem> bestSellers = new ArrayList<>();
        String query = "SELECT COALESCE(p." + COL_PRODUCT_NAME + ", 'Deleted Item') AS p_name,"
                + " SUM(si." + COL_ITEM_QUANTITY + ") AS total_units,"
                + " SUM(si." + COL_ITEM_SUBTOTAL + ") AS total_revenue"
                + " FROM " + TABLE_SALE_ITEMS + " si"
                + " JOIN " + TABLE_SALES + " s ON si." + COL_ITEM_SALE_ID + " = s." + COL_SALE_ID
                + " LEFT JOIN " + TABLE_PRODUCTS + " p ON si." + COL_ITEM_PRODUCT_ID + " = p." + COL_PRODUCT_ID
                + " WHERE s." + COL_SALE_USER_ID + " = ?"
                + " AND DATE(s." + COL_SALE_DATE + ") >= DATE(?)"
                + " AND DATE(s." + COL_SALE_DATE + ") <= DATE(?)";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), startDate, endDate});

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                int units = cursor.getInt(1);
                double revenue = cursor.getDouble(2);
                bestSellers.add(new BestSellerItem(name, units, revenue));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return bestSellers;
    }

    public List<DailySalesData> getDailySalesTrend(int userId, String startDate, String endDate) {
        List<DailySalesData> trendList = new ArrayList<>();
        String query = "SELECT DATE(s." + COL_SALE_DATE + ") AS day_str, SUM(s." + COL_SALE_TOTAL + ") AS day_total"
                + " FROM " + TABLE_SALES + " s"
                + " WHERE s." + COL_SALE_USER_ID + " = ?"
                + " AND DATE(s." + COL_SALE_DATE + ") >= DATE(?)"
                + " AND DATE(s." + COL_SALE_DATE + ") <= DATE(?)"
                + " GROUP BY day_str"
                + " ORDER BY day_str ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), startDate, endDate});

        if (cursor.moveToFirst()) {
            do {
                String fullDate = cursor.getString(0);
                double total = cursor.getDouble(1);

                // Format "YYYY-MM-DD" to "MM/DD" (e.g., "02/23")
                String dateLabel = fullDate;
                if (fullDate != null && fullDate.length() >= 10) {
                    try {
                        String month = fullDate.substring(5, 7);
                        String day = fullDate.substring(8, 10);
                        dateLabel = month + "/" + day;
                    } catch (Exception ignored) {}
                }

                trendList.add(new DailySalesData(dateLabel, total));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return trendList;
    }

    // ==========================================
    // TRANSACTION HISTORY QUERIES
    // ==========================================

    public List<SaleTransaction> getAllTransactions(int userId) {
        List<SaleTransaction> list = new ArrayList<>();
        String query = "SELECT s." + COL_SALE_ID + ", s." + COL_SALE_USER_ID + ", s." + COL_SALE_USER_TXN_NUM
                + ", s." + COL_SALE_DATE + ", s." + COL_SALE_TOTAL + ", COALESCE(SUM(si." + COL_ITEM_QUANTITY + "), 0) AS total_qty"
                + " FROM " + TABLE_SALES + " s"
                + " LEFT JOIN " + TABLE_SALE_ITEMS + " si ON s." + COL_SALE_ID + " = si." + COL_ITEM_SALE_ID
                + " WHERE s." + COL_SALE_USER_ID + " = ?"
                + " GROUP BY s." + COL_SALE_ID
                + " ORDER BY s." + COL_SALE_DATE + " DESC, s." + COL_SALE_ID + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                int saleId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SALE_ID));
                int uId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SALE_USER_ID));
                int txnNum = cursor.getInt(cursor.getColumnIndexOrThrow(COL_SALE_USER_TXN_NUM));
                String saleDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_SALE_DATE));
                double total = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SALE_TOTAL));
                int totalQty = cursor.getInt(cursor.getColumnIndexOrThrow("total_qty"));

                list.add(new SaleTransaction(saleId, uId, txnNum, saleDate, total, totalQty));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return list;
    }

    public List<SaleDetailItem> getTransactionDetails(int saleId, int userId) {
        List<SaleDetailItem> list = new ArrayList<>();
        String query = "SELECT p." + COL_PRODUCT_NAME + ", si." + COL_ITEM_QUANTITY
                + ", si." + COL_ITEM_UNIT_PRICE + ", si." + COL_ITEM_SUBTOTAL
                + " FROM " + TABLE_SALE_ITEMS + " si"
                + " JOIN " + TABLE_SALES + " s ON si." + COL_ITEM_SALE_ID + " = s." + COL_SALE_ID
                + " JOIN " + TABLE_PRODUCTS + " p ON si." + COL_ITEM_PRODUCT_ID + " = p." + COL_PRODUCT_ID
                + " WHERE s." + COL_SALE_ID + " = ? AND s." + COL_SALE_USER_ID + " = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(saleId), String.valueOf(userId)});

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                int qty = cursor.getInt(1);
                double unitPrice = cursor.getDouble(2);
                double subtotal = cursor.getDouble(3);

                list.add(new SaleDetailItem(name, qty, unitPrice, subtotal));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return list;
    }

    // Check if a sale already exists in SQLite (to prevent duplicate sales on restore)
    public boolean checkSaleExists(int userId, String saleDate, double totalAmount) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COL_SALE_ID + " FROM " + TABLE_SALES
                + " WHERE " + COL_SALE_USER_ID + " = ? AND " + COL_SALE_DATE + " = ? AND " + COL_SALE_TOTAL + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userId), saleDate, String.valueOf(totalAmount)});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Insert historical sale record during backup restore with user-specific transaction_number
    public long restoreSaleHeader(int userId, int transactionNumber, String saleDate, double totalAmount, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COL_SALE_USER_ID, userId);
        values.put(COL_SALE_USER_TXN_NUM, transactionNumber > 0 ? transactionNumber : getNextTransactionNumber(userId));
        values.put(COL_SALE_DATE, saleDate);
        values.put(COL_SALE_TOTAL, totalAmount);
        return db.insert(TABLE_SALES, null, values);
    }

    // Insert historical sale item during backup restore (WITHOUT deducting stock)
    public long restoreSaleItem(long saleId, int productId, int quantity, double unitPrice, double subtotal, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COL_ITEM_SALE_ID, saleId);
        values.put(COL_ITEM_PRODUCT_ID, productId);
        values.put(COL_ITEM_QUANTITY, quantity);
        values.put(COL_ITEM_UNIT_PRICE, unitPrice);
        values.put(COL_ITEM_SUBTOTAL, subtotal);
        return db.insert(TABLE_SALE_ITEMS, null, values);
    }
}