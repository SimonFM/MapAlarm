package nintendont.mapalarm.utils;

import android.graphics.Color;

/**
 * Created by simon on 28/12/2016.
 */

public class Constants {
    public static final String APP_PACKAGE_REFERENCE = "nintendont.mapalarm";
    public static final String LOCATION_KEY = APP_PACKAGE_REFERENCE.concat(".location");

    //SharedPreferences
    public static final String LATITUDE_KEY = LOCATION_KEY.concat(".longitude.key");
    public static final String LONGITUDE_KEY = LOCATION_KEY.concat(".latitude.key");
    public static final String AlARM_SET = APP_PACKAGE_REFERENCE.concat(".alarm.value");
    public static final String AlARM_SERVICE = APP_PACKAGE_REFERENCE.concat(".service.alarm.value");

    //Broadcast Receiver Intent things
    public static final String LATITUDE = LOCATION_KEY.concat(".latitude");
    public static final String LONGITUDE = LOCATION_KEY.concat(".longitude");

    //Values
    public static final int KILOMETRE = 1000;
    public static final int TEST = 500;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 2711;
    public static final int DEFAULT_ZOOM = 14;
    public static final int ONE_SECOND = 1000;
    public static final int HALF_MINUTE = 30 * ONE_SECOND;
    public static final int FIVE_SECONDS = 5 * ONE_SECOND;
    public static final int ALARM_THRESHOLD = 50;
    public static final int ALARMBAR_ON = 75;
    public static final int ALARMBAR_OFF = 0;

    //Colours
    public static final String DESTINATION_MARKER_COLOUR = "#BCD0F2";

    //Tutorial things
    //colours
    public static final int RED_HEADING_COLOUR = Color.parseColor("#eb273f");
    public static final int CONTENT_COLOUR = Color.parseColor("#ffffff");
    public static final int MASK_COLOUR = Color.parseColor("#dc000000");

    //keys
    public static final String TURNING_ON_ALARM_TUTORIAL_KEY = "TurningOnAlarmTutorial";
    public static final String MAP_TUTORIAL_KEY = "MapTutorial";
    public static final String FINAL_TUTORIAL_KEY = "FinalTutorial";



    public interface ACTION {
        String STARTFOREGROUND_ACTION = "nintendont.foregroundservice.action.startforeground";
        String STOPFOREGROUND_ACTION = "nintendont.foregroundservice.action.stopforeground";
        String BROADCAST = "nintendont.foregroundservice.action.stopforeground";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 27110;
    }

}
