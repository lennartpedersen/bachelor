package net.anders.autounlock.MachineLearning;

import android.util.Log;

import net.anders.autounlock.CoreService;
import net.anders.autounlock.MachineLearning.UnlockData;
import net.anders.autounlock.MachineLearning.WindowData;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import be.ac.ulg.montefiore.run.jahmm.ObservationInteger;
import be.ac.ulg.montefiore.run.jahmm.ObservationReal;
import be.ac.ulg.montefiore.run.jahmm.ObservationVector;
import be.ac.ulg.montefiore.run.jahmm.OpdfGaussianFactory;
import be.ac.ulg.montefiore.run.jahmm.OpdfGaussianMixture;
import be.ac.ulg.montefiore.run.jahmm.OpdfMultiGaussianFactory;
import be.ac.ulg.montefiore.run.jahmm.io.OpdfIntegerWriter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.OpdfIntegerFactory;
import be.ac.ulg.montefiore.run.jahmm.io.HmmWriter;
import be.ac.ulg.montefiore.run.jahmm.learn.BaumWelchLearner;
import be.ac.ulg.montefiore.run.jahmm.learn.BaumWelchScaledLearner;
import be.ac.ulg.montefiore.run.jahmm.learn.KMeansLearner;

/**
 * Created by Anders on 06-03-2017.
 */

public class TrainModel {

    private static final String TAG = "TrainModel";

    // Lists of lists of values from multiple iterations (observations), used to create HMMs
    List<List<ObservationVector>> toHMM;

    //Hidden Markov Model of vector values of orientation and velocity
    public Hmm<ObservationVector> HMM;

    public void train(ArrayList<UnlockData> cluster) throws IOException, InterruptedException {
        toHMM = new LinkedList<>();

        // Loop through every unlock in the cluster
        for (UnlockData unlock : cluster) {

            // Transform unlock data into observation vectors used in HMM
            createSequenceData(unlock.getWindows());
        }
        // Construct Hidden Markov Model from lists of 'lists of vectors'
        HMM = createHmm(toHMM);

        // Add the newly constructed HMM to the list of HMMs
        CoreService.HMM.add(HMM);
    }

    // Create observation vectors
    public void createSequenceData(List<WindowData> windows){
        List<ObservationVector> vectors = new LinkedList<>();

        for (WindowData window : windows) {
            double newOri = window.getOrientation();
            double newVelo = window.getVelocity();

            // Add new vector to the list from orientation and velocity data
            vectors.add(new ObservationVector(new double[]{newOri, newVelo}));
        }
        toHMM.add(vectors);
    }

    // Training problem - Baum Welch
    public Hmm<ObservationVector> createHmm(List<List<ObservationVector>> toHMM) {
        // K-means algorithm to find clusters with the use of centroids
        KMeansLearner<ObservationVector> kml = new KMeansLearner<ObservationVector>(5, new OpdfMultiGaussianFactory(2), toHMM);
        // K-means iterate function returns a better approximation of a matching HMM
        // and will stop when the approximation can not improve
        Hmm model = kml.iterate();
        // We can now build a BaumWelchLearner object that can find an HMM
        // fitted to the observation sequences we've just generated
        BaumWelchLearner bwl = new BaumWelchLearner();
        bwl.setNbIterations(10);
        model = bwl.learn(model, toHMM);
        return model;
    }
}
