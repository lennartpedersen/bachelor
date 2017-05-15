package net.anders.autounlock;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.*;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;

import net.anders.autounlock.Export.Export;
import net.anders.autounlock.MachineLearning.PatternRecognitionService;
import net.anders.autounlock.MachineLearning.UnlockData;
import net.anders.autounlock.MachineLearning.WindowProcess;
import net.anders.autounlock.MachineLearning.TrainingProcess;
import net.anders.autounlock.MachineLearning.WindowData;

import java.util.ArrayList;
import java.util.List;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.ObservationVector;

public class CoreService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<Status> {

    private static final String TAG = "CoreService";

    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    private Intent accelerometerIntent;
    private Intent bluetoothIntent;
    private Intent wifiIntent;
    private Intent locationIntent;
    private Intent scannerIntent;
    private Intent patternRecognitionIntent;

    private GoogleApiClient mGoogleApiClient;
    private net.anders.autounlock.Geofence geofence;

    static List<BluetoothData> recordedBluetooth = new ArrayList<BluetoothData>();
    static List<WifiData> recordedWifi = new ArrayList<WifiData>();
    static List<LocationData> recordedLocation = new ArrayList<LocationData>();
    static volatile ArrayList<String> activeInnerGeofences = new ArrayList<>();
    static ArrayList<String> activeOuterGeofences = new ArrayList<>();
    static WindowProcess windowProcess = new WindowProcess();

    static float currentOrientation = -1f;

    static boolean isLocationDataCollectionStarted = false;
    static boolean isDetailedDataCollectionStarted = false;
    static volatile boolean isScanningForLocks = false;

    static DataStore dataStore;

    /* Values used for ML */
    public static RingBuffer<WindowData> windowBuffer; // Ring buffer consisting of windows
    public static int windowBufferSize; // Size of the ring buffer
    public static int windowSize; // Size of each window, the number of accelerometer data inputs
    public static double windowPercentageOverlap; // Overlap percentage of the windows
    public static int windowOverlap; // Number of windows overlap, computed from overlap % and size of windows
    public static int reqUnlockTraining; // Required numbers of unlock sessions
    public static boolean trainingComplete = false; // Is training done
    public static int orientationThreshold; // Threshold to orientation clustering
    public static int velocityThreshold; // Threshold to velocity clustering
    public static double activityThreshold; // Threshold to activity used in correlation with accMag
    public static boolean isPatternRecognitionRunning = false; // Is service running
    public static boolean isTraining = false; // Is app in training mode, stop pattern recog service
    public static boolean isMoving = false; // Are we currently moving

    // List of vector observations computed from orientation and velocity
    public static List<Hmm<ObservationVector>> HMM = new ArrayList<>();

    // Binder given to clients
    private final IBinder localBinder = new LocalBinder();


    // Class used for the client Binder.  Because we know this service always
    // runs in the same process as its clients, we don't need to deal with IPC.

    class LocalBinder extends Binder {
        CoreService getService() {
            // Return this instance of LocalService so clients can call public methods
            return CoreService.this;
        }
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download adapter file.
            // For our sample, we just sleep for 5 seconds.
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                // Restore interrupt status.
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create adapter
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        // Running the service in the foreground by creating adapter notification
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.bekey_logo)
                .setContentTitle("AutoUnlock")
                .setContentText("Service running in the background")
                .setContentIntent(pendingIntent).build();

        startForeground(1337, notification);

        dataStore = new DataStore(this);
        geofence = new Geofence();

        accelerometerIntent = new Intent(this, AccelerometerService.class);
        locationIntent = new Intent(this, LocationService.class);
        wifiIntent = new Intent(this, WifiService.class);
        bluetoothIntent = new Intent(this, BluetoothService.class);
        scannerIntent = new Intent(this, ScannerService.class);
        patternRecognitionIntent = new Intent(this, PatternRecognitionService.class);

        buildGoogleApiClient();

        IntentFilter geofencesFilter = new IntentFilter();
        geofencesFilter.addAction("GEOFENCES_ENTERED");
        geofencesFilter.addAction("GEOFENCES_EXITED");
        registerReceiver(geofencesReceiver, geofencesFilter);

        IntentFilter startPatternRecognitionFilter = new IntentFilter();
        startPatternRecognitionFilter.addAction("START_PATTERNRECOGNITION");
        startPatternRecognitionFilter.addAction("STOP_PATTERNRECOGNITION");
        startPatternRecognitionFilter.addAction("STOP_RECOGNISE");
        startPatternRecognitionFilter.addAction("INCORRECT_UNLOCK");
        registerReceiver(startPatternRecognitionReceiver, startPatternRecognitionFilter);

        /*  MACHINE LEARNING VALUES */
        CoreService.windowBufferSize = 50;
        CoreService.windowSize = 20;
        CoreService.windowPercentageOverlap = .2;
        CoreService.windowOverlap =  CoreService.windowSize - ((int)(CoreService.windowSize *  CoreService.windowPercentageOverlap));
        CoreService.reqUnlockTraining = 5;
        CoreService.orientationThreshold = 50;
        CoreService.velocityThreshold = 50;
        CoreService.activityThreshold = 0;

        // If the app needs to be training at upstart, mark the training setting
        if (dataStore.getUnlockCount() >= reqUnlockTraining); { trainingComplete = true;}

        // Train the models as the first thing, before application can be used
        if (!dataStore.getUnlocks().isEmpty()) {
            MainActivity.lockView.setText("Updating intelligence \n please be patient");
            trainModel();
        }

        Log.v("CoreService", "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // For each start request, send adapter message to start adapter job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        serviceHandler.sendMessage(msg);

        googleConnect();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        googleDisconnect();
        Log.v("CoreService", "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        // localBinder is used for bound services
        return localBinder;
    }

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    void googleConnect() {
        if (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
    }

    void googleDisconnect() {
        if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.v(TAG, "Connected ");
        addGeofences();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(@NonNull Status status) {

    }

    private BroadcastReceiver geofencesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();
            List<String> triggeringGeofencesList = extras.getStringArrayList("Geofences");
            Log.i(TAG, "onReceive: " + extras.getStringArrayList("Geofences"));

            if ("GEOFENCES_ENTERED".equals(action)) {
                for (String geofence : triggeringGeofencesList) {
                    if (geofence.contains("inner")) {
                        Log.i(TAG, "Entered inner geofence");
                        activeInnerGeofences.add(geofence.substring(5));
                        if (!isDetailedDataCollectionStarted) {
                            Log.i(TAG, "onReceive: starting detailed data collection");
                            isDetailedDataCollectionStarted = true;
                            isScanningForLocks = true;

                            startAccelerometerService();
                            startBluetoothService();
                            startWifiService();
                            scanForLocks();

                            MainActivity.lockDoor.setVisibility(View.VISIBLE);
                            MainActivity.unlockDoor.setVisibility(View.VISIBLE);
                            MainActivity.lockView.setVisibility(View.GONE);
                        }
                    } else if (geofence.contains("outer")) {
                        Log.i(TAG, "Entered outer geofence");
                        activeOuterGeofences.add(geofence.substring(5));
                        newRingBuffer();
                        if (!isLocationDataCollectionStarted) {
                            isLocationDataCollectionStarted = true;
                            startLocationService();
                        }
                    }
                }
            } else if ("GEOFENCES_EXITED".equals(action)) {
                for (String geofence : triggeringGeofencesList) {
                    if (geofence.contains("inner")) {
                        Log.i(TAG, "Exited inner geofence");
                        if (isDetailedDataCollectionStarted && activeInnerGeofences.isEmpty()) {
                            isDetailedDataCollectionStarted = false;
                            isScanningForLocks = false;

                            stopAccelerometerService();
                            stopBluetoothService();
                            stopWifiService();
                            stopPatternRecognitionService();

                            MainActivity.lockDoor.setVisibility(View.GONE);
                            MainActivity.unlockDoor.setVisibility(View.GONE);
                            MainActivity.lockView.setVisibility(View.VISIBLE);
                        }
                    } else if (geofence.contains("outer")) {
                        Log.i(TAG, "Entered outer geofence");

                        if (isLocationDataCollectionStarted && activeOuterGeofences.isEmpty()) {
                            isLocationDataCollectionStarted = false;
                            stopLocationService();
                        }
                    }
                }
            }
        }
    };

    private BroadcastReceiver startPatternRecognitionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Bundle extras = intent.getExtras();

            Log.e(TAG, "StartPatternRecognition");

            if ("START_PATTERNRECOGNITION".equals(action)) {
                startPatternRecognitionService();
            } else if ("STOP_PATTERNRECOGNITION".equals(action)) {
                isMoving = false;
                stopPatternRecognitionService();
                newRingBuffer();
                isDetailedDataCollectionStarted = false;
                isLocationDataCollectionStarted = false;
            } else if ("INCORRECT_UNLOCK".equals(action)) {
                dataStore.deleteCluster(extras.getInt("Cluster"));
                isDetailedDataCollectionStarted = true;
                isLocationDataCollectionStarted = true;

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                startAccelerometerService();
                startBluetoothService();
                startWifiService();
                startLocationService();
                startPatternRecognitionService();
                isDetailedDataCollectionStarted = true;
                isLocationDataCollectionStarted = true;

                // Check to see if training is needed after deleting incorrect clusters
                if (dataStore.getUnlockCount() < reqUnlockTraining) { trainingComplete = false; }
            } else if ("STOP_RECOGNISE".equals(action)) {
                stopAccelerometerService();
                stopBluetoothService();
                stopWifiService();
                stopLocationService();
                stopPatternRecognitionService();
                isScanningForLocks = false;
                isDetailedDataCollectionStarted = false;
                isLocationDataCollectionStarted = false;
            }
        }
    };


    void startAccelerometerService() {
        Log.v(TAG, "Starting AccelerometerService");
        Thread accelerometerServiceThread = new Thread() {
            public void run() {
                startService(accelerometerIntent);
            }
        };
        accelerometerServiceThread.start();
    }

    void stopAccelerometerService() {
        Log.d("CoreService", "Trying to stop accelerometerService");
        stopService(accelerometerIntent);
    }

    void startLocationService() {
        Log.v(TAG, "Starting LocationService");
        Thread locationServiceThread = new Thread() {
            public void run() {
                startService(locationIntent);
            }
        };
        locationServiceThread.start();
    }

    void stopLocationService() {
        stopService(locationIntent);
    }

    void startWifiService() {
        Log.v(TAG, "Starting WifiService");
        Thread wifiServiceThread = new Thread() {
            public void run() {
                startService(wifiIntent);
            }
        };
        wifiServiceThread.start();
    }

    void stopWifiService() {
        stopService(wifiIntent);
    }

    void startBluetoothService() {
        Log.v(TAG, "Starting BluetoothService");
        Thread bluetoothServiceThread = new Thread() {
            public void run() {
                startService(bluetoothIntent);
            }
        };
        bluetoothServiceThread.start();
    }

    void stopBluetoothService() {
        stopService(bluetoothIntent);
    }

    void startPatternRecognitionService() {
        isPatternRecognitionRunning = true;
        Log.v(TAG, "Starting PatternRecognitionService");
        Thread patternRecognitionServiceThread = new Thread() {
            public void run() {
                startService(patternRecognitionIntent);
            }
        };
        patternRecognitionServiceThread.start();
    }

    void stopPatternRecognitionService() {
        isPatternRecognitionRunning = false;
        WindowProcess.prevWindow = null;
        Log.d("CoreService", "Trying to stop PatternRecognitionService");
        stopService(patternRecognitionIntent);
    }

    void newRingBuffer() {
        // Initialize the new ring buffer
        windowBuffer = new RingBuffer(WindowData.class, windowBufferSize);
    }

    void addGeofences() {
        ArrayList<LockData> lockDataArrayList = dataStore.getKnownLocks();
        if (!lockDataArrayList.isEmpty()) {
            for (int i = 0; i < lockDataArrayList.size(); i++) {
                geofence.addGeofence(lockDataArrayList.get(i));
            }
            registerGeofences();
        }
    }

    void registerGeofences() {
        geofence.registerGeofences(this, mGoogleApiClient);
    }

    void unregisterGeofences() {
        geofence.unregisterGeofences(this, mGoogleApiClient);
    }

    // Store decision output values - used for confusion matrix
    public static void newTruePositive() { long time = System.currentTimeMillis(); dataStore.insertDecision(0, time); }
    public static void newFalseNegative() { long time = System.currentTimeMillis(); dataStore.insertDecision(1, time); }
    public static void newFalsePositive() { long time = System.currentTimeMillis(); dataStore.insertDecision(2, time); }
    public static void newTrueNegative() { long time = System.currentTimeMillis(); dataStore.insertDecision(3, time); }

    // Collect environmental data before saving the lock
    void saveLock(final String lockMAC) {
        new Thread(new Runnable() {
            public void run() {
                boolean success = true;
                String passphrase = "";

                startBluetoothService();
                startWifiService();
                startLocationService();

                try {
                    Thread.sleep(20000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                stopBluetoothService();
                stopWifiService();
                stopLocationService();

                if (success && recordedLocation.size() != 0) {
                    LocationData currentLocation = recordedLocation.get(recordedLocation.size() - 1);

                    LockData lockData = new LockData(
                            lockMAC,
                            passphrase,
                            currentLocation,
                            30,
                            100,
                            -1f,
                            recordedBluetooth,
                            recordedWifi
                    );
                    Log.d(TAG, lockData.toString());
                    newLock(lockData);
                } else {
                    Log.e(TAG, "No location found, cannot add lock");
                }
            }
        }).start();
    }

    // Insert the new lock into the database
    private boolean newLock(LockData lockData) {
        Log.d(TAG, "Inserting lock into db");
        dataStore.insertLockDetails(
                lockData.getMAC(),
                lockData.getPassphrase(),
                lockData.getLocation().getLatitude(),
                lockData.getLocation().getLongitude(),
                lockData.getInnerGeofence(),
                lockData.getOuterGeofence(),
                lockData.getOrientation(),
                System.currentTimeMillis()
        );

        for (int i = 0; i < lockData.getNearbyBluetoothDevices().size(); i++) {
            dataStore.insertBtle(
                    lockData.getNearbyBluetoothDevices().get(i).getName(),
                    lockData.getNearbyBluetoothDevices().get(i).getSource(),
                    lockData.getNearbyBluetoothDevices().get(i).getRssi(),
                    lockData.getMAC(),
                    lockData.getNearbyBluetoothDevices().get(i).getTime()
            );
        }

        for (int i = 0; i < lockData.getNearbyWifiAccessPoints().size(); i++) {
            dataStore.insertWifi(
                    lockData.getNearbyWifiAccessPoints().get(i).getSsid(),
                    lockData.getNearbyWifiAccessPoints().get(i).getMac(),
                    lockData.getNearbyWifiAccessPoints().get(i).getRssi(),
                    lockData.getMAC(),
                    lockData.getNearbyWifiAccessPoints().get(i).getTime()
            );
        }

        unregisterGeofences();
        addGeofences();
        registerGeofences();
        return true;
    }

    private void scanForLocks() {
        Log.e(TAG, "scanForLocks: " + activeInnerGeofences);
        Thread scannerServiceThread = new Thread() {
            public void run() {
                startService(scannerIntent);
            }
        };
        scannerServiceThread.start();
    }

    void addLock() {
        saveLock(BluetoothService.ANDERS_BEKEY);
        unlockNow();
    }

    void unlock() {
        if (dataStore.getKnownLocks().isEmpty()) {
            saveLock(BluetoothService.ANDERS_BEKEY);
        } else {
            if (isPatternRecognitionRunning) {
                stopPatternRecognitionService();
                isTraining = true;
            }
            handleUnlock();
        }
    }

    void lock() {
        if (dataStore.getKnownLocks().isEmpty()) {
            saveLock(BluetoothService.ANDERS_BEKEY);
        }
        lockNow();
    }

    void lockNow(){
        Toast.makeText(getApplicationContext(), "BeKey locked", Toast.LENGTH_SHORT).show();
    }

    void unlockNow() {
        Toast.makeText(getApplicationContext(), "BeKey unlocked", Toast.LENGTH_SHORT).show();
    }

    // Method to insert unlock session and evaluate if calibration is complete
    void handleUnlock() {
        // Take snapshot of the currently sequential data
        WindowData[] snapshot = RingBuffer.getSnapshot();

        // Insert the sequential data into the database
        dataStore.insertUnlock(snapshot);

        int cntUnlock = dataStore.getUnlockCount();

        // Checks if the calibration is complete
        if (cntUnlock >= reqUnlockTraining) {
            trainingComplete = true;
            Log.v(TAG, "START TRAINING");
            HMM = new ArrayList<>();

            // False negative condition as the door did not catch the unlock
            if (cntUnlock != reqUnlockTraining) {
                Log.v(TAG, "Inserting FN for unlocking");
                newFalseNegative();
            }

            // Start training procedure
            trainModel();

            Log.v(TAG, "TRAINING FINISHED");
            isTraining = false;
        }
    }

    // Initiate training of the HMM
    public void trainModel(){
        if (!dataStore.getUnlocks().isEmpty()) {
            new TrainingProcess(dataStore.getUnlocks());
        }
    }

    // Method to check if the unlock is already clustered
    public static boolean isUnlockClustered(int id) {
        return dataStore.isClustered(id);
    }

    // Method to update the cluster value
    public static void updateCluster(int cur_id, int next_id) {
        dataStore.updateCluster(cur_id, next_id);
    }

    // Method to retreive the cluster id
    public static int getClusterId(int id) {
        return dataStore.getClusterId(id);
    }

    public static void accelerometerEvent(AccelerometerData anAccelerometerEvent) {
        windowProcess.insertAccelerometerEventIntoWindow(anAccelerometerEvent);
    }

    // Method to export the database
    void exportDB() {
        Export.Database();
        Toast.makeText(getApplicationContext(), "Database exported", Toast.LENGTH_SHORT).show();
    }

    // Method to get a list of unlocks sessions
    public static ArrayList<UnlockData> getUnlocks() {
        return dataStore.getUnlocks();
    }

    // Method to check if the given environment is approved
    static boolean environmentApproved(String foundLock) {
        return Environment.makeDecision(foundLock);
    }
}
