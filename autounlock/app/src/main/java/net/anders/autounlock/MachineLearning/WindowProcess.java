package net.anders.autounlock.MachineLearning;

import net.anders.autounlock.SensorData;
import net.anders.autounlock.CoreService;
import net.anders.autounlock.RingBuffer;

import java.util.ArrayList;
import java.util.List;


public class WindowProcess {

    private static final String TAG = "WindowProcess";

    private List<SensorData> currentSensorList = new ArrayList<>();
    private List<SensorData> nextSensorList = new ArrayList<>();

    public void insertSensorEventIntoWindow(
            SensorData aSensorEvent) {

        currentSensorList.add(aSensorEvent);

        // Numbers of overlapping values in integers
        int overlap = CoreService.windowOverlap;

        // Adds accelerometer data if it is needed for the next sliding window
        if (overlap < currentSensorList.size()) {
            nextSensorList.add(aSensorEvent);
        }

        // Convert current accelerometer data segments into a window
        if (currentSensorList.size() == CoreService.windowSize) {

            // Process window and add it to ring buffer
            processWindow(currentSensorList);

            // Ensures that the accelerometer data needed for next window is stored
            currentSensorList.addAll(nextSensorList);
            nextSensorList.clear();
        }
    }

    private void processWindow(List<SensorData> rawSensorData) {

        // Method to build a window based on the current accelerometer data and
        WindowData window = buildWindow(rawSensorData);

        // If the windows magnitude accelerometer rate is below threshold, don't use data
        if (window.getAccelerationMag() > CoreService.activityThreshold) {
            // Put new window into the circular buffer
            RingBuffer.addWindow(window);

            if (CoreService.trainingComplete) {
                CoreService.isMoving = true;
            }
        }
        CoreService.isMoving = false;
        currentSensorList.clear();
    }

    public WindowData buildWindow(List<SensorData> rawSensorData) {
        float meanAccX, meanAccY, meanOri, meanMag;
        float sumAccX = 0, sumAccY = 0, sumOri = 0, sumMag = 0;
        double time_current;

        // Finds the sum of accelerometer x and y, the orientation and the magnitude of accelerometer
        for (SensorData acc : rawSensorData) {
            sumAccX += acc.getAccelerationX();
            sumAccY += acc.getAccelerationY();
            sumOri += acc.getOrientation();
            sumMag += Math.sqrt(acc.getAccelerationX()*acc.getAccelerationX()
                    + acc.getAccelerationY()*acc.getAccelerationY()
                    + acc.getAccelerationZ()*acc.getAccelerationZ());
        }

        // Find the means of the values
        meanAccX = sumAccX / rawSensorData.size();
        meanAccY = sumAccY / rawSensorData.size();
        meanOri = sumOri / rawSensorData.size();
        meanMag = sumMag / rawSensorData.size();

        time_current = System.currentTimeMillis() * Math.pow(10, -3);

        // Save the collected data into a window
        //-1 returned as id - does not require unlockid until put in database
        return new WindowData(-1, meanAccX, meanAccY, meanOri, meanMag, time_current);
    }
}
