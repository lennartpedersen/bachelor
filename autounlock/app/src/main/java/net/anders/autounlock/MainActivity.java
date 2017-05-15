package net.anders.autounlock;

import android.Manifest;
import android.app.ActivityManager;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private CoreService coreService;
    private boolean bound = false;

    static TextView lockView;

    static Button addLock;
    static Button unlockDoor;
    static Button lockDoor;
    static Button export;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Constructing the database
        DataStore dataStore = new DataStore(this);

        // Buttons for UI
        addLock = (Button) findViewById(R.id.addlock);
        unlockDoor = (Button) findViewById(R.id.unlock);
        lockDoor = (Button) findViewById(R.id.lock);
        export = (Button) findViewById(R.id.exportDb);

        // TextViews for UI
        lockView = (TextView) findViewById(R.id.lockView);

        // Disables add lock and change text if lock is already stored
        if (!dataStore.getKnownLocks().isEmpty()) {
            Log.v("tjek", dataStore.getKnownLocks().toString());
            addLock.setVisibility(View.GONE);
            lockView.setText("SCANNING FOR LOCK!");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (isMyServiceRunning(CoreService.class)) {
            bindService(new Intent(this, CoreService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            ComponentName coreService = startService(new Intent(this, CoreService.class));
            bindService(new Intent(this, CoreService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        }

        // Check for location permission on startup if not granted.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else

            // Check for permission to write to external storage.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (bound) {
            unbindService(serviceConnection);
            bound = false;
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v("Permission", "Permission grannted, yay");
                    if (bound) {
                        coreService.googleConnect();
                    }
                } else {
                    Toast.makeText(this, "The app needs access to location in order to function.", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    /* Export database adapter button */
    public void onButtonClickExportDatastore(View v) {
        coreService.exportDB();
    }

    /* Add lock adapter button */
    public void onButtonClickAddLock(View v) {
        if (bound) {
            coreService.addLock();
            addLock.setVisibility(View.GONE);
            lockView.setText("SCANNING FOR LOCK!!");
        }
    }

    /* Unlock adapter button */
    public void onButtonClickUnlock(View v) {
        if (bound) {
            new UnlockTask().execute();
        }
    }

     /* Lock adapter button */
    public void onButtonClickLock(View v) {
        if (bound) {
            coreService.lock();
        }
    }

    /* Asynchronous task to unlock*/
    class UnlockTask extends AsyncTask<Void, String, Void>{
        @Override
        protected void onPreExecute() {
            if (CoreService.getUnlocks().size()+1 >= CoreService.reqUnlockTraining) {
                lockView.setText("Updating intelligence \n please be patient");
                lockView.setVisibility(View.VISIBLE);
                lockDoor.setVisibility(View.GONE);
                unlockDoor.setVisibility(View.GONE);
            }
            unlockDoor.setEnabled(false);
            lockDoor.setEnabled(false);
            export.setEnabled(false);

            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Toast.makeText(getApplicationContext(), "BeKey unlocked", Toast.LENGTH_SHORT).show();
            lockView.setVisibility(View.GONE);
            unlockDoor.setVisibility(View.VISIBLE);
            lockDoor.setVisibility(View.VISIBLE);

            unlockDoor.setEnabled(true);
            lockDoor.setEnabled(true);
            export.setEnabled(true);

            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            coreService.unlock();
            return null;
        }
    }

    /* Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CoreService.LocalBinder binder = (CoreService.LocalBinder) service;
            coreService = binder.getService();
            bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };
}
