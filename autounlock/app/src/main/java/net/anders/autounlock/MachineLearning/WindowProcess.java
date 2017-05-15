package net.anders.autounlock.MachineLearning;

import net.anders.autounlock.AccelerometerData;
import net.anders.autounlock.CoreService;
import net.anders.autounlock.RingBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Anders on 21-02-2017.
 */

public class WindowProcess {

    private static final String TAG = "WindowProcess";

    private List<AccelerometerData> currentAccelerometerList = new ArrayList<>();
    private List<AccelerometerData> nextAccelerometerList = new ArrayList<>();
    public static WindowData prevWindow;

    public void insertAccelerometerEventIntoWindow(
            AccelerometerData anAccelerometerEvent) {

        currentAccelerometerList.add(anAccelerometerEvent);

        // Numbers of overlapping values in integers
        int overlap = CoreService.windowOverlap;

        // Adds accelerometer data if it is needed for the next sliding window
        if (overlap < currentAccelerometerList.size()) {
            nextAccelerometerList.add(anAccelerometerEvent);
        }

        // Convert current accelerometer data segments into a window
        if (currentAccelerometerList.size() == CoreService.windowSize) {

            // Process window and add it to ring buffer
            processWindow(currentAccelerometerList);

            // Ensures that the accelerometer data needed for next window is stored
            currentAccelerometerList.addAll(nextAccelerometerList);
            nextAccelerometerList.clear();
        }
    }

    private void processWindow(List<AccelerometerData> rawAccelerometerData) {

        // Method to build a window based on the current accelerometer data and
        WindowData window = buildWindow(rawAccelerometerData, prevWindow);

        // If the windows magnitude accelerometer rate is below threshold, don't use data
        if (window.getAccelerationMag() > CoreService.activityThreshold) {
            // Put new window into the circular buffer
            RingBuffer.addWindow(window);

            if (CoreService.trainingComplete) {
                CoreService.isMoving = true;
            }
        }
        CoreService.isMoving = false;
        prevWindow = window;
        currentAccelerometerList.clear();
    }

    public WindowData buildWindow(List<AccelerometerData> rawAccelerometerData, WindowData prevWindow) {
        float meanAccX, meanAccY, meanOri, meanMag;
        float sumAccX = 0, sumAccY = 0, sumOri = 0, sumMag = 0;
        double speedX, speedY, time_current, time_prev;
        double speedX_prev = 0, speedY_prev = 0;

        // Ensures that if the previous window is null, the speed is not set
        if (prevWindow != null) {
            speedX_prev = prevWindow.getSpeedX();
            speedY_prev = prevWindow.getSpeedY();
            time_prev = prevWindow.getTime();
        } else {
            time_prev = 0;
        }

        // Finds the sum of accelerometer x and y, the orientation and the magnitude of accelerometer
        for (AccelerometerData acc : rawAccelerometerData) {
            sumAccX += acc.getAccelerationX();
            sumAccY += acc.getAccelerationY();
            sumOri += acc.getOrientation();
            sumMag += Math.sqrt(acc.getAccelerationX()*acc.getAccelerationX()
                    + acc.getAccelerationY()*acc.getAccelerationY()
                    + acc.getAccelerationZ()*acc.getAccelerationZ());
        }

        // Find the means of the values
        meanAccX = sumAccX / rawAccelerometerData.size();
        meanAccY = sumAccY / rawAccelerometerData.size();
        meanOri = sumOri / rawAccelerometerData.size();
        meanMag = sumMag / rawAccelerometerData.size();

        time_current = System.currentTimeMillis() * Math.pow(10, -3);

        // If the previous speed is 0 m/s, the device has not moved before
        // and the time factor between current and next is not possible
        if (speedX_prev ==  0 && speedY_prev == 0) {
            speedX = speedX_prev + meanAccX;
            speedY = speedY_prev + meanAccY;
        } else {
            speedX = speedX_prev + meanAccX * Math.pow(time_current - time_prev, 2);
            speedY = speedY_prev + meanAccY * Math.pow(time_current - time_prev, 2);
        }

        // Compute the velocity from the speed on the x and y axes
        double velocity = Math.sqrt(Math.pow(speedX, 2) + Math.pow(speedY, 2));

        // Save the collected data into a window
        return new WindowData(meanAccX, meanAccY,
                speedX, speedY, meanOri,
                velocity, meanMag, time_current);
    }
}
