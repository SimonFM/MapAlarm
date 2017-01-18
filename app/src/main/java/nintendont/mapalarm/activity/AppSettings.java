package nintendont.mapalarm.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import static android.content.Context.ALARM_SERVICE;
import static nintendont.mapalarm.utils.Constants.APP_PACKAGE_REFERENCE;
import static nintendont.mapalarm.utils.Constants.AlARM_SET;
import static nintendont.mapalarm.utils.Constants.KILOMETRE;
import static nintendont.mapalarm.utils.Constants.LATITUDE_KEY;
import static nintendont.mapalarm.utils.Constants.LONGITUDE_KEY;
import static nintendont.mapalarm.utils.Constants.RADIUS;

/**
 * Created by simon on 13/01/2017.
 */

public class AppSettings {
    private SharedPreferences settings;

    public AppSettings(Context context){
        settings = context.getSharedPreferences(APP_PACKAGE_REFERENCE, Context.MODE_PRIVATE);
    }

    public void saveUserDistance(int distance){
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.putInt(RADIUS, distance);
        settingsEditor.apply();
    }

    // save last location to memory
    public void saveUserSelection(LatLng desiredPosition, boolean alarmSet) {
        SharedPreferences.Editor settingsEditor = settings.edit();
        //boolean alarmWasActivated = settings.getBoolean(ALARM_SERVICE, false);
        if (alarmSet){
            settingsEditor.putBoolean(AlARM_SET, true);
            settingsEditor.putBoolean(ALARM_SERVICE, false);
            String lat = Double.toString(desiredPosition.latitude);
            String lon = Double.toString(desiredPosition.longitude);
            settingsEditor.putString(LONGITUDE_KEY, lon);
            settingsEditor.putString(LATITUDE_KEY, lat);
        }
        settingsEditor.apply();
    }

    public LatLng recoverLastUserLocation() {
        String savedLat = settings.getString(LATITUDE_KEY, "");
        String savedLon = settings.getString(LONGITUDE_KEY, "");
        LatLng desiredPosition = null;
        if(!savedLat.isEmpty() && !savedLon.isEmpty()){
            double lat = Double.parseDouble(savedLat);
            double lon = Double.parseDouble(savedLon);
            desiredPosition = new LatLng(lat, lon);
        }
        return desiredPosition;
    }

    public int recoverUserDistance(){
        int distance = settings.getInt(RADIUS, KILOMETRE);
        return distance;
    }

    public boolean recoverAlarmState(){
        return settings.getBoolean(AlARM_SET, false);
    }

    public void deleteSharedPreferences() {
        SharedPreferences.Editor settingsEditor = settings.edit();
        settingsEditor.remove(LONGITUDE_KEY);
        settingsEditor.remove(LATITUDE_KEY);
        settingsEditor.putBoolean(AlARM_SET, false);
        settingsEditor.apply();
    }
}
