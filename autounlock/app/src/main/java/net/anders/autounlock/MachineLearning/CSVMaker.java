package net.anders.autounlock.MachineLearning;

/**
 * Created by Lennart Pedersen on 16-06-2017.
 */

import android.content.Context;
import android.os.Environment;

import net.anders.autounlock.CoreService;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by TSN on 11-06-2017.
 */
class CSVMaker {
    private static int K_UNLOCK         = 1;
    private static int K_ACCELERATION_X = 2;
    private static int K_ACCELERATION_Y = 3;
    private static int K_ORIENTATION    = 4;

    private static ArrayList<Double> acc_x = new ArrayList<>();
    private static ArrayList<Double> acc_y = new ArrayList<>();
    private static ArrayList<Double> orientation = new ArrayList<>();
    public static String outputPath = Environment.getExternalStorageDirectory() + "/AutoUnlock/Csv"; // Ændres til temp folder på device

    public static void convertToCSV(Map<Integer, Double[][]> trainData) throws Exception {
        String outputPath_F = outputPath + "/train/features";
        String outputPath_L = outputPath + "/train/labels";

        for (int i = 0; i < trainData.keySet().size(); i++) {
            Double[][] trainList = trainData.get(i);
            convertSingleArrayToCSV(trainList, i+1);
        }

    }

    public static void convertSingleArrayToCSV(Double[][] sequence, int unlockID) throws Exception {
        String outputPath_F = outputPath + "/train/features";
        String outputPath_L = outputPath + "/train/labels";

        for (int j = 0; j < sequence[0].length; j++) {
            orientation.add(sequence[0][j]);
            acc_x.add(sequence[1][j]);
            acc_y.add(sequence[2][j]);
        }
        // Write the feature file
        writeFile(outputPath_F+"/"+(getNumber(outputPath_F)+1)+".csv");
        // Write the label file
        writeFileLabel(outputPath_L+"/"+(getNumber(outputPath_L)+1)+".csv", ""+DatabaseRetriever.getUnlockValue(unlockID));

        acc_x = new ArrayList<>();
        acc_y = new ArrayList<>();
        orientation = new ArrayList<>();
    }

    public static void convertToTempCSVProbArray(Double[][] sequence, String kClass) throws Exception {
        String outputPath_F = outputPath + "/train/features";
        String outputPath_L = outputPath + "/train/labels";

        for (int j = 0; j < sequence[0].length; j++) {
            orientation.add(sequence[0][j]);
            acc_x.add(sequence[1][j]);
            acc_y.add(sequence[2][j]);
        }
        // Write the feature file
        writeFile(outputPath_F+"/"+(getNumber(outputPath_F)+1)+".csv");
        // Write the label file
        writeFileLabel(outputPath_L+"/"+(getNumber(outputPath_L)+1)+".csv", kClass);

        acc_x = new ArrayList<>();
        acc_y = new ArrayList<>();
        orientation = new ArrayList<>();
    }

/*
    private static void convertToCSV(String path, String nline, boolean skip, String type, String kClass) throws Exception {
        String outputPath_F = "/Users/tom-fire/Desktop/uitest/"+type+"/features";
        String outputPath_L = "/Users/tom-fire/Desktop/uitest/"+type+"/labels";

        InputStream is = new FileInputStream(new File(path));
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        if (skip) {
            reader.readLine(); // To get rid of attribute titles
        }

        try {
            String line;
            String lastUnlockID = nline;
            while((line=reader.readLine()) != null) {
                String[] RowData = line.split(",");
                String newUnlockID = RowData[K_UNLOCK];

                if (lastUnlockID.equals(newUnlockID)) {
                    acc_x.add(Double.parseDouble(RowData[K_ACCELERATION_X]));
                    acc_y.add(Double.parseDouble(RowData[K_ACCELERATION_Y]));
                    orientation.add(Double.parseDouble(RowData[K_ORIENTATION]));
                } else {
                    writeFile(outputPath_F+"/"+(getNumber(outputPath_F)+1)+".csv");
                    writeFileLabel(outputPath_L+"/"+(getNumber(outputPath_L)+1)+".csv", kClass);

                    lastUnlockID = newUnlockID;
                    acc_x = new ArrayList<>();
                    acc_y = new ArrayList<>();
                    orientation = new ArrayList<>();
                }
            }

            if (orientation.size() > 1) {
                writeFile(outputPath_F + "/" + (getNumber(outputPath_F) + 1) + ".csv");
                writeFileLabel(outputPath_L + "/" + (getNumber(outputPath_L) + 1) + ".csv", kClass);
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
    }
*/
    private static void writeFile(String outputPath) throws Exception {
        FileWriter writer = new FileWriter(outputPath);

        for (int i = 0; i < orientation.size(); i++) {
            List<String> numbers = new ArrayList<>();
            numbers.add(acc_x.get(i) + "," + acc_y.get(i) + "," + orientation.get(i));
            CSVUtils.writeLine(writer, numbers);
        }

        writer.flush();
        writer.close();
    }

    private static void writeFileLabel(String outputPath, String kClass) throws Exception {
        FileWriter writer = new FileWriter(outputPath);

        List<String> numbers = new ArrayList<>();
        numbers.add(kClass);
        CSVUtils.writeLine(writer, numbers);

        writer.flush();
        writer.close();
    }

    private static int getNumber(String path) {
        File folder = new File(path);
        return folder.listFiles().length;

    }
}

class CSVUtils {

    private static final char DEFAULT_SEPARATOR = ',';

    public static void writeLine(Writer w, List<String> values) throws IOException {
        writeLine(w, values, DEFAULT_SEPARATOR, ' ');
    }

    public static void writeLine(Writer w, List<String> values, char separators) throws IOException {
        writeLine(w, values, separators, ' ');
    }

    //https://tools.ietf.org/html/rfc4180
    private static String followCVSformat(String value) {

        String result = value;
        if (result.contains("\"")) {
            result = result.replace("\"", "\"\"");
        }
        return result;

    }

    public static void writeLine(Writer w, List<String> values, char separators, char customQuote) throws IOException {

        boolean first = true;

        //default customQuote is empty

        if (separators == ' ') {
            separators = DEFAULT_SEPARATOR;
        }

        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (!first) {
                sb.append(separators);
            }
            if (customQuote == ' ') {
                sb.append(followCVSformat(value));
            } else {
                sb.append(customQuote).append(followCVSformat(value)).append(customQuote);
            }

            first = false;
        }
        sb.append("\n");
        w.append(sb.toString());


    }

}
