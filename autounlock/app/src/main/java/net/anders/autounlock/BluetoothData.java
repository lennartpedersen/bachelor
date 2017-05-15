package net.anders.autounlock;

import java.io.Serializable;

public class BluetoothData implements Serializable{
    String name;
    String source;
    int rssi;
    long time;

    public BluetoothData(String name, String source, int rssi, long time) {
        this.name = name;
        this.source = source;
        this.rssi = rssi;
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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
        return "BluetoothData{" +
                "name='" + name + '\'' +
                ", source='" + source + '\'' +
                ", rssi=" + rssi +
                ", time=" + time +
                '}';
    }
}
