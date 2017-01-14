package nintendont.mapalarm.tutorial;

import android.app.Activity;
import android.content.Context;
import android.view.View;

import com.wooplr.spotlight.SpotlightView;
import com.wooplr.spotlight.utils.SpotlightListener;

import static nintendont.mapalarm.utils.Constants.CONTENT_COLOUR;
import static nintendont.mapalarm.utils.Constants.FINAL_TUTORIAL_KEY;
import static nintendont.mapalarm.utils.Constants.MAP_TUTORIAL_KEY;
import static nintendont.mapalarm.utils.Constants.MASK_COLOUR;
import static nintendont.mapalarm.utils.Constants.RED_HEADING_COLOUR;
import static nintendont.mapalarm.utils.Constants.TURNING_ON_ALARM_TUTORIAL_KEY;
import static nintendont.mapalarm.utils.Messages.FINAL_CONTENT;
import static nintendont.mapalarm.utils.Messages.FINAL_HEADING;
import static nintendont.mapalarm.utils.Messages.MAP_TUTORIAL_CONTENT;
import static nintendont.mapalarm.utils.Messages.MAP_TUTORIAL_HEADING;
import static nintendont.mapalarm.utils.Messages.SETTING_ALARM_CONTENT;
import static nintendont.mapalarm.utils.Messages.SETTING_AN_ALARM_HEADING;

/**
 * Created by simon on 13/01/2017.
 */

public class Tutorial {
    private Context context;
    private SpotlightView.Builder alarm, map, finalTutorial;

    public Tutorial(Context context){
        this.context = context;
    }

    public void makeMapTutorial(View target) {
        SpotlightView.Builder map = makeTutorial(MAP_TUTORIAL_HEADING, MAP_TUTORIAL_CONTENT, MAP_TUTORIAL_KEY);
        map.setListener(new SpotlightListener() {
            @Override
            public void onUserClicked(String s) {
                alarm.show();
            }
        });
        map.target(target);
        this.map = map;
    }

    public void makeAlarmTutorial(View target) {
        SpotlightView.Builder tempAlarm = makeTutorial(SETTING_AN_ALARM_HEADING, SETTING_ALARM_CONTENT, TURNING_ON_ALARM_TUTORIAL_KEY);
//        tempAlarm.setListener(new SpotlightListener() {
//            @Override
//            public void onUserClicked(String s) {
//                finalTutorial.show();
//            }
//        });
        tempAlarm.target(target);
        this.alarm = tempAlarm;
    }

    public void makeFinalTutorial(){
        SpotlightView.Builder finalBuilder = makeTutorial(FINAL_HEADING, FINAL_CONTENT, FINAL_TUTORIAL_KEY);
        this.finalTutorial = finalBuilder;
    }

    public void show(){
        map.show();
    }

    private SpotlightView.Builder makeTutorial(String heading, String content, String key) {
        return new SpotlightView.Builder((Activity) context)
                .introAnimationDuration(400)
                .enableRevalAnimation(true)
                .fadeinTextDuration(400)
                .headingTvColor(RED_HEADING_COLOUR)
                .headingTvSize(20)
                .headingTvText(heading)
                .subHeadingTvColor(CONTENT_COLOUR)
                .subHeadingTvSize(18)
                .subHeadingTvText(content)
                .maskColor(MASK_COLOUR)
                .lineAnimDuration(400)
                .lineAndArcColor(RED_HEADING_COLOUR)
                .enableDismissAfterShown(true)
                .usageId(key);
    }
}
