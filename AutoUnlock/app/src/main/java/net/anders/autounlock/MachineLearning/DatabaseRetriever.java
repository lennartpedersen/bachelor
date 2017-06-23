package net.anders.autounlock.MachineLearning;

import net.anders.autounlock.CoreService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tom-fire on 16/05/2017.
 */

public class DatabaseRetriever {

    public static void readOldData() throws Exception {
        ArrayList<Double> accelerationX = new ArrayList<>();
        ArrayList<Double> accelerationY = new ArrayList<>();
        ArrayList<Double> orientation = new ArrayList<>();
        int unlockID = 0;
        for (ArrayList<WindowData> aw : CoreService.getUnlocks()) {
            for (WindowData w : aw) {
                unlockID = w.getId();
                accelerationX.add(w.getAccelerationX());
                accelerationY.add(w.getAccelerationY());
                orientation.add(w.getOrientation());
            }

            Double[][] tuples = convertToTupleArray(orientation, accelerationX, accelerationY);
            //UnlockID-1 as database is 1-indexed instead of 0-indexed.
            CoreService.RNN.put(unlockID-1, tuples);

            accelerationX = new ArrayList<>();
            accelerationY = new ArrayList<>();
            orientation = new ArrayList<>();
        }


    }


    public static int getUnlockValue(int id) {
        return CoreService.getUnlockValue(id);
    }

    private static Double[][] convertToTupleArray(ArrayList<Double> orientation, ArrayList<Double> accelerationX, ArrayList<Double> accelerationY) {
        int length = orientation.size();

        Double[][] array = new Double[3][];
        array[0] = new Double[length];
        array[1] = new Double[length];
        array[2] = new Double[length];

        for (int i = 0; i < length; i++) {
            array[0][i] = orientation.get(i);
            array[1][i] = accelerationX.get(i);
            array[2][i] = accelerationY.get(i);
        }

        return array;
    }

}
