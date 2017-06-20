package net.anders.autounlock.MachineLearning;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import net.anders.autounlock.CoreService;
import net.anders.autounlock.NotificationUtility;

import java.util.ArrayList;
import java.util.List;

import be.ac.ulg.montefiore.run.jahmm.ForwardBackwardCalculator;
import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.ObservationVector;
import be.ac.ulg.montefiore.run.jahmm.ViterbiCalculator;

/**
 * Created by Anders on 06-03-2017.
 */

public class RecogniseSequence {

    private static String TAG = "RecogniseSequence";

    public static Double[][] sequence;
    public static double probability = 0;

    public static Double[][] getSequence() {
        return sequence;
    }

    public static double getProbability() {
        return probability;
    }

    public boolean recognise(Context context, WindowData[] snapshot) {
        // Transform sequential data into a list of vector sequence
        createSequenceData(snapshot);
        Log.v("tryRecognise","...");
        // Compute score of the sequence data
        getProbability(sequence);
        Log.v("recogniseSequence", "Prob = " + probability);
        // Check if a match was found
        if (probability > 0.5){

            System.out.println("Best match is RNN: " + " with probability " + probability);

            // Send a notification to the user that the lock was successfully unlocked
            NotificationUtility notification = new NotificationUtility();
            notification.displayUnlockNotification(context);
            Log.v("Prob before reset = ", "" + probability);
            probability = 0;
            return true;
        }
        return false;
    }

    public void createSequenceData(WindowData[] snapshot){
        sequence = new Double[3][];
        Double[] ori = new Double[snapshot.length];
        Double[] accX = new Double[snapshot.length];
        Double[] accY = new Double[snapshot.length];
        for (int i = 0; i < snapshot.length; i++) {
            WindowData window = snapshot[i];
            ori[i] = window.getOrientation();
            accX[i] = window.getAccelerationX();
            accY[i] = window.getAccelerationY();
        }
        sequence[0] = ori;
        sequence[1] = accX;
        sequence[2] = accY;
    }


    // Evaluation problem - Forward-Backward Calculator
    public static void getProbability(Double[][] sequence){
        try {
            probability = RecurrentNN2.getProbability(sequence);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}