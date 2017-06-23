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


public class Export {

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

}
