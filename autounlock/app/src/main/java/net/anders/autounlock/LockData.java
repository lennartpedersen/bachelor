package net.anders.autounlock;

import java.util.ArrayList;
import java.util.List;

class LockData {
    private String MAC;
    private String passphrase;
    private LocationData location;
    private float innerGeofence;
    private float outerGeofence;
    private float orientation;
    private List<BluetoothData> nearbyBluetoothDevices;
    private List<WifiData> nearbyWifiAccessPoints;

    LockData(String MAC) {
        this.MAC = MAC;
    }

    LockData(String MAC, LocationData location, float innerGeofence, float outerGeofence) {
        this.MAC = MAC;
        this.location = location;
        this.innerGeofence = innerGeofence;
        this.outerGeofence = outerGeofence;
    }

    LockData(String MAC, LocationData location, float innerGeofence, float outerGeofence, float orientation) {
        this.MAC = MAC;
        this.location = location;
        this.innerGeofence = innerGeofence;
        this.outerGeofence = outerGeofence;
        this.orientation = orientation;
    }

    LockData(String MAC, String passphrase, LocationData location, float innerGeofence, float outerGeofence,
             float orientation, List<BluetoothData> nearbyBluetoothDevices, List<WifiData> nearbyWifiAccessPoints) {
        this.MAC = MAC;
        this.passphrase = passphrase;
        this.location = location;
        this.innerGeofence = innerGeofence;
        this.outerGeofence = outerGeofence;
        this.orientation = orientation;
        this.nearbyBluetoothDevices = nearbyBluetoothDevices;
        this.nearbyWifiAccessPoints = nearbyWifiAccessPoints;
    }

    public String getMAC() {
        return MAC;
    }

    public void setMAC(String MAC) {
        this.MAC = MAC;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public LocationData getLocation() {
        return location;
    }

    public void setLocation(LocationData location) {
        this.location = location;
    }

    public float getInnerGeofence() {
        return innerGeofence;
    }

    public void setInnerGeofence(int innerGeofence) {
        this.innerGeofence = innerGeofence;
    }

    public float getOuterGeofence() {
        return outerGeofence;
    }

    public void setOuterGeofence(int outerGeofence) {
        this.outerGeofence = outerGeofence;
    }

    public float getOrientation() {
        return orientation;
    }

    public void setOrientation(float orientation) {
        this.orientation = orientation;
    }

    public List<BluetoothData> getNearbyBluetoothDevices() {
        return nearbyBluetoothDevices;
    }

    public void setNearbyBluetoothDevices(ArrayList<BluetoothData> nearbyBluetoothDevices) {
        this.nearbyBluetoothDevices = nearbyBluetoothDevices;
    }

    public List<WifiData> getNearbyWifiAccessPoints() {
        return nearbyWifiAccessPoints;
    }

    public void setNearbyWifiAccessPoints(ArrayList<WifiData> nearbyWifiAccessPoints) {
        this.nearbyWifiAccessPoints = nearbyWifiAccessPoints;
    }

    @Override
    public String toString() {
        return "LockData{" +
                "MAC='" + MAC + '\'' +
                ", passphrase='" + passphrase + '\'' +
                ", location=" + location +
                ", innerGeofence=" + innerGeofence +
                ", outerGeofence=" + outerGeofence +
                ", nearbyBluetoothDevices=" + nearbyBluetoothDevices +
                ", nearbyWifiAccessPoints=" + nearbyWifiAccessPoints +
                '}';
    }
}

