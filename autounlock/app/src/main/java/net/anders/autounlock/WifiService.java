package net.anders.autounlock;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class WifiService extends Service {
    private static String TAG = "WifiService";

    int startMode;       // indicates how to behave if the service is killed
    IBinder binder;      // interface for clients that bind
    boolean allowRebind; // indicates whether onRebind should be used

    WifiManager wifiManager;

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> scanResults = wifiManager.getScanResults();
                long time = System.currentTimeMillis();

                CoreService.recordedWifi = new ArrayList<>();
                for (int i = 0; i < scanResults.size(); i++) {
                    String SSID = scanResults.get(i).SSID;
                    String MAC = scanResults.get(i).BSSID;
                    int RSSI = scanResults.get(i).level;

                    WifiData aWifiDevice = new WifiData(SSID, MAC, RSSI, time);
                    CoreService.recordedWifi.add(aWifiDevice);
                }
            }
            // We force the device to scan for wifi again when we have finished the previous scan.
            wifiManager.startScan();
        }
    };

    @Override
    public void onCreate() {
        // The service is being created
        powerManager = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "WifiService");
        wakeLock.acquire();

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        registerReceiver(broadcastReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiManager.createWifiLock(String.valueOf(WifiManager.WIFI_MODE_SCAN_ONLY)).acquire();
        wifiManager.startScan();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to adapter call to startService()
        return startMode;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return allowRebind;
    }

    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        Log.v("WifiService", "Stopping, do we still have wifi lock?");
        //wifiManager.createWifiLock(String.valueOf(WifiManager.WIFI_MODE_SCAN_ONLY)).release();
        wakeLock.release();
        unregisterReceiver(broadcastReceiver);
    }
}