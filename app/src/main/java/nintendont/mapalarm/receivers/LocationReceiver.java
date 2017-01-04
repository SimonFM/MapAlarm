package nintendont.mapalarm.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;

import nintendont.mapalarm.services.LocationService;
import nintendont.mapalarm.utils.Constants;

import static nintendont.mapalarm.utils.Constants.LATITUDE;
import static nintendont.mapalarm.utils.Constants.LONGITUDE;

/**
 * Created by simon on 29/12/2016.
 */

public class LocationReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = makeLocationServiceIntent(context, intent);
        context.startService(serviceIntent);
    }

    @NonNull
    private Intent makeLocationServiceIntent(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, LocationService.class);

        serviceIntent.setAction(Constants.ACTION.STARTFOREGROUND_ACTION);
        double lat = intent.getDoubleExtra(LATITUDE, 0);
        double lon = intent.getDoubleExtra(LONGITUDE, 0);

        Bundle extras = new Bundle();
        extras.putDouble(LATITUDE, lat);
        extras.putDouble(LONGITUDE, lon);
        serviceIntent.putExtras(extras);
        return serviceIntent;
    }

}
