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
    public double bestMatchProb = 0.00000000000000000000000001;
    public int bestMatchNo = -1;
    public int currentMatchNo = -1;

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

            System.out.println("Best match is RNN: " + bestMatchNo +  " with probability " + probability);

            // Send a notification to the user that the lock was successfully unlocked
            NotificationUtility notification = new NotificationUtility();
            notification.displayUnlockNotification(context, bestMatchNo+1);
            Log.v("Prob before reset = ", "" + probability);
            probability = 0;
            bestMatchNo = -1;
            bestMatchProb = 0.00000000000000000000000001;
            return true;
        }
        return false;
    }

    public void createSequenceData(WindowData[] snapshot){
        sequence = new Double[2][];
        Double[] ori = new Double[snapshot.length];
        Double[] vel = new Double[snapshot.length];
        for (int i = 0; i < snapshot.length; i++) {
            WindowData window = snapshot[i];
            ori[i] = window.getOrientation();
            vel[i] = window.getVelocity();
        }
        sequence[0] = ori;
        sequence[1] = vel;
    }


    // Evaluation problem - Forward-Backward Calculator
    public void getProbability(Double[][] sequence){

        //ForwardBackwardCalculator fbc = new ForwardBackwardCalculator(sequence, HMM);

        // Compute the probability with the Forward-Backward Calculator
        //probability = fbc.probability();
        try {
            //Outcommented while te
            //probability = RecurrentNN.getProbability(sequence);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


//        ViterbiCalculator vit = new ViterbiCalculator(sequence, hmm);
//        probability = vit.lnProbability();
//        System.out.println("FBC: - " + label + ": "+ probability);