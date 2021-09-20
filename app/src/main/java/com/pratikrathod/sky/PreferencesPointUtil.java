package com.pratikrathod.sky;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public class PreferencesPointUtil {
   static void savePref(@NonNull Context context) {
       SharedPreferences preferences = context.getSharedPreferences(context.getPackageName() + "sky_preferences", Activity.MODE_PRIVATE);
        SharedPreferences.Editor myEdit = preferences.edit();
        myEdit.putInt("x", CustomPointWidget.xPoint[0]);
        myEdit.putInt("y", CustomPointWidget.yPoint[0]);
        myEdit.putInt("x1", CustomPointWidget.xPoint[1]);
        myEdit.apply();
    }

    static int[] readPrefPoint(@NonNull Context context) {
        SharedPreferences preferences = context.getSharedPreferences(context.getPackageName() + "sky_preferences", Activity.MODE_PRIVATE);
        int[] pointXY = new int[3];
        pointXY[0] = preferences.getInt("x", 672);
        pointXY[1] = preferences.getInt("y", 219);
        pointXY[2] = preferences.getInt("x1", 875);
        return pointXY;
    }
}
