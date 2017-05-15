package net.anders.autounlock.MachineLearning;

import java.util.ArrayList;

/**
 * Created by Anders on 14-03-2017.
 */

public class UnlockData {
    int id;
    int cluster_id;
    ArrayList<WindowData> windows;

    public UnlockData(int id, int cluster_id, ArrayList<WindowData> windows) {
        this.id = id;
        this.cluster_id = cluster_id;
        this.windows = windows;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public int getClusterId() {
        return cluster_id;
    }
    public void setClusterId(int cluster_id) {
        this.cluster_id = cluster_id;
    }

    public ArrayList<WindowData> getWindows() { return windows; }
    public void setWindowss(ArrayList<WindowData> windows) { this.windows = windows; }
}

