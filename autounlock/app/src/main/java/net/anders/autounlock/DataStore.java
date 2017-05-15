package net.anders.autounlock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import net.anders.autounlock.MachineLearning.UnlockData;
import net.anders.autounlock.MachineLearning.WindowData;

import java.util.ArrayList;

class DataStore {
    private static final String DATABASE_NAME = "datastore.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TIMESTAMP = "TIMESTAMP";

    private static final String LOCK_TABLE = "lock";
    private static final String LOCK_MAC = "MAC";
    private static final String LOCK_PASSPHRASE = "passphrase";
    private static final String LOCK_LATITUDE = "latitude";
    private static final String LOCK_LONGITUDE = "longitude";
    private static final String LOCK_INNER_GEOFENCE = "inner_geofence";
    private static final String LOCK_OUTER_GEOFENCE = "outer_geofence";
    private static final String LOCK_ORIENTATION = "orientation";

    private static final String BLUETOOTH_TABLE = "bluetooth";
    private static final String BLUETOOTH_NAME = "name";
    private static final String BLUETOOTH_SOURCE = "source";
    private static final String BLUETOOTH_RSSI = "RSSI";
    private static final String BLUETOOTH_NEARBY_LOCK = "nearby_lock";

    private static final String WIFI_TABLE = "wifi";
    private static final String WIFI_SSID = "SSID";
    private static final String WIFI_MAC = "MAC";
    private static final String WIFI_RSSI = "RSSI";
    private static final String WIFI_NEARBY_LOCK = "nearby_lock";

    private static final String UNLOCK_TABLE = "unlock";
    private static final String UNLOCK_ID = "id";
    private static final String UNLOCK_CLUSTER = "cluster";

    private static final String WINDOW_TABLE = "window";
    private static final String WINDOW_ID = "window_id";
    private static final String WINDOW_UNLOCK_ID = "unlock_id";
    private static final String WINDOW_ORIENTATION = "orientation";
    private static final String WINDOW_VELOCITY = "velocity";
    private static final String WINDOW_ACCELERATION_X = "acceleration_x";
    private static final String WINDOW_ACCELERATION_Y = "acceleration_y";
    private static final String WINDOW_SPEED_X = "speed_x";
    private static final String WINDOW_SPEED_Y = "speed_y";
    private static final String WINDOW_ACCELERATION_MAG = "acceleration_mag";

    private static final String DECISION_TABLE = "decision";
    private static final String DECISION_ID = "id";
    private static final String DECISION_DECISION = "decision";

    private SQLiteDatabase database;
    private DatabaseHelper databaseHelper;

    DataStore(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    void insertLockDetails(
            String lockMAC,
            String lockPassphrase,
            double lockLatitude,
            double lockLongitude,
            float lockInnerGeofence,
            float lockOuterGeofence,
            float lockOrientation,
            long timestamp
    ) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(LOCK_MAC, lockMAC);
        contentValues.put(LOCK_PASSPHRASE, lockPassphrase);
        contentValues.put(LOCK_LATITUDE, lockLatitude);
        contentValues.put(LOCK_LONGITUDE, lockLongitude);
        contentValues.put(LOCK_INNER_GEOFENCE, lockInnerGeofence);
        contentValues.put(LOCK_OUTER_GEOFENCE, lockOuterGeofence);
        contentValues.put(LOCK_ORIENTATION, lockOrientation);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(LOCK_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    ArrayList<LockData> getKnownLocks() {
        ArrayList<LockData> lockDataArrayList = new ArrayList<>();

        try {
            database = databaseHelper.getReadableDatabase();
            database.beginTransaction();

            String lockQuery = "SELECT * FROM " + LOCK_TABLE + ";";
            Cursor lockCursor = database.rawQuery(lockQuery, null);

            lockCursor.moveToFirst();
            if (!lockCursor.isAfterLast()) {
                for (int i = 0; i < lockCursor.getCount(); i++) {
                    String lockMac = lockCursor.getString(lockCursor.getColumnIndex(LOCK_MAC));
                    double lockLatitude = lockCursor.getDouble(lockCursor.getColumnIndex(LOCK_LATITUDE));
                    double lockLongitude = lockCursor.getDouble(lockCursor.getColumnIndex(LOCK_LONGITUDE));
                    float innerGeofence = lockCursor.getInt(lockCursor.getColumnIndex(LOCK_INNER_GEOFENCE));
                    float outerGeofence = lockCursor.getInt(lockCursor.getColumnIndex(LOCK_OUTER_GEOFENCE));
                    float orientation = lockCursor.getFloat(lockCursor.getColumnIndex(LOCK_ORIENTATION));
                    LockData lockData = new LockData(
                            lockMac,
                            new LocationData(lockLatitude, lockLongitude),
                            innerGeofence,
                            outerGeofence,
                            orientation
                    );
                    lockDataArrayList.add(lockData);
                }
            }
            lockCursor.close();
        } finally {
            database.endTransaction();
        }
        return lockDataArrayList;
    }

    LockData getLockDetails(String foundLock) {
        LockData lockData;
        LocationData locationData;
        BluetoothData bluetoothData;
        WifiData wifiData;

        String lockMac;
        String lockPassphrase;
        double lockLatitude;
        double lockLongitude;
        float innerGeofence;
        float outerGeofence;
        float orientation;

        ArrayList<BluetoothData> nearbyBluetoothDevices = new ArrayList<>();
        ArrayList<WifiData> nearbyWifiAccessPoints = new ArrayList<>();

        try {
            database = databaseHelper.getReadableDatabase();
            database.beginTransaction();

            String lockQuery = "SELECT * FROM " + LOCK_TABLE + " WHERE " + LOCK_MAC + "='" + foundLock + "';";
            Cursor lockCursor = database.rawQuery(lockQuery, null);

            lockCursor.moveToFirst();
            if (lockCursor.isAfterLast()) {
                // We have not found any locks and return null.
                lockCursor.close();
                return null;
            } else {
                lockMac = lockCursor.getString(lockCursor.getColumnIndex(LOCK_MAC));
                lockPassphrase = lockCursor.getString(lockCursor.getColumnIndex(LOCK_PASSPHRASE));
                lockLatitude = lockCursor.getDouble(lockCursor.getColumnIndex(LOCK_LATITUDE));
                lockLongitude = lockCursor.getDouble(lockCursor.getColumnIndex(LOCK_LONGITUDE));
                innerGeofence = lockCursor.getInt(lockCursor.getColumnIndex(LOCK_INNER_GEOFENCE));
                outerGeofence = lockCursor.getInt(lockCursor.getColumnIndex(LOCK_OUTER_GEOFENCE));
                orientation = lockCursor.getFloat(lockCursor.getColumnIndex(LOCK_ORIENTATION));
                lockCursor.close();
            }

            String bluetoothQuery = "SELECT * FROM " + BLUETOOTH_TABLE + " WHERE "
                    + BLUETOOTH_NEARBY_LOCK + "='" + foundLock + "';";
            Cursor bluetoothCursor = database.rawQuery(bluetoothQuery, null);
            if (bluetoothCursor.getColumnCount() != 0) {
                bluetoothCursor.moveToFirst();
                for (int i = 0; i <= bluetoothCursor.getColumnCount(); i++) {
                    String bluetoothName = bluetoothCursor.getString(bluetoothCursor.getColumnIndex(BLUETOOTH_NAME));
                    String bluetoothSource = bluetoothCursor.getString(bluetoothCursor.getColumnIndex(BLUETOOTH_SOURCE));
                    int bluetoothRSSI = bluetoothCursor.getInt(bluetoothCursor.getColumnIndex(BLUETOOTH_RSSI));
                    long bluetoothTimestamp = bluetoothCursor.getLong(bluetoothCursor.getColumnIndex(TIMESTAMP));

                    bluetoothData = new BluetoothData(bluetoothName, bluetoothSource, bluetoothRSSI, bluetoothTimestamp);
                    nearbyBluetoothDevices.add(bluetoothData);

                    if (!(bluetoothCursor.isLast() || bluetoothCursor.isAfterLast())) {
                        bluetoothCursor.moveToNext();
                    }
                }
            }
            bluetoothCursor.close();

            String wifiQuery = "SELECT * FROM " + WIFI_TABLE + " WHERE "
                    + WIFI_NEARBY_LOCK + "='" + foundLock + "';";
            Cursor wifiCursor = database.rawQuery(wifiQuery, null);
            if (wifiCursor.getColumnCount() != 0) {
                wifiCursor.moveToFirst();
                for (int i = 0; i <= wifiCursor.getColumnCount(); i++) {
                    String wifiSSID = wifiCursor.getString(wifiCursor.getColumnIndex(WIFI_SSID));
                    String wifiMAC = wifiCursor.getString(wifiCursor.getColumnIndex(WIFI_MAC));
                    int wifiRSSI = wifiCursor.getInt(wifiCursor.getColumnIndex(WIFI_RSSI));
                    long wifiTimestamp = wifiCursor.getLong(wifiCursor.getColumnIndex(TIMESTAMP));

                    wifiData = new WifiData(wifiSSID, wifiMAC, wifiRSSI, wifiTimestamp);
                    nearbyWifiAccessPoints.add(wifiData);

                    if (!(wifiCursor.isLast() || wifiCursor.isAfterLast())) {
                        wifiCursor.moveToNext();
                    }
                }
            }
            wifiCursor.close();

            locationData = new LocationData(lockLatitude, lockLongitude);
            lockData = new LockData(lockMac, lockPassphrase, locationData,
                    innerGeofence, outerGeofence, orientation, nearbyBluetoothDevices, nearbyWifiAccessPoints);
            return lockData;
        } finally {
            database.endTransaction();
        }
    }

    void insertBtle(String name, String btleSource, int btleRSSI, String nearbyLock, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(BLUETOOTH_NAME, name);
        contentValues.put(BLUETOOTH_SOURCE, btleSource);
        contentValues.put(BLUETOOTH_RSSI, btleRSSI);
        contentValues.put(BLUETOOTH_NEARBY_LOCK, nearbyLock);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(BLUETOOTH_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    void insertWifi(String wifiSSID, String wifiMAC, int wifiRSSI, String nearbyLock, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(WIFI_SSID, wifiSSID);
        contentValues.put(WIFI_MAC, wifiMAC);
        contentValues.put(WIFI_RSSI, wifiRSSI);
        contentValues.put(WIFI_NEARBY_LOCK, nearbyLock);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(WIFI_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    // Method to insert decision into DECISION_TABLE
    void insertDecision(int decision, long timestamp) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DECISION_DECISION, decision);
        contentValues.put(TIMESTAMP, timestamp);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(DECISION_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    // Method to delete unlock and windows
    void deleteCluster(int delete) {
        ArrayList<UnlockData> unlocks = getUnlocks();

        for (UnlockData unlock : unlocks) {
            int clusterId = getClusterId(unlock.getId());

            if (clusterId == delete) {
                deleteWindows(unlock.getId());
                String unlockQuery = "DELETE FROM " + UNLOCK_TABLE + " WHERE " + UNLOCK_ID + "='" + unlock.getId() + "';";

                try {
                    database = databaseHelper.getWritableDatabase();
                    database.beginTransaction();
                    database.execSQL(unlockQuery);
                    database.setTransactionSuccessful();
                } finally {
                    database.endTransaction();
                }

            }
        }
    }

    void deleteWindows(int id){
        String windowQuery = "DELETE FROM " + WINDOW_TABLE + " WHERE " + WINDOW_UNLOCK_ID + "='" + id + "';";

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.execSQL(windowQuery);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    // Insert snapshot consisting of sliding windows into UNLOCK_TABLE
    void insertUnlock(WindowData[] snapshot) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(UNLOCK_CLUSTER, 0);

        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(UNLOCK_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }

        Cursor c = database.rawQuery("SELECT last_insert_rowid()", null);
        c.moveToFirst();
        int id = c.getInt(0);

        insertWindows(snapshot, id);
    }

    void insertWindows(WindowData[] snapshot, int id) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(WINDOW_UNLOCK_ID, id);

        for (WindowData window: snapshot) {
            contentValues.put(WINDOW_ACCELERATION_X, window.getAccelerationX());
            contentValues.put(WINDOW_ACCELERATION_Y, window.getAccelerationY());
            contentValues.put(WINDOW_SPEED_X, window.getSpeedX());
            contentValues.put(WINDOW_SPEED_Y, window.getSpeedY());
            contentValues.put(WINDOW_ORIENTATION, window.getOrientation());
            contentValues.put(WINDOW_VELOCITY, window.getVelocity());
            contentValues.put(WINDOW_ACCELERATION_MAG, window.getAccelerationMag());
            contentValues.put(TIMESTAMP, window.getTime());

            try {
                database = databaseHelper.getWritableDatabase();
                database.beginTransaction();
                database.replace(WINDOW_TABLE, null, contentValues);
                database.setTransactionSuccessful();
            } finally {
                database.endTransaction();
            }
        }
    }

    // Method to get the amount of unlock sessions stored
    int getUnlockCount() {
        int cnt;
        try {
            database = databaseHelper.getReadableDatabase();
            database.beginTransaction();
            String countQuery;
            countQuery = "SELECT * FROM " + UNLOCK_TABLE;
            Cursor cursor = database.rawQuery(countQuery, null);
            cnt = cursor.getCount();
            cursor.close();
        } finally {
            database.endTransaction();
        }
        return cnt;
    }

    // Method to retreive unlock sessions
    ArrayList<UnlockData> getUnlocks() {
        ArrayList<UnlockData> unlocks = new ArrayList<>();
        int cur_id = 0;
        int prev_id = 0;

        try {
            database = databaseHelper.getReadableDatabase();
            database.beginTransaction();

            ArrayList<WindowData> unlock = new ArrayList<>();

            String unlockQuery;
            unlockQuery = "SELECT * FROM " + WINDOW_TABLE
                    + " INNER JOIN " + UNLOCK_TABLE
                    + " ON " + WINDOW_UNLOCK_ID + "=" + UNLOCK_ID + ";";

            Cursor unlockCursor = database.rawQuery(unlockQuery, null);

            if (unlockCursor.moveToFirst()) {
                do {
                    cur_id = unlockCursor.getInt(unlockCursor.getColumnIndex(WINDOW_UNLOCK_ID));
                    double accelerationX = unlockCursor.getDouble(unlockCursor.getColumnIndex(WINDOW_ACCELERATION_X));
                    double accelerationY = unlockCursor.getDouble(unlockCursor.getColumnIndex(WINDOW_ACCELERATION_Y));
                    double speedX = unlockCursor.getDouble(unlockCursor.getColumnIndex(WINDOW_SPEED_X));
                    double speedY = unlockCursor.getDouble(unlockCursor.getColumnIndex(WINDOW_SPEED_Y));
                    double orientation = unlockCursor.getDouble(unlockCursor.getColumnIndex(WINDOW_ORIENTATION));
                    double velocity = unlockCursor.getDouble(unlockCursor.getColumnIndex(WINDOW_VELOCITY));
                    double accelerationMag = unlockCursor.getDouble(unlockCursor.getColumnIndex(WINDOW_ACCELERATION_MAG));
                    double time = unlockCursor.getDouble(unlockCursor.getColumnIndex(TIMESTAMP));

                    if (cur_id != prev_id && prev_id != 0) {
                        UnlockData cluster = new UnlockData(prev_id, 0, unlock);
                        unlocks.add(cluster);
                        unlock = new ArrayList<>();
                    }
                    unlock.add(new WindowData(accelerationX, accelerationY, speedX, speedY, orientation, velocity, accelerationMag, time));
                    prev_id = cur_id;
                } while (unlockCursor.moveToNext());

                // Add current unlock to cluster when queue is at its end
                UnlockData u = new UnlockData(cur_id, 0, unlock);
                unlocks.add(u);
            }
            unlockCursor.close();
        } finally {
            database.endTransaction();
        }
        return unlocks;
    }

    // Check if the unlock session is already clustered
    boolean isClustered(int id) {
        boolean clustered = false;
        try {
            database = databaseHelper.getReadableDatabase();
            database.beginTransaction();

            String countQuery = "SELECT " + UNLOCK_CLUSTER + " FROM " + UNLOCK_TABLE + " WHERE " + UNLOCK_ID + "=" + id + ";";
            Cursor cursor = database.rawQuery(countQuery, null);

            if(cursor.moveToFirst()){
                if (cursor.getInt(0) != 0) {
                    clustered = true;
                }
            }
            cursor.close();
        } finally {
            database.endTransaction();
        }
        return clustered;
    }

    // Get cluster id of the unlock session
    int getClusterId(int id) {
        int cluster_id = 0;
        try {
            database = databaseHelper.getReadableDatabase();
            database.beginTransaction();

            String countQuery = "SELECT " + UNLOCK_CLUSTER + " FROM " + UNLOCK_TABLE + " WHERE " + UNLOCK_ID + "=" + id + ";";
            Cursor cursor = database.rawQuery(countQuery, null);

            if(cursor.moveToFirst()){
                if (cursor.getInt(0) != 0) {
                    cluster_id = cursor.getInt(0);
                }
            }
            cursor.close();
        } finally {
            database.endTransaction();
        }
        return cluster_id;
    }

    // Get the amount of currently stored cluster
    int getClusterCount() {
        int cnt;
        try {
            database = databaseHelper.getReadableDatabase();
            database.beginTransaction();

            String countQuery = "SELECT MAX(" + UNLOCK_CLUSTER + ") FROM " + UNLOCK_TABLE + ";";
            Cursor cursor = database.rawQuery(countQuery, null);
            cursor.moveToFirst();
            cnt = cursor.getInt(0);
            cursor.close();
        } finally {
            database.endTransaction();
        }
        return cnt;
    }

    // Update the cluster of two unlock sessions
    void updateCluster(int cur_id, int next_id) {
        int clusterValue;

        if (isClustered(next_id) && getClusterId(next_id) != 0)  {
            clusterValue = getClusterId(next_id);
        }
        else {
            int ctn = getClusterCount() + 1;
            clusterValue = ctn;
        }

        ContentValues args = new ContentValues();
        args.put(UNLOCK_ID, cur_id);
        args.put(UNLOCK_CLUSTER, clusterValue);
        database.update(UNLOCK_TABLE, args, UNLOCK_ID + "=" + cur_id, null);

        ContentValues args2 = new ContentValues();
        args2.put(UNLOCK_ID, next_id);
        args2.put(UNLOCK_CLUSTER, clusterValue);
        database.update(UNLOCK_TABLE, args2, UNLOCK_ID + "=" + next_id, null);

    }

    private class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createDatastore(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            // Production-quality upgrade code should modify the tables when
            // the database version changes instead of dropping the tables and
            // re-creating them.
            if (newVersion != DATABASE_VERSION) {
                Log.w("Datastore", "Database upgrade from old: " + oldVersion + " to: " +
                        newVersion);
                database = databaseHelper.getWritableDatabase();
                dropDatastore();
                createDatastore(database);
                database.close();
            }
        }

        private void createDatastore(SQLiteDatabase database) {
            database.execSQL("PRAGMA foreign_keys = ON;");
            database.execSQL("CREATE TABLE " + LOCK_TABLE + " ("
                    + LOCK_MAC + " TEXT PRIMARY KEY, "
                    + LOCK_PASSPHRASE + " TEXT, "
                    + LOCK_LATITUDE + " DOUBLE, "
                    + LOCK_LONGITUDE + " DOUBLE, "
                    + LOCK_INNER_GEOFENCE + " FLOAT, "
                    + LOCK_OUTER_GEOFENCE + " FLOAT, "
                    + LOCK_ORIENTATION + " FLOAT, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + BLUETOOTH_TABLE + " ("
                    + BLUETOOTH_NAME + " TEXT, "
                    + BLUETOOTH_SOURCE + " TEXT, "
                    + BLUETOOTH_RSSI + " INTEGER, "
                    + BLUETOOTH_NEARBY_LOCK + " FOREIGNKEY REFERENCES " + LOCK_TABLE + "(" + LOCK_MAC + "), "
                    + TIMESTAMP + " LONG, "
                    + "PRIMARY KEY (" + BLUETOOTH_SOURCE + ", " + BLUETOOTH_NEARBY_LOCK + "))");

            database.execSQL("CREATE TABLE " + WIFI_TABLE + " ("
                    + WIFI_SSID + " TEXT, "
                    + WIFI_MAC + " TEXT, "
                    + WIFI_RSSI + " INTEGER, "
                    + WIFI_NEARBY_LOCK + " FOREIGNKEY REFERENCES " + LOCK_TABLE + "(" + LOCK_MAC + "), "
                    + TIMESTAMP + " LONG, "
                    + "PRIMARY KEY (" + WIFI_MAC + ", " + WIFI_NEARBY_LOCK + "))");

            database.execSQL("CREATE TABLE " + UNLOCK_TABLE + " ("
                    + UNLOCK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + UNLOCK_CLUSTER + " INTEGER DEFAULT 0)");

            database.execSQL("CREATE TABLE " + WINDOW_TABLE + " ("
                    + WINDOW_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + WINDOW_UNLOCK_ID + " INTEGER,"
                    + WINDOW_ACCELERATION_X + " DOUBLE, "
                    + WINDOW_ACCELERATION_Y + " DOUBLE, "
                    + WINDOW_SPEED_X + " DOUBLE, "
                    + WINDOW_SPEED_Y + " DOUBLE, "
                    + WINDOW_ORIENTATION + " DOUBLE, "
                    + WINDOW_VELOCITY + " DOUBLE, "
                    + WINDOW_ACCELERATION_MAG + " DOUBLE, "
                    + TIMESTAMP + " LONG, "
                    + "FOREIGN KEY(" + WINDOW_UNLOCK_ID + ") REFERENCES " + UNLOCK_TABLE + "(" + UNLOCK_ID + "));");

            database.execSQL("CREATE TABLE " + DECISION_TABLE + " ("
                    + DECISION_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + DECISION_DECISION + " INTEGER, "
                    + TIMESTAMP + " LONG)");
        }

        private void dropDatastore() {
            database.execSQL("DROP TABLE IF EXISTS " + LOCK_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + BLUETOOTH_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + WIFI_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + DECISION_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + WINDOW_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + UNLOCK_TABLE);
        }
    }
}
