package net.anders.autounlock.MachineLearning;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import net.anders.autounlock.CoreService;
import net.anders.autounlock.RingBuffer;

/**
 * Created by Anders on 22-02-2017.
 */

public class PatternRecognitionService extends Service {
    private volatile boolean running = true;

    private PatternRecognition patternRecognition;
    private Thread recognitionThread;

    private RecogniseSequence recognise;

    private static String TAG = "RecognitionService";
    private Intent stopRecognise = new Intent("STOP_RECOGNISE");

    @Override
    public void onCreate() {
        patternRecognition = new PatternRecognition();
        recognitionThread = new Thread(patternRecognition);
        recognitionThread.start();

        recognise = new RecogniseSequence();
    }

    @Override
    public void onDestroy() {
        patternRecognition.terminate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class PatternRecognition implements Runnable {

        @Override
        public void run() {

            Intent startRecognition = new Intent("START_RECOGNITION");
            sendBroadcast(startRecognition);

            while (running) {
                if (CoreService.isPatternRecognitionRunning) {

                    // Ensure that the device is currently moving
                    if (CoreService.isMoving) {

                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        Log.i(TAG, "INITIATE RECOGNITION");
                        // Get current snapshot as the sequential data
                        WindowData[] snapshot = RingBuffer.getSnapshot();

                        // Initiate recognition procedure of the snapshot
                        if (recognise.recognise(getApplicationContext(), snapshot)) {

                            // If the sequential data was recognised as correcet,
                            // stop the recognition
                            Intent startDecision = new Intent("STOP_PATTERNRECOGNITION");
                            sendBroadcast(startDecision);

                            sendBroadcast(stopRecognise);
                            running = false;
                            stopSelf();
                        } else {
                            CoreService.newTrueNegative();
                        }

                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    stopSelf();
                }
            }
        }
        private void terminate() {
            running = false;
        }
    }
}