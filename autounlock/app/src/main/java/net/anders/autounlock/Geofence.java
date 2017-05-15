package net.anders.autounlock;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

class Geofence {

    private static String TAG = "Geofence";

    private ArrayList<com.google.android.gms.location.Geofence> geofenceArrayList;

    private PendingIntent pendingIntent;

    Geofence() {
        geofenceArrayList = new ArrayList<com.google.android.gms.location.Geofence>();
    }

    private void populateGeofenceList(String name, LatLng location, Float radius) {

        Log.v("Populate geofence list", "Name: " + name + " Location: " + location);

        // Calculate hours to milliseconds
        int expireInHours = 720;
        long expireInMilliseconds = expireInHours * 60 * 60 * 1000;

        geofenceArrayList.add(new com.google.android.gms.location.Geofence.Builder()
                .setRequestId(name)
                .setCircularRegion(location.latitude, location.longitude, radius)
                .setExpirationDuration(expireInMilliseconds)
                .setTransitionTypes(com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER |
                        com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
    }

    private GeofencingRequest geofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceArrayList);

        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent(Context context) {
        if (pendingIntent != null) {
            return pendingIntent;
        }
        Intent intent = new Intent(context, GeofenceService.class);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofenceArrayList", securityException);
    }

    void addGeofence(LockData lockData) {
        populateGeofenceList(
                "inner" + lockData.getMAC(),
                new LatLng(lockData.getLocation().getLatitude(), lockData.getLocation().getLongitude()),
                lockData.getInnerGeofence()
        );
        populateGeofenceList(
                "outer" + lockData.getMAC(),
                new LatLng(lockData.getLocation().getLatitude(), lockData.getLocation().getLongitude()),
                lockData.getOuterGeofence()
        );
    }

    void registerGeofences(Context context, GoogleApiClient googleApiClient) {
        Log.v("geofenceArrayList", geofenceArrayList.toString());

        if (!googleApiClient.isConnected()) {
            Log.v(TAG, "GoogleApiClient not connected");
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    geofencingRequest(),
                    getGeofencePendingIntent(context)
            ).setResultCallback((ResultCallback<? super Status>) context);
        } catch (SecurityException securityException) {
            logSecurityException(securityException);
        }
    }

    void unregisterGeofences(Context context, GoogleApiClient googleApiClient) {
        try {
            LocationServices.GeofencingApi.removeGeofences(
                    googleApiClient,
                    getGeofencePendingIntent(context)
            ).setResultCallback((ResultCallback<? super Status>) context);
        } catch (SecurityException securityException) {
            logSecurityException(securityException);
        }
    }
}
