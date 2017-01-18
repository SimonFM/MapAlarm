package nintendont.mapalarm.activity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import nintendont.mapalarm.R;
import nintendont.mapalarm.tutorial.Tutorial;
import nintendont.mapalarm.receivers.LocationReceiver;
import nintendont.mapalarm.services.LocationService;
import nintendont.mapalarm.utils.Constants;

import static nintendont.mapalarm.utils.Constants.ALARMBAR_OFF;
import static nintendont.mapalarm.utils.Constants.ALARMBAR_ON;
import static nintendont.mapalarm.utils.Constants.ALARM_THRESHOLD;
import static nintendont.mapalarm.utils.Constants.DEFAULT_ZOOM;
import static nintendont.mapalarm.utils.Constants.DESTINATION_MARKER_COLOUR;
import static nintendont.mapalarm.utils.Constants.FIVE_SECONDS;
import static nintendont.mapalarm.utils.Constants.HALF_MINUTE;
import static nintendont.mapalarm.utils.Constants.KILOMETRE;
import static nintendont.mapalarm.utils.Constants.LATITUDE;
import static nintendont.mapalarm.utils.Constants.LONGITUDE;
import static nintendont.mapalarm.utils.Constants.MY_PERMISSIONS_REQUEST_LOCATION;
import static nintendont.mapalarm.utils.Constants.ONE_SECOND;
import static nintendont.mapalarm.utils.Messages.NO_DESTINATION_ERROR;
import static nintendont.mapalarm.utils.Messages.TOO_CLOSE_ERROR;
import static nintendont.mapalarm.utils.Messages.YOUR_DESTINATION;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener,
        GoogleApiClient.ConnectionCallbacks ,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private boolean alarmSet;

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private LatLng desiredPosition;
    private Marker userSelection;
    private AlarmManager alarmManager;
    private SeekBar seekBar;
    private NotificationManager nMgr;
    private AppSettings appSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        nMgr = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        appSettings = new AppSettings(this);
        setupAlarmSwitch();
    }

    private void setupOptionsButton() {
        final Dialog optionsDialogs = makeOptionsDialog();
        Button optionsButton = (Button) findViewById(R.id.optionsButton);
        optionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                optionsDialogs.show();
            }
        });
    }

    @NonNull
    private Dialog makeOptionsDialog() {
        final Dialog optionsDialogs = new Dialog(this);

        // retrieve display dimensions
        Rect displayRectangle = new Rect();
        Window window = this.getWindow();
        window.getDecorView().getWindowVisibleDisplayFrame(displayRectangle);

        // inflate and adjust layout
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.radius_settings, null);
        layout.setMinimumWidth((displayRectangle.width() )); //* 0.2f
        layout.setMinimumHeight((int)(displayRectangle.height() * 0.2f));
        optionsDialogs.setContentView(layout);
        final TextView textView = (TextView) optionsDialogs.findViewById(R.id.level_txt);
        int distance = appSettings.recoverUserDistance();
        textView.setText("Distance: " + distance + " m");

        final SeekBar distanceSeekBar = (SeekBar) optionsDialogs.findViewById(R.id.level_seek);
        final Button button = (Button) optionsDialogs.findViewById(R.id.level_ok);
        distanceSeekBar.setMax(KILOMETRE * 10);
        distanceSeekBar.setProgress(distance);
        distanceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //add code here
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //add code here
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                String newText = "Distance: " + progress + " m";
                textView.setText(newText);
            }
        });

        button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //respond to level
                int chosenLevel = distanceSeekBar.getProgress();
                appSettings.saveUserDistance(chosenLevel);
                optionsDialogs.dismiss();
            }
        });

        return optionsDialogs;
    }

    @Override
    protected void onPause(){
        super.onPause();
        //boolean alarmServiceSet = settings.getBoolean(ALARM_SERVICE, false);
        // if there's an alarm set. Save position, otherwise remove user preferences
        if(alarmSet){// && !alarmServiceSet) {
            appSettings.saveUserSelection(desiredPosition, alarmSet);
        } else {
            removeUserMarker();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        checkLocationPermission();
        setupOptionsButton();
        desiredPosition = appSettings.recoverLastUserLocation();
        if(desiredPosition != null){
            alarmSet = appSettings.recoverAlarmState();
        }

        if(alarmSet){
            seekBar.setThumb(ContextCompat.getDrawable(getApplicationContext(), R.drawable.thumb_image_on));
            seekBar.setProgress(ALARMBAR_ON);
        } else {
            seekBar.setThumb(ContextCompat.getDrawable(getApplicationContext(), R.drawable.thumb_image_off));
            seekBar.setProgress(ALARMBAR_OFF);
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

    @Override
    public void onMapLongClick(LatLng desiredPosition) {
        addUserDestinationToMap(desiredPosition);
        this.desiredPosition = desiredPosition;
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
        runTutorials();
    }

    private void runTutorials(){
        TextView seekbarTarget = makeSeekbarTarget();
        TextView mapTarget = makeMapTarget();

        Tutorial tutorial = new Tutorial(MapsActivity.this);
        tutorial.makeMapTutorial(mapTarget);
        tutorial.makeAlarmTutorial(seekbarTarget);
        tutorial.show();
    }

    @NonNull
    private TextView makeMapTarget() {
        TextView mapView = new TextView(this);
        int w = mapFragment.getView().getWidth() / 2;
        int h = mapFragment.getView().getHeight() / 2;

        mapView.setWidth(w);
        mapView.setHeight(h);
        mapView.setX(w / 2);
        mapView.setY(h / 2);
        RelativeLayout rl = (RelativeLayout) this.findViewById(R.id.map_relative_layout);
        rl.addView(mapView);
        return mapView;
    }

    @NonNull
    private TextView makeSeekbarTarget() {
        TextView view = new TextView(this);

        view.setWidth(seekBar.getWidth() * 3);
        view.setHeight(seekBar.getHeight() / 2);
        view.setX(seekBar.getX() - (seekBar.getWidth() / 2));
        view.setY(seekBar.getY());
        RelativeLayout rl = (RelativeLayout) this.findViewById(R.id.seekbar_layout);
        rl.addView(view);
        return view;
    }

    private void setupAlarmSwitch() {
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                //boolean alarmServiceSet = settings.getBoolean(ALARM_SERVICE, false);
                if (alarmOn(progress)) {
                    cancelAlarm(); // stop alarm receiver
                    Intent serviceIntent = makeServiceIntent(desiredPosition);
                    stopService(serviceIntent);// stop current service
                    enableAlarm(desiredPosition); // start alarm receiver
                    toast("Alarm On!");
                    seekBar.setThumb(ContextCompat.getDrawable(getApplicationContext(), R.drawable.thumb_image_on));
                } else if(alarmOff(progress)){
                    cancelAlarm(); // stop alarm receiver
                    Intent serviceIntent = makeServiceIntent(desiredPosition);
                    stopService(serviceIntent);// stop current service
                    removeUserMarker();
                    appSettings.deleteSharedPreferences();
                    toast("Alarm Off!");
                    seekBar.setThumb(ContextCompat.getDrawable(getApplicationContext(), R.drawable.thumb_image_off));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

        });
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

    private void addUserDestinationToMap(LatLng desiredPosition) {
        if(userSelection != null){
            userSelection.remove();
        }
        userSelection = mMap.addMarker(new MarkerOptions()
                                            .position(desiredPosition)
                                            .title(YOUR_DESTINATION)
                                            .icon(getMarkerIcon(DESTINATION_MARKER_COLOUR)));
    }

    public BitmapDescriptor getMarkerIcon(String color) {
        float[] hsv = new float[3];
        Color.colorToHSV(Color.parseColor(color), hsv);
        return BitmapDescriptorFactory.defaultMarker(hsv[0]);
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

    @NonNull
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

    private boolean alarmOff(int progress) {
        return progress <= ALARM_THRESHOLD && desiredPosition != null && alarmSet && distance();
    }

    private boolean alarmOn(int progress) {
        distance();
        return progress > ALARM_THRESHOLD && desiredPosition != null && !alarmSet && distance();
    }

    private void removeUserMarker() {
        if(userSelection != null){
            userSelection.remove();
        }
        desiredPosition = null;
    }

    private boolean distance() {
        float[] results = new float[1];
        if(mLastLocation != null && desiredPosition != null){
            Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), desiredPosition.latitude, desiredPosition.longitude, results);
        } else {
            toast(NO_DESTINATION_ERROR);
        }
        boolean withinRange = results[0] > KILOMETRE;
        if(!withinRange){
            toast(TOO_CLOSE_ERROR);
            removeUserMarker();
            seekBar.setProgress(ALARMBAR_OFF);
        }
        return withinRange;
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
