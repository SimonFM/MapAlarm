package nintendont.mapalarm.services;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import nintendont.mapalarm.R;
import nintendont.mapalarm.receivers.LocationReceiver;
import nintendont.mapalarm.utils.Constants;

import static nintendont.mapalarm.utils.Constants.APP_PACKAGE_REFERENCE;
import static nintendont.mapalarm.utils.Constants.AlARM_SERVICE;
import static nintendont.mapalarm.utils.Constants.AlARM_SET;
import static nintendont.mapalarm.utils.Constants.LATITUDE;
import static nintendont.mapalarm.utils.Constants.LATITUDE_KEY;
import static nintendont.mapalarm.utils.Constants.LONGITUDE;
import static nintendont.mapalarm.utils.Constants.LONGITUDE_KEY;

public class LocationService extends Service {
    private static final String TAG = "MapAlarm";
    public static final int KILOMETRE = 1000;
    private LocationManager mLocationManager = null;
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;
    private Location mDestination, mLastLocation;
    private NotificationManager mNotificationManager;
    private SharedPreferences settings;
    LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
       // toast("Creating Service");
        initializeLocationManager();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        settings = this.getSharedPreferences(APP_PACKAGE_REFERENCE, Context.MODE_PRIVATE);
        for(LocationListener listener : mLocationListeners){
            try {
                mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, listener);
            } catch (java.lang.SecurityException ex) {
                Log.i(TAG, "fail to request location update, ignore", ex);
            } catch (IllegalArgumentException ex) {
                Log.d(TAG, "network provider does not exist, " + ex.getMessage());
            }
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        String action = intent.getAction();
        if(action == null){
            Log.e (TAG, action + " was null, flags=" + flags + " bits=" + Integer.toBinaryString (flags));
        } else if (action.equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Bundle extras = intent.getExtras();
            double lat = extras.getDouble(LATITUDE, 0);
            double lon = extras.getDouble(LONGITUDE, 0);
            mDestination = createNewLocation(lat, lon);
            //toast("Starting Service");
            float[] results = new float[1];
            Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), mDestination.getLatitude(), mDestination.getLongitude(), results);

            Notification notification = getNotification(results[0]);
            if(notification.sound == null){
                startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);
            } else {
                deleteSharedPreferences();
                stopForeground(true);
                removeNotification();
                cancelAlarm();
                mNotificationManager.notify(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification);
            }
        } else if(action.equals(Constants.ACTION.STOPFOREGROUND_ACTION)){
            stopForeground(true);
        }
        return START_STICKY;
    }

    private void deleteSharedPreferences() {
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.remove(LONGITUDE_KEY);
        settingsEditor.remove(LATITUDE_KEY);
        settingsEditor.putBoolean(AlARM_SET, false);
       // settingsEditor.putBoolean(AlARM_SERVICE, false);
        settingsEditor.apply();
    }


    private Notification getNotification(float distance) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle("Map Alarm");
        builder.setTicker("Map Alarm");
        builder.setSmallIcon(R.drawable.ic_audiotrack);

        if(distance <= KILOMETRE){
            Intent stopSelf = new Intent(this, LocationService.class);
            stopSelf.setAction(Constants.ACTION.STOPFOREGROUND_ACTION);
            PendingIntent pStopSelf = PendingIntent.getService(this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT);
            builder.addAction(R.drawable.cast_ic_notification_stop_live_stream, "Stop", pStopSelf);

            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(uri);
            builder.setContentText("You've Arrived!");
            int rgbaColour = Color.argb(255, 183, 3, 1);
            builder.setLights(rgbaColour, 2000, 1000);
        } else {
            //saveServiceAlarm();
            builder.setContentText("Distance : "+ distance + "m");
            builder.setOngoing(true);
        }
        Notification notification = builder.build();
        return notification;
    }

    private void saveServiceAlarm() {
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putBoolean(AlARM_SERVICE, true);
        settingsEditor.apply();
    }

    private Location createNewLocation(double latitude, double longitude) {
        Location location = new Location("dummyprovider");
        location.setLongitude(longitude);
        location.setLatitude(latitude);
        return location;
    }

    private void cancelAlarm() {
        // Cancel Existing alarm
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        LatLng latLng = new LatLng(mDestination.getLatitude(), mDestination.getLongitude());
        Intent intent = getReceiverIntent(latLng);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
        // remove from sharedPreferences
        //toast("Alarm Off");
    }

    private void removeNotification() {
        mNotificationManager.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);
    }

    @NonNull
    private Intent getReceiverIntent(LatLng desiredPosition) {
        Intent receiverIntent = new Intent();
        receiverIntent.setClass(this, LocationReceiver.class);
        makeIntent(receiverIntent, desiredPosition);
        return receiverIntent;
    }

    private Intent makeIntent(Intent intent, LatLng desiredPosition){
        Bundle extras = new Bundle();
        double lat = desiredPosition.latitude;
        double lon = desiredPosition.longitude;
        extras.putDouble(LATITUDE, lat);
        extras.putDouble(LONGITUDE, lon);
        intent.putExtras(extras);
        return intent;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (mLocationManager != null) {
            for(LocationListener listener : mLocationListeners){
                try {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mLocationManager.removeUpdates(listener);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }

    private void toast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private class LocationListener implements android.location.LocationListener {

        public LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }
}
