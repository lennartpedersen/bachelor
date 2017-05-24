package net.anders.autounlock.MachineLearning;

import android.content.Context;

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

    public Double[][] sequence;
    public double probability = 0;
    public double bestMatchProb = 0.00000000000000000000000001;
    public int bestMatchNo = -1;
    public int currentMatchNo = -1;

    public boolean recognise(Context context, WindowData[] snapshot) {
        // Transform sequential data into a list of vector sequence
        createSequenceData(snapshot);

        // Compute score of the sequence data
        evaluateSequence(sequence);

        // Check if a match was found
        if (bestMatchNo < 0){
            System.out.println("Unable to recognise sequence");
        }
        else {
            System.out.println("Best match is RNN: " + bestMatchNo +  " with probability " + bestMatchProb);

            // Send a notification to the user that the lock was successfully unlocked
            NotificationUtility notification = new NotificationUtility();
            notification.displayUnlockNotification(context, bestMatchNo+1);

            bestMatchNo = -1;
            bestMatchProb = 0.00000000000000000000000001;
            return true;
        }
        return false;
    }

    public void createSequenceData(WindowData[] snapshot){
        sequence = new Double[snapshot.length][];

        for (int i = 0; i < snapshot.length; i++) {
            WindowData window = snapshot[i];
            sequence[i] = new Double[]{window.getOrientation(), window.getVelocity()};
        }
    }

    public void evaluateSequence(Double[][] sequence){
        for (int i = 0; CoreService.RNN.keySet().size() > i; i++) {
            currentMatchNo = i;

            // Compute probability of HMM and the concurrent sequential data
            getProbability(sequence);

            System.out.println("RNN: " + i + ": " + probability);

            // Check if the probability is greater than the currently highest recorded
            if (probability !=0 && probability > bestMatchProb) {
                bestMatchProb = probability;
                bestMatchNo = currentMatchNo;
            }
        }
    }

    // Evaluation problem - Forward-Backward Calculator
    public void getProbability(Double[][] sequence){

        //ForwardBackwardCalculator fbc = new ForwardBackwardCalculator(sequence, HMM);

        // Compute the probability with the Forward-Backward Calculator
        //probability = fbc.probability();
        try {
            probability = RecurrentNN.getProbability(sequence);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


//        ViterbiCalculator vit = new ViterbiCalculator(sequence, hmm);
//        probability = vit.lnProbability();
//        System.out.println("FBC: - " + label + ": "+ probability);