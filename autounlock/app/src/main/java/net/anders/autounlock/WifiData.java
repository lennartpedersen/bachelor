package net.anders.autounlock;

import java.io.Serializable;

public class WifiData implements Serializable {
    String ssid;
    String mac;
    int rssi;
    long time;

    public WifiData(String ssid, String mac, int rssi, long time) {
        this.ssid = ssid;
        this.mac = mac;
        this.rssi = rssi;
        this.time = time;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "WifiData{" +
                "ssid='" + ssid + '\'' +
                ", mac='" + mac + '\'' +
                ", rssi=" + rssi +
                ", time=" + time +
                '}';
    }
}
