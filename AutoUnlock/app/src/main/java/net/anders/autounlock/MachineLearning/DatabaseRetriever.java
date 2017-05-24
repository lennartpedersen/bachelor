package net.anders.autounlock.MachineLearning;

import net.anders.autounlock.CoreService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        //InputStream is = new FileInputStream(new File("C:\\Users\\TSN\\Documents\\Gits\\bachelor\\databases\\data1.csv"));
        //BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        //reader.readLine(); // To get rid of attribute titles

        ArrayList<Double> velocity = new ArrayList<>();
        ArrayList<Double> orientation = new ArrayList<>();


        for (UnlockData u : CoreService.getUnlocks())
        {
            int unlockID = u.getId();

            for (WindowData w : u.getWindows()
                    ) {
                velocity.add(w.getVelocity());
                orientation.add(w.getOrientation());
            }

            Double[][] tuples = convertToTupleArray(orientation, velocity);
            CoreService.RNN.put(unlockID-1, tuples);
            //tupleDict.put(index-1, tuples);

            velocity = new ArrayList<>();
            orientation = new ArrayList<>();



        }
        /*
        try {
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

    private static Double[][] convertToTupleArray(ArrayList<Double> orientation, ArrayList<Double> velocity) {
        int length = orientation.size();

        Double[][] array = new Double[2][];
        array[0] = new Double[length];
        array[1] = new Double[length];


        for (int i = 0; i < length; i++) {
            array[0][i] = orientation.get(i);
            array[1][i] = velocity.get(i);

            //System.out.println("Orientation = " + array[0][i] + ", velocity = " + array[1][i]);
        }

        return array;
    }

    private static Map<Integer, Double[][]> getTupleDict() throws Exception {
        if (tupleDict.isEmpty()) {
            readOldData();
        }
        return tupleDict;
    }

    public static Double[][] readTestData() throws Exception {
        InputStream is = new FileInputStream(new File("C:\\Users\\TSN\\Documents\\Gits\\bachelor\\databases\\AutoUnlock-3-test-sucks.csv"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        reader.readLine(); // To get rid of attribute titles

        ArrayList<Double> velocity = new ArrayList<>();
        ArrayList<Double> orientation = new ArrayList<>();

        try {
            String line;
            String lastUnlockID = "1";
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
    }

}
