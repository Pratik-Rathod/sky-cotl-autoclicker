package com.pratikrathod.sky;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesPointUtil {

    SharedPreferences preferences = null;
    PreferencesPointUtil() {
    }

    void savePref(int no, Context context) {

        preferences = context.getSharedPreferences("sky_preferences", Activity.MODE_PRIVATE);
        SharedPreferences.Editor myEdit = preferences.edit();

        if (no == 0) {
            myEdit.putInt("x", CustomPointWidget.xPoint);
            myEdit.putInt("y", CustomPointWidget.yPoint);
        } else {
            myEdit.putInt("x1", CustomPointWidget.xPoint);
        }
        myEdit.apply();
    }

    int[] readPrefPoint(Context context) {
        int[] pointXY = new int[3];
        preferences = context.getSharedPreferences("sky_preferences", Activity.MODE_PRIVATE);

        pointXY[0] = preferences.getInt("x", 672);
        pointXY[1] = preferences.getInt("y", 219);
        pointXY[2] = preferences.getInt("x1", 875);

        return pointXY;
    }
}
