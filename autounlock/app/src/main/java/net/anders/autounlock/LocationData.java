package net.anders.autounlock;

import java.io.Serializable;

public class LocationData implements Serializable {
    String provider;
    double latitude;
    double longitude;
    float accuracy;
    long time;

    public LocationData(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public LocationData(String provider, double latitude, double longitude, float accuracy, long time) {
        this.provider = provider;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.time = time;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(int accuracy) {
        this.accuracy = accuracy;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "LocationData{" +
                "provider='" + provider + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", accuracy=" + accuracy +
                ", time=" + time +
                '}';
    }
}
