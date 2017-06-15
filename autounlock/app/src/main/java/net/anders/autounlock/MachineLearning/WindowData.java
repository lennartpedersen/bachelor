package net.anders.autounlock.MachineLearning;

/**
 * Created by Anders on 22-02-2017.
 */

public class WindowData {
    int id;
    double accelerationX;
    double accelerationY;
    double speedX;
    double speedY;
    double orientation;
    double velocity;
    double accelerationMag;
    double time;



    public WindowData(int id, double accelerationX, double accelerationY, double orientation, double accelerationMag, double time) {
        this.id = id;
        this.accelerationX = accelerationX;
        this.accelerationY = accelerationY;
        this.orientation = orientation;
        this.accelerationMag = accelerationMag;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getOrientation() {
        return orientation;
    }
    public void setOrientation(double orientation) {
        this.orientation = orientation;
    }

    public double getAccelerationMag() {
        return accelerationMag;
    }
    public void setAccelerationMag(double accelerationMag) {
        this.accelerationMag = accelerationMag;
    }

    public double getAccelerationX() {
        return accelerationX;
    }

    public double getAccelerationY() {
        return accelerationY;
    }

    public double getTime() {
        return time;
    }
    public void setTime(double time) {
        this.time = time;
    }
}

