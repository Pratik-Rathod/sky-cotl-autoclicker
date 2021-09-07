package com.pratikrathod.sky;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

@SuppressLint("ClickableViewAccessibility")
public class CustomPointWidget extends Service {

    private WindowManager windowManager;
    private View floatingView;
    final String TAG = "DEBUG";

    private WindowManager.LayoutParams params;
    static boolean isServiceRunning = false;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static int xPoint = 672, yPoint = 219;

    @SuppressLint("InflateParams")
    @Override
    public void onCreate() {
        super.onCreate();
        isServiceRunning = true;

        floatingView = LayoutInflater.from(this).inflate(R.layout.custom_point_overlay_layout, null);

        int LAYOUT_FLAG;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        //get pref point
        PreferencesPointUtil pointUtil = new PreferencesPointUtil();

        //Specify the view position
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = pointUtil.readPrefPoint(getApplicationContext())[0];
        params.y = pointUtil.readPrefPoint(getApplicationContext())[1];


        //Add the view to the window
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        assert windowManager != null;
        windowManager.addView(floatingView, params);

        floatingView.findViewById(R.id.root_layout).setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;


            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:

                        //remember the initial position.
                        initialX = params.x;
                        initialY = params.y;

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
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);

                        xPoint = params.x;
                        yPoint = params.y;
                        Log.d(TAG, "onTouch: x" + xPoint + " y:" + yPoint);

                        //Update the layout with new X & Y coordinate
                        windowManager.updateViewLayout(floatingView, params);
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
            windowManager.removeView(floatingView);
            isServiceRunning = false;
        }
    }
}
