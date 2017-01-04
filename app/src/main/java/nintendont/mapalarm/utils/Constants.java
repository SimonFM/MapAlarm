package nintendont.mapalarm.utils;

/**
 * Created by simon on 28/12/2016.
 */

public class Constants {
    public static final String YOUR_DESTINATION = "Your Destination";
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

    public interface ACTION {
        String STARTFOREGROUND_ACTION = "nintendont.foregroundservice.action.startforeground";
        String STOPFOREGROUND_ACTION = "nintendont.foregroundservice.action.stopforeground";
        String BROADCAST = "nintendont.foregroundservice.action.stopforeground";
    }

    public interface NOTIFICATION_ID {
        int FOREGROUND_SERVICE = 27110;
    }

}
