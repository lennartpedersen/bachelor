package net.anders.autounlock.Export;

import android.os.Environment;
import android.util.Log;

import net.anders.autounlock.MachineLearning.WindowData;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.List;

/**
 * Created by Anders on 15-02-2017.
 */

public class Export {

    static FileWriter writer;

    public static void Database() {
        try {
            File data = Environment.getDataDirectory();

            try {
                String datastorePath = "data/net.anders.autounlock/databases/datastore.db";

                String exportPath = constructDbName();

                File outputDirectory = new File(Environment.getExternalStorageDirectory() + "/AutoUnlock/");
                boolean dirs = outputDirectory.mkdirs();
                Log.v("database", outputDirectory.toString());

                File datastore = new File(data, datastorePath);
                File export = new File(outputDirectory, exportPath);

                FileChannel source = new FileInputStream(datastore).getChannel();
                FileChannel destination = new FileOutputStream(export).getChannel();

                destination.transferFrom(source, 0, source.size());
                source.close();
                destination.close();

                Log.v("Export Datastore", "Datastore exported to " + exportPath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private static String constructDbName() {
        File file = new File(Environment.getExternalStorageDirectory() + "/AutoUnlock/");
        File[] list = file.listFiles();
        return "AutoUnlock-" + list.length + ".db";
    }

//    public static void CsvRawAcc(List<SensorData> calibrationAccelerometer, String activity) throws IOException {
//
//        File root = Environment.getExternalStorageDirectory();
//        File gpxfile = new File(root, activity + ".csv");
//
//        try {
//            writer = new FileWriter(gpxfile);
//            writeCsvHeader(activity.toString(), "x","y","z", "time", "ori");
//
//            for (SensorData acc: calibrationAccelerometer) {
//                writeCsvData(activity.toString(), acc.getAccelerationX(), acc.getAccelerationY(), acc.getAccelerationZ(), acc.getTime(), acc.getOrientation());
//            }
//
//            writer.flush();
//            writer.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        String exportPath = constructCalibrationName(activity);
//
//        File outputDirectory = new File("/sdcard/AutoUnlock/");
//        outputDirectory.mkdirs();
//
//        File export = new File(outputDirectory, exportPath);
//
//        FileChannel source = new FileInputStream(gpxfile).getChannel();
//        FileChannel destination = new FileOutputStream(export).getChannel();
//
//        destination.transferFrom(source, 0, source.size());
//        source.close();
//        destination.close();
//    }

//    public static void CsvRawAcc(List<SensorData> calibrationAccelerometer, String activity) throws IOException {
//
//        File root = Environment.getExternalStorageDirectory();
//        File gpxfile = new File(root, activity + ".csv");
//
//        try {
//            writer = new FileWriter(gpxfile);
//            writeCsvHeader("time", "acc_x","acc_y", "speed_x", "speed_y", "ori");
//
//            for (SensorData acc: calibrationAccelerometer) {
//                writeCsvData(acc.getTime(), acc.getAccelerationX(), acc.getAccelerationY(), acc.getSpeedX(), acc.getSpeedY(), acc.getOrientation());
//            }
//
//            writer.flush();
//            writer.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

//        String exportPath = constructCalibrationName(activity);
//
//        File outputDirectory = new File("/sdcard/AutoUnlock/");
//        outputDirectory.mkdirs();
//
//        File export = new File(outputDirectory, exportPath);
//
//        FileChannel source = new FileInputStream(gpxfile).getChannel();
//        FileChannel destination = new FileOutputStream(export).getChannel();
//
//        destination.transferFrom(source, 0, source.size());
//        source.close();
//        destination.close();
//    }

    private static void writeCsvHeader(String h1, String h2, String h3, String h4, String h5, String h6) throws IOException {
        String line = String.format("%s;%s;%s;%s;%s;%s\n", h1,h2,h3,h4,h5,h6);
        writer.write(line);
    }

//    private static void writeCsvData(String d, float e, float f, float g, float h, float i) throws IOException {
//        String line = String.format("%s;%f;%f;%f;%f;%f\n", d, e, f, g, h, i);
//        writer.write(line);
//    }

    private static void writeCsvData(float d, float e, float f, float g, float h, float i) throws IOException {
        String line = String.format("%f;%f;%f;%f;%f;%f\n", d, e, f, g, h, i);
        writer.write(line);
    }

    private static String constructCalibrationName(String activity) {
        File file=new File("/"+ Environment.getExternalStorageDirectory().getPath() + "/Android/");
        File[] list = file.listFiles();
        return "AutoUnlock-" + activity + "-" + list.length + ".csv";
    }



    public static void CsvMean(List<Float> mean) throws IOException {

        String activity = "mean";
        String type = "float";

        File root = Environment.getExternalStorageDirectory();
        File gpxfile = new File(root, activity + ".csv");

        try {
            writer = new FileWriter(gpxfile);
            writeFeatureCsvHeader("type", "avg");

            for (Float f: mean) {
                writeMeanCsvData(type.toString(), f);
            }

            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        String exportPath = constructCalibrationName(activity);

        File outputDirectory = new File("/"+ Environment.getExternalStorageDirectory().getPath() + "/Android/");
        outputDirectory.mkdirs();

        File export = new File(outputDirectory, exportPath);

        FileChannel source = new FileInputStream(gpxfile).getChannel();
        FileChannel destination = new FileOutputStream(export).getChannel();

        destination.transferFrom(source, 0, source.size());
        source.close();
        destination.close();
    }

    private static void writeMeanCsvData(String d, float e) throws IOException {
        String line = String.format("%s;%f\n", d, e);
        writer.write(line);
    }

    private static void writeFeatureCsvHeader(String h1, String h2) throws IOException {
        String line = String.format("%s;%s\n", h1,h2);
        writer.write(line);
    }


    //55555555555

    public static void CsvRms(List<Double> rms) throws IOException {

        String activity = "rms";
        String type = "double";

        File root = Environment.getExternalStorageDirectory();
        File gpxfile = new File(root, activity + ".csv");

        try {
            writer = new FileWriter(gpxfile);
            writeFeatureCsvHeader("type", "avg");

            for (Double d: rms) {
                writeDoubleCsvData(type.toString(), d);
            }

            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        String exportPath = constructCalibrationName(activity);

        File outputDirectory = new File("/"+ Environment.getExternalStorageDirectory().getPath() + "/Android/");
        outputDirectory.mkdirs();

        File export = new File(outputDirectory, exportPath);

        FileChannel source = new FileInputStream(gpxfile).getChannel();
        FileChannel destination = new FileOutputStream(export).getChannel();

        destination.transferFrom(source, 0, source.size());
        source.close();
        destination.close();
    }

    private static void writeDoubleCsvData(String d, double e) throws IOException {
        String line = String.format("%s;%f\n", d, e);
        writer.write(line);
    }

    public static void CsvStd(List<Double> std) throws IOException {

        String activity = "std";
        String type = "double";

        File root = Environment.getExternalStorageDirectory();
        File gpxfile = new File(root, activity + ".csv");

        try {
            writer = new FileWriter(gpxfile);
            writeFeatureCsvHeader("type", "avg");

            for (Double d: std) {
                writeDoubleCsvData(type.toString(), d);
            }

            writer.flush();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        String exportPath = constructCalibrationName(activity);

        File outputDirectory = new File("/"+ Environment.getExternalStorageDirectory().getPath() + "/Android/");
        outputDirectory.mkdirs();

        File export = new File(outputDirectory, exportPath);

        FileChannel source = new FileInputStream(gpxfile).getChannel();
        FileChannel destination = new FileOutputStream(export).getChannel();

        destination.transferFrom(source, 0, source.size());
        source.close();
        destination.close();
    }


    public static void Windows(WindowData[] windows) {
        try {
            String activity = "acc";
            File root = Environment.getExternalStorageDirectory();
            File gpxfile = new File(root, activity + ".csv");

            try {
                writer = new FileWriter(gpxfile);
                writeCsvWindowHeader("degree", "velocity");

                for (WindowData window: windows) {
                    writeCsvWindowData(window.getOrientation(),window.getAccelerationMag());
                }

                writer.flush();
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            String exportPath = constructCalibrationName(activity);

            File outputDirectory = new File("/"+ Environment.getExternalStorageDirectory().getPath() + "/Android/");
            outputDirectory.mkdirs();

            File export = new File(outputDirectory, exportPath);

            FileChannel source = new FileInputStream(gpxfile).getChannel();
            FileChannel destination = new FileOutputStream(export).getChannel();

            destination.transferFrom(source, 0, source.size());
            source.close();
            destination.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeCsvWindowHeader(String h1, String h2) throws IOException {
        String line = String.format("%s;%s\n", h1,h2);
        writer.write(line);
    }

    public static void writeCsvWindowData(double d, double e) throws IOException {
        String line = String.format("%f;%f\n", d, e);
        writer.write(line);
    }

//    public static void CsvCoord(List<CoordinateData> coordinateData) throws IOException {
//
//        String activity = "coord";
//        File root = Environment.getExternalStorageDirectory();
//        File gpxfile = new File(root, activity + ".csv");
//
//        try {
//            writer = new FileWriter(gpxfile);
//            writeCsvCoordHeader("x", "y");
//
//            for (CoordinateData coord: coordinateData) {
//                writeCsvCoordData(coord.getX(), coord.getY());
//            }
//
//            writer.flush();
//            writer.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        String exportPath = constructCalibrationName(activity);
//
//        File outputDirectory = new File("/sdcard/AutoUnlock/");
//        outputDirectory.mkdirs();
//
//        File export = new File(outputDirectory, exportPath);
//
//        FileChannel source = new FileInputStream(gpxfile).getChannel();
//        FileChannel destination = new FileOutputStream(export).getChannel();
//
//        destination.transferFrom(source, 0, source.size());
//        source.close();
//        destination.close();
//    }

    private static void writeCsvCoordHeader(String h1, String h2) throws IOException {
        String line = String.format("%s;%s\n", h1,h2);
        writer.write(line);
    }

    private static void writeCsvCoordData(double d, double e) throws IOException {
        String line = String.format("%f;%f\n", d, e);
        writer.write(line);
    }
}
