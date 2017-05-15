package net.anders.autounlock;

public class AccelerometerData {
    float accelerationX;
    float accelerationY;
    float accelerationZ;
    long time;
    float orientation;


    public AccelerometerData(float accelerationX, float accelerationY, float accelerationZ, long time, float orientation) {
        this.accelerationX = accelerationX;
        this.accelerationY = accelerationY;
        this.accelerationZ = accelerationZ;
        this.time = time;
        this.orientation = orientation;
    }

    public float getAccelerationX() {
        return accelerationX;
    }

    public void setAccelerationX(float accelerationX) {
        this.accelerationX = accelerationX;
    }

    public float getAccelerationY() {
        return accelerationY;
    }

    public void setAccelerationY(float accelerationY) {
        this.accelerationY = accelerationY;
    }

    public float getAccelerationZ() {
        return accelerationZ;
    }

    public void setAccelerationZ(float accelerationZ) {
        this.accelerationZ = accelerationZ;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public float getOrientation() { return orientation; }

    public void setOrientation(float orientation) { this.orientation = orientation; }

    @Override
    public String toString() {
        return "AccelerometerData{" +
                "accelerationX=" + accelerationX +
                ", accelerationY=" + accelerationY +
                ", accelerationZ=" + accelerationZ +
                ", time=" + time +
                ", orientation=" + orientation +
                '}';
    }
}
