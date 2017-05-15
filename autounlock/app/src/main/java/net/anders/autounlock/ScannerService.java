package net.anders.autounlock;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class ScannerService extends Service {
    private volatile boolean running = true;

    private Scanner scanner;
    private Thread scannerThread;

    private static final String TAG = "ScannerService";

    private Intent stopScan = new Intent("STOP_SCAN");

    @Override
    public void onCreate() {
        scanner = new Scanner();
        scannerThread = new Thread(scanner);
        scannerThread.start();
    }

    @Override
    public void onDestroy() {
        scanner.terminate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class Scanner implements Runnable {

        @Override
        public void run() {
            CoreService.isScanningForLocks = true;
            List<String> foundLocks = new ArrayList<String>();
            ArrayList<String> decisionLocks = new ArrayList<String>();

            while (running) {
                for (BluetoothData bluetoothData : CoreService.recordedBluetooth) {
                    if (CoreService.activeInnerGeofences.contains(bluetoothData.getSource())) {
                        foundLocks.add(bluetoothData.getSource());
                    }
                }
                if (!foundLocks.isEmpty() && !CoreService.recordedLocation.isEmpty()) {
                    for (String foundLock : foundLocks) {
                        decisionLocks.add(foundLock);
                    }
                    if (!decisionLocks.isEmpty()) {
                        if (!CoreService.isPatternRecognitionRunning &&
                                !CoreService.isTraining &&
                                !CoreService.HMM.isEmpty() &&
                                CoreService.trainingComplete) {
                            if (CoreService.environmentApproved(decisionLocks.get(0).toString())) {
                                Intent startDecision = new Intent("START_PATTERNRECOGNITION");
                                sendBroadcast(startDecision);
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void terminate() {
            sendBroadcast(stopScan);
            running = false;
        }
    }
}
