package com.pratikrathod.sky;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

public class CustomPointWidget extends GlobalActionBarService {

    private View floatingView;
    final String TAG = "DEBUG";

    static boolean isServiceRunning = false;

    static int xPoint = 672, yPoint = 219;

    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;

        floatingView = LayoutInflater.from(this).inflate(R.layout.custom_point_overlay_layout, new FrameLayout(this));

        PreferencesPointUtil pointUtil = new PreferencesPointUtil();

        //Specify the view position
        lp[1].gravity = Gravity.TOP | Gravity.START;
        lp[1].x = pointUtil.readPrefPoint(getApplicationContext())[0];
        lp[1].y = pointUtil.readPrefPoint(getApplicationContext())[1];

        assert wm != null;
        wm.addView(floatingView, lp[1]);

        floatingView.findViewById(R.id.root_layout).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;


            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        //remember the initial position.
                        initialX = lp[1].x;
                        initialY = lp[1].y;

                        //get the touch location
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        Log.d("POINT", "onTouch: X" + initialTouchX + " Y" + initialTouchY);
                        return true;
                    case MotionEvent.ACTION_UP:

                        int XDiff = (int) (event.getRawX() - initialTouchX);
                        int YDiff = (int) (event.getRawY() - initialTouchY);

                        if (XDiff < 10 && YDiff < 10) {
                            Log.d("POINT", "onTouch: X" + initialTouchX + " Y" + initialTouchY);
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:

                        //Calculate the X and Y coordinates of the view.
                        lp[1].x = initialX + (int) (event.getRawX() - initialTouchX);
                        lp[1].y = initialY + (int) (event.getRawY() - initialTouchY);

                        xPoint = lp[1].x;
                        yPoint = lp[1].y;
                        Log.d(TAG, "onTouch: x" + xPoint + " y:" + yPoint);

                        //Update the layout with new X & Y coordinate
                        wm.updateViewLayout(floatingView, lp[1]);
                        return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) {
            wm.removeView(floatingView);
            isServiceRunning = false;
        }
    }
}
