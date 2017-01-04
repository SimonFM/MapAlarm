package nintendont.mapalarm.activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import nintendont.mapalarm.R;
import nintendont.mapalarm.receivers.LocationReceiver;
import nintendont.mapalarm.services.LocationService;
import nintendont.mapalarm.utils.Constants;

import static nintendont.mapalarm.utils.Constants.APP_PACKAGE_REFERENCE;
import static nintendont.mapalarm.utils.Constants.AlARM_SET;
import static nintendont.mapalarm.utils.Constants.LATITUDE;
import static nintendont.mapalarm.utils.Constants.LATITUDE_KEY;
import static nintendont.mapalarm.utils.Constants.LONGITUDE;
import static nintendont.mapalarm.utils.Constants.LONGITUDE_KEY;
import static nintendont.mapalarm.utils.Constants.YOUR_DESTINATION;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        GoogleApiClient.ConnectionCallbacks ,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 2711;
    public static final int DEFAULT_ZOOM = 14;
    public static final int ONE_SECOND = 1000;
    public static final int HALF_MINUTE = 30 * ONE_SECOND;
    public static final int FIVE_SECONDS = 5 * ONE_SECOND;
    public static final int ALARM_THRESHOLD = 50;
    public static final int ALARMBAR_ON = 75;
    public static final int ALARMBAR_OFF = 0;

    private boolean alarmSet;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private LatLng desiredPosition;
    private Marker userSelection;
    private AlarmManager alarmManager;
    private SeekBar seekBar;
    private NotificationManager nMgr;
    private SharedPreferences settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        checkLocationPermission();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        nMgr = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        settings = this.getSharedPreferences(APP_PACKAGE_REFERENCE, Context.MODE_PRIVATE);
        setupAlarmSwitch();
    }

    private void setupAlarmSwitch() {
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                //boolean alarmServiceSet = settings.getBoolean(ALARM_SERVICE, false);
                if (progress > ALARM_THRESHOLD && desiredPosition != null && !alarmSet) {
                    cancelAlarm(); // stop alarm receiver
                    Intent serviceIntent = makeServiceIntent(desiredPosition);
                    stopService(serviceIntent);// stop current service
                    enableAlarm(desiredPosition); // start alarm receiver
                    toast("Alarm On!");
                    seekBar.setThumb(getResources().getDrawable(R.drawable.ic_audiotrack_light));
                } else if(progress <= ALARM_THRESHOLD && desiredPosition != null && alarmSet){
                    cancelAlarm(); // stop alarm receiver
                    Intent serviceIntent = makeServiceIntent(desiredPosition);
                    stopService(serviceIntent);// stop current service
                    removeUserMarker();
                    deleteSharedPreferences();
                    toast("Alarm Off!");
                    seekBar.setThumb(getResources().getDrawable(R.drawable.ic_audiotrack));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

        });
    }

    private void removeUserMarker() {
        if(userSelection != null){
            userSelection.remove();
        }
    }

    private void deleteSharedPreferences() {
        Editor settingsEditor = settings.edit();
        settingsEditor.remove(LONGITUDE_KEY);
        settingsEditor.remove(LATITUDE_KEY);
        settingsEditor.putBoolean(AlARM_SET, false);
        settingsEditor.apply();
    }

    @Override
    protected void onPause(){
        super.onPause();
        //boolean alarmServiceSet = settings.getBoolean(ALARM_SERVICE, false);
        // if there's an alarm set. Save position, otherwise remove user preferences
        if(alarmSet){// && !alarmServiceSet) {
            saveUserSelection();
        } else {
            removeUserMarker();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        recoverLastUserLocation();
//        boolean alarmServiceSet = settings.getBoolean(ALARM_SERVICE, false);
        if(alarmSet){// && alarmServiceSet){
            seekBar.setThumb(getResources().getDrawable(R.drawable.ic_audiotrack_light));
            seekBar.setProgress(ALARMBAR_ON);
        } else {
            seekBar.setThumb(getResources().getDrawable(R.drawable.ic_audiotrack));
            seekBar.setProgress(ALARMBAR_OFF);
        }
    }

    // save last location to memory
    private void saveUserSelection() {
        Editor settingsEditor = settings.edit();
        //boolean alarmServiceSet = settings.getBoolean(ALARM_SERVICE, false);
        if (alarmSet){// && alarmServiceSet){
            settingsEditor.putBoolean(AlARM_SET, true);
            String lat = Double.toString(desiredPosition.latitude);
            String lon = Double.toString(desiredPosition.longitude);
            settingsEditor.putString(LONGITUDE_KEY, lon);
            settingsEditor.putString(LATITUDE_KEY, lat);
        }
        settingsEditor.apply();
    }

    private void recoverLastUserLocation() {
        String savedLat = settings.getString(LATITUDE_KEY, "");
        String savedLon = settings.getString(LONGITUDE_KEY, "");

        if(!savedLat.isEmpty() && !savedLon.isEmpty()){
            double lat = Double.parseDouble(savedLat);
            double lon = Double.parseDouble(savedLon);
            desiredPosition = new LatLng(lat, lon);
            alarmSet = settings.getBoolean(AlARM_SET, false);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);
        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

        //boolean alarmServiceSet = settings.getBoolean(ALARM_SERVICE, false);
        if(alarmSet){// && alarmServiceSet){
            addUserDestinationToMap(desiredPosition);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //toast("Location Changed");
        zoomTo(location);
    }

    private void zoomTo(Location myLocation){
        if(myLocation != null){
            mLastLocation = myLocation;
            LatLng myLocationCoords = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(myLocationCoords));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
            //stop location updates
            if (mGoogleApiClient != null) {
                LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onMapLongClick(LatLng desiredPosition) {
        addUserDestinationToMap(desiredPosition);
        this.desiredPosition = desiredPosition;
    }

    private void addUserDestinationToMap(LatLng desiredPosition) {
        if(userSelection != null){
            userSelection.remove();
        }
        userSelection = mMap.addMarker(new MarkerOptions().position(desiredPosition).title(YOUR_DESTINATION));
    }

    private void enableAlarm(LatLng desiredPosition) {
        Intent resultIntent = getReceiverIntent(desiredPosition); //start the update alarm manager
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, 0,  HALF_MINUTE, pendingIntent);
        alarmSet = true;
        //toast("Alarm On");
    }

    private void cancelAlarm() {
        // Cancel Existing alarm
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = getReceiverIntent(desiredPosition);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.cancel(pendingIntent);
        removeNotification();
        alarmSet = false;
        // remove from sharedPreferences
        //toast("Alarm Off");
    }

    private void removeNotification() {
        nMgr.cancel(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE);
    }

    @NonNull
    private Intent getReceiverIntent(LatLng desiredPosition) {
        Intent receiverIntent = new Intent();
        receiverIntent.setClass(this, LocationReceiver.class);
        makeIntent(receiverIntent, desiredPosition);
        return receiverIntent;
    }

    @NonNull
    private Intent makeServiceIntent(LatLng desiredPosition) {
        Intent serviceIntent = new Intent(MapsActivity.this, LocationService.class);
        serviceIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        makeIntent(serviceIntent, desiredPosition);
        return serviceIntent;
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

    public boolean checkLocationPermission(){
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Asking user if explanation is needed
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
                }
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(HALF_MINUTE);
        mLocationRequest.setFastestInterval(FIVE_SECONDS);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        locationChecker(MapsActivity.this);
    }

    /**
     * Prompt user to enable GPS and Location Services
     * @param activity
     */
    public void locationChecker(final Activity activity) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        builder.setAlwaysShow(true);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(activity, ONE_SECOND);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission was granted.
                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    toast("permission denied");
                }
                return;
            }
        }
    }

    private void toast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
