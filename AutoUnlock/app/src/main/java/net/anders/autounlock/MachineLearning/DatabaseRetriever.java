package net.anders.autounlock.MachineLearning;

import net.anders.autounlock.CoreService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tom-fire on 16/05/2017.
 */

public class DatabaseRetriever {
    private static int K_UNLOCK      = 1;
    private static int K_ORIENTATION = 6;
    private static int K_VELOCITY    = 7;


    private static Map<Integer, Double[][]> tupleDict = new HashMap<>();

    public static void readOldData() throws Exception {
        //InputStream is = new FileInputStream(new File("C:\\Users\\Lennart Pedersen\\Dropbox\\Bachelor\\databases\\AutoUnlock-13.csv"));
        //BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        //reader.readLine(); // To get rid of attribute titles

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


        /*try {
            String line;
            String lastUnlockID = "1";
            while((line=reader.readLine()) != null) {
                String[] RowData = line.split(",");
                String newUnlockID = RowData[K_UNLOCK];

                if (lastUnlockID.equals(newUnlockID)) {
                    orientation.add(Double.parseDouble(RowData[K_ORIENTATION]));
                    velocity.add(Double.parseDouble(RowData[K_VELOCITY]));
                } else {
                    Double[][] tuples = convertToTupleArray(orientation, velocity);

                    int index = Integer.parseInt(lastUnlockID);


                    CoreService.RNN.put(index-1, tuples);
                    //tupleDict.put(index-1, tuples);
                    lastUnlockID = newUnlockID;

                    velocity = new ArrayList<>();
                    orientation = new ArrayList<>();
                    orientation.add(Double.parseDouble(RowData[K_ORIENTATION]));
                    velocity.add(Double.parseDouble(RowData[K_VELOCITY]));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/
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

    public static Map<Integer, Double[][]> getTupleDict() throws Exception {
        if (tupleDict.isEmpty()) {
            readOldData();
        }
        return tupleDict;
    }

    /*public static Double[][] readTestData() throws Exception {
        InputStream is = new FileInputStream(new File("C:\\Users\\Lennart Pedersen\\Dropbox\\Bachelor\\databases\\AutoUnlock-13-test.csv"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        //reader.readLine(); // To get rid of attribute titles

        ArrayList<Double> velocity = new ArrayList<>();
        ArrayList<Double> orientation = new ArrayList<>();

        try {
            String line;
            String lastUnlockID = "15";
            while((line=reader.readLine()) != null) {
                String[] RowData = line.split(",");
                String newUnlockID = RowData[K_UNLOCK];

                if (lastUnlockID.equals(newUnlockID)) {
                    orientation.add(Double.parseDouble(RowData[K_ORIENTATION]));
                    velocity.add(Double.parseDouble(RowData[K_VELOCITY]));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return convertToTupleArray(orientation, velocity);
    }*/

}
