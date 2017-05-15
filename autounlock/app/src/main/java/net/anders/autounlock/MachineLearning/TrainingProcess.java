package net.anders.autounlock.MachineLearning;

import android.util.Log;

import net.anders.autounlock.CoreService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Anders on 09-03-2017.
 */

public class TrainingProcess {

    private static final String TAG = "TrainingProcess";

    public TrainingProcess(ArrayList<UnlockData> unlocks) {
        training(unlocks);
    }

    // Initiate the training process
    public void training(ArrayList<UnlockData> unlocks) {
        // Create a map to hold sub lists of clusters
        Map<Integer, ArrayList<UnlockData>> map = new HashMap<>();
        // Method to identify clusters in the training unlocks
        ArrayList<UnlockData> clusters = analyseClusters(unlocks);
        for (UnlockData unlock : clusters) {
            // Fetch the list for this object's cluster id
            ArrayList<UnlockData> temp = map.get(unlock.getClusterId());

            if (temp == null) {
                // If the list is null we haven't seen an
                // object with this cluster id before, so create
                // a new list and add it to the map
                temp = new ArrayList<UnlockData>();
                map.put(unlock.getClusterId(), temp);
            }
            temp.add(unlock);
        }
        // Method to create models of clusters of the same unlocks
        for (Map.Entry<Integer, ArrayList<UnlockData>> entry : map.entrySet()) {
            TrainModel model = new TrainModel();

            ArrayList<UnlockData> cluster = new ArrayList<>();

            for (UnlockData unlock : entry.getValue()) {
                cluster.add(unlock);
            }
            // Skip cluster id with 0, as they have not been assigned yet
            // Since every group of clusters has the same id, check only first
            if (cluster.get(0).cluster_id != 0) {
                try {
                    model.train(cluster);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ArrayList<UnlockData> analyseClusters(ArrayList<UnlockData> unlocks) {
        ArrayList<UnlockData> clusters = new ArrayList<>();
        for (int i = 0; i < unlocks.size(); i++) {
            UnlockData currentUnlock = unlocks.get(i);
            // If the unlock is already clustered, skip
            if (!CoreService.isUnlockClustered(i+1)) {
                for (int j = 0; j < unlocks.size(); j++) {
                    if(j!=i){
                        UnlockData nextUnlock = unlocks.get(j);
                        boolean cluster = true;

                        // Compares each window in current unlock with against the next unlock
                        for (int k = 0; k < currentUnlock.getWindows().size(); k++) {
                            WindowData currentWindow = currentUnlock.getWindows().get(k);
                            WindowData nextWindow = nextUnlock.getWindows().get(k);

                            // Break if the windows are not within the required thresholds,
                            // else continue to investigate if unlocks should be clustered
                            if (!similarity(currentWindow, nextWindow)) {
                                cluster = false;
                                Log.i(TAG, "CLUSTER NOT EXISTING: Unlock " + String.valueOf(i) + " and " + String.valueOf(i+1));
                                break;
                            }
                        }
                        if (cluster) {
                            // Update the two unlocks to be clustered together
                            Log.i(TAG, "CLUSTER FOUND: Unlock " + String.valueOf(i) + " and " + String.valueOf(i+1));
                            CoreService.updateCluster(i+1, j+1); // Current train
                            break;
                        }
                    }
                }
            }
            clusters.add(new UnlockData(currentUnlock.getId(), CoreService.getClusterId(i+1), currentUnlock.getWindows()));
        }
        return clusters;
    }

    private boolean similarity(WindowData currentWindow, WindowData nextWindow) {

        // Current orientation and velocity of the current and next window of the unlocks
        double c_ori = currentWindow.getOrientation();
        double c_velo = currentWindow.getVelocity();
        double n_ori = nextWindow.getOrientation();
        double n_velo = nextWindow.getVelocity();

        // Check if the orientation and velocity are within threshold
        if ((Math.abs(c_ori - n_ori) < CoreService.orientationThreshold) &&
                (Math.abs(c_velo - n_velo) < CoreService.velocityThreshold)) {
            return true;
        }
        return false;
    }
}