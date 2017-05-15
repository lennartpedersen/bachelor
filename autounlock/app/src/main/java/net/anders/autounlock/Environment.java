package net.anders.autounlock;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class Environment {
    private static final String TAG = "Environment";

    static List<BluetoothData> recentBluetoothList;
    static List<WifiData> recentWifiList;

    static boolean makeDecision(String foundLock) {
        recentBluetoothList =  CoreService.recordedBluetooth;
        recentWifiList =  CoreService.recordedWifi;

        // Compare the recently recorded data with the stored data and give adapter score.
        double lockScore = 0;
        int validWifi = 0;
        int validBluetooth = 0;
        LockData storedLockData;
        storedLockData = CoreService.dataStore.getLockDetails(foundLock);

        if (!recentWifiList.isEmpty()) {
            for (WifiData storedWifi : storedLockData.getNearbyWifiAccessPoints()) {
                for (WifiData recentWifi : recentWifiList) {
                    if (storedWifi.getMac().equals(recentWifi.getMac())) {
                        validWifi++;
                    }
                }
            }
            if (validWifi != 0) {
                Log.i(TAG, "validWifi " + validWifi + " total wifi " + storedLockData.getNearbyWifiAccessPoints().size());
                lockScore += ((double) validWifi / (double) storedLockData.getNearbyWifiAccessPoints().size()) * 100;
            }
        }

        if (!recentBluetoothList.isEmpty()) {
            for (BluetoothData storedBluetooth : storedLockData.getNearbyBluetoothDevices()) {
                for (BluetoothData recentBluetooth : recentBluetoothList) {
                    if (storedBluetooth.getSource().equals(recentBluetooth.getSource())) {
                        validBluetooth++;
                    }
                }
            }
            if (validBluetooth != 0) {
                lockScore += ((double) validBluetooth / (double) storedLockData.getNearbyBluetoothDevices().size()) * 100;
            }
        }
        if (lockScore > 100) {
            return true;
        }
        return false;
    }
}