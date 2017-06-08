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

    public WindowData(int id, double speedX, double speedY, double orientation, double velocity, double accelerationMag, double time) {
        this.id = id;
        this.speedX = speedX;
        this.speedY = speedY;
        this.orientation = orientation;
        this.velocity = velocity;
        this.accelerationMag = accelerationMag;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getSpeedX() {
        return speedX;
    }
    public void setSpeedX(double speedX) {
        this.speedX = speedX;
    }

    public double getSpeedY() {
        return speedY;
    }
    public void setSpeedY(double speedY) {
        this.speedY = speedY;
    }

    public double getOrientation() {
        return orientation;
    }
    public void setOrientation(double orientation) {
        this.orientation = orientation;
    }

    public double getVelocity() {
        return velocity;
    }
    public void setVelocity(double velocity) { this.velocity = velocity;}

    public double getAccelerationMag() {
        return accelerationMag;
    }
    public void setAccelerationMag(double accelerationMag) {
        this.accelerationMag = accelerationMag;
    }

    public double getTime() {
        return time;
    }
    public void setTime(double time) {
        this.time = time;
    }
}

