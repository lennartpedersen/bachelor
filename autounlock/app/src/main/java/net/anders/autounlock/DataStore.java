package net.anders.autounlock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

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

    private static final String WINDOW_TABLE = "window";
    private static final String WINDOW_ID = "window_id";
    private static final String WINDOW_UNLOCK_ID = "unlock_id";
    private static final String WINDOW_ORIENTATION = "orientation";
    private static final String WINDOW_ACCELERATION_MAG = "acceleration_mag";
    private static final String WINDOW_ACCELERATION_X = "acceleration_x";
    private static final String WINDOW_ACCELERATION_Y = "acceleration_y";

    private static final String UNLOCK_TABLE = "unlock";
    private static final String UNLOCK_ID = "unlock_id";
    private static final String UNLOCK_VAL = "value";

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

    void insertUnlockValue(int val) {
        ContentValues contentValues = new ContentValues();
        Cursor c = database.rawQuery("SELECT MAX(unlock_id) FROM "+ UNLOCK_TABLE + ";", null);
        c.moveToFirst();
        int id = c.getInt(0) + 1;
        contentValues.put(UNLOCK_ID, id);
        contentValues.put(UNLOCK_VAL, val);
        try {
            database = databaseHelper.getWritableDatabase();
            database.beginTransaction();
            database.replace(UNLOCK_TABLE, null, contentValues);
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
    }

    void insertWindows(WindowData[] snapshot) {
        ContentValues contentValues = new ContentValues();
        Cursor c = database.rawQuery("SELECT MAX(unlock_id) FROM "+ WINDOW_TABLE + ";", null);
        c.moveToFirst();
        int id = c.getInt(0) + 1;
        contentValues.put(WINDOW_UNLOCK_ID, id);

        for (WindowData window: snapshot) {
            contentValues.put(WINDOW_ACCELERATION_X, window.getAccelerationX());
            contentValues.put(WINDOW_ACCELERATION_Y, window.getAccelerationY());
            contentValues.put(WINDOW_ORIENTATION, window.getOrientation());
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
            countQuery = "SELECT DISTINCT unlock_id FROM " + WINDOW_TABLE;
            Cursor cursor = database.rawQuery(countQuery, null);
            cnt = cursor.getCount();
            cursor.close();
        } finally {
            database.endTransaction();
        }
        return cnt;
    }

    int getUnlockValue(int id) {
        int idCursor;
        try {
            database = databaseHelper.getReadableDatabase();
            database.beginTransaction();
            String countQuery;
            countQuery = "SELECT value FROM " + UNLOCK_TABLE + " WHERE " + UNLOCK_ID + " = " + id;
            Cursor cursor = database.rawQuery(countQuery, null);
            cursor.moveToFirst();
            idCursor = cursor.getInt(0);
            cursor.close();
        } finally {
            database.endTransaction();
        }
        return idCursor;
    }

    // Method to retreive unlock sessions
    ArrayList<ArrayList<WindowData>> getUnlocks() {
        ArrayList<ArrayList<WindowData>> unlocks = new ArrayList<>();
        int cur_id = 0;
        int prev_id = 0;

        try {
            database = databaseHelper.getReadableDatabase();
            database.beginTransaction();

            ArrayList<WindowData> unlock = new ArrayList<>();

            String unlockQuery;
            unlockQuery = "SELECT * FROM " + WINDOW_TABLE + ";";

            Cursor unlockCursor = database.rawQuery(unlockQuery, null);

            if (unlockCursor.moveToFirst()) {
                do {
                    cur_id = unlockCursor.getInt(unlockCursor.getColumnIndex(WINDOW_UNLOCK_ID));
                    double accelerationX = unlockCursor.getDouble(unlockCursor.getColumnIndex(WINDOW_ACCELERATION_X));
                    double accelerationY = unlockCursor.getDouble(unlockCursor.getColumnIndex(WINDOW_ACCELERATION_Y));
                    double orientation = unlockCursor.getDouble(unlockCursor.getColumnIndex(WINDOW_ORIENTATION));
                    double accelerationMag = unlockCursor.getDouble(unlockCursor.getColumnIndex(WINDOW_ACCELERATION_MAG));
                    double time = unlockCursor.getDouble(unlockCursor.getColumnIndex(TIMESTAMP));

                    if (cur_id != prev_id && prev_id != 0) {
                        unlocks.add(unlock);
                        unlock = new ArrayList<>();
                    }
                    unlock.add(new WindowData(cur_id,accelerationX, accelerationY, orientation, accelerationMag, time));
                    prev_id = cur_id;
                } while (unlockCursor.moveToNext());

                // Add current unlock when queue is at its end
                unlocks.add(unlock);
            }
            unlockCursor.close();
        } finally {
            database.endTransaction();
        }
        return unlocks;
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

            database.execSQL("CREATE TABLE " + WINDOW_TABLE + " ("
                    + WINDOW_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + WINDOW_UNLOCK_ID + " INTEGER,"
                    + WINDOW_ACCELERATION_X + " DOUBLE, "
                    + WINDOW_ACCELERATION_Y + " DOUBLE, "
                    + WINDOW_ORIENTATION + " DOUBLE, "
                    + WINDOW_ACCELERATION_MAG + " DOUBLE, "
                    + TIMESTAMP + " LONG)");

            database.execSQL("CREATE TABLE " + UNLOCK_TABLE + " ("
                    + UNLOCK_ID + " INTEGER PRIMARY KEY,"
                    + UNLOCK_VAL + " INTEGER)");
        }

        private void dropDatastore() {
            database.execSQL("DROP TABLE IF EXISTS " + LOCK_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + BLUETOOTH_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + WINDOW_TABLE);
            database.execSQL("DROP TABLE IF EXISTS " + UNLOCK_TABLE);
        }
    }
}
