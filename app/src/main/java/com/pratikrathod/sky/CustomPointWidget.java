package com.pratikrathod.sky;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class CustomPointWidget extends GlobalActionBarService {

    private final View []floatingView = new View[3];
    final String TAG = "DEBUG";

    static boolean isServiceRunning = false;

    static int[] xPoint,yPoint ;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;
        xPoint = new int[2];
        yPoint = new int[2];
        PreferencesPointUtil pointUtil = new PreferencesPointUtil();
        int[] pointXY = pointUtil.readPrefPoint(getApplicationContext());
        //Specify the view position
        for(int i =0; i<2;i++){
            floatingView[i]= LayoutInflater.from(this).inflate(R.layout.custom_point_overlay_layout, new FrameLayout(this));
            lp[i+1].gravity = Gravity.TOP | Gravity.START;
            lp[i+1].x = pointXY[0];
            lp[i+1].y = pointXY[1];
        }

        lp[2].x = pointXY[2];

        assert wm != null;

        for(int i =0; i<2;i++) {
            wm.addView(floatingView[i], lp[i+1]);
            final int finalI = i;
            floatingView[i].findViewById(R.id.root_layout).setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            //remember the initial position.
                            initialX = lp[finalI +1].x;
                            initialY = lp[finalI +1].y;
                            //get the touch location
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            Log.d("POINT", "onTouch: X" + initialTouchX + " Y" + initialTouchY);
                            return true;

                        case MotionEvent.ACTION_MOVE:
                            //Calculate the X and Y coordinates of the view.
                            lp[finalI +1].x = initialX + (int) (event.getRawX() - initialTouchX);
                            lp[finalI +1].y = initialY + (int) (event.getRawY() - initialTouchY);

                            xPoint[finalI] = lp[finalI +1].x;
                            yPoint[finalI] = lp[finalI +1].y;

                            Log.d(TAG, "onTouch:["+finalI+"] x" + xPoint[finalI] + " y:" + yPoint[finalI]);
                            //Update the layout with new X & Y coordinate
                            wm.updateViewLayout(floatingView[finalI], lp[finalI +1]);
                            return true;
                    }
                    return false;
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        int i = 0;
        while(i < 2){
            if (floatingView[i] != null) {
                wm.removeView(floatingView[i]);
                isServiceRunning = false;
            }
            i++;
        }
    }
}
