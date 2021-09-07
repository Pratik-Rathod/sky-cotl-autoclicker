package com.pratikrathod.sky;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Thread.sleep;
import static java.util.regex.Pattern.compile;


public class GlobalActionBarService extends AccessibilityService {

    @SuppressLint("StaticFieldLeak")
    private static FrameLayout mLayout = null;
    //static int Global_I
    static volatile int pause_var = 0;
    static volatile boolean stop = true;
    static GestureDescription[] keyChords;
    private JSONArray musicSheetArray = null;
    private JSONObject keys = null;
    private int keyIndex = 0;
    private String keyIndexString = null;
    private int ms = 250;
    //Extract Keycode
    private final Pattern p = compile("[0-9]+$");
    private Matcher m = null;
    private String s = null;
    int findGap = 0;
    Runnable mRunnable = null;
    String musicSheetSelected = "1test.json";
    static int TILE_STATE = 1;
    static WindowManager wm;
    static WindowManager.LayoutParams lp;
    static boolean isPlaying = false;
    //Image button objects
    private ImageButton playButton;
    private ImageButton selectTrack;
    private ImageButton customSettings;
    //CustomPointWidget
    private final PreferencesPointUtil pointUtil = new PreferencesPointUtil();
    private Intent serviceIntent;
    int pointNo = 0;
    private static Future<?> f = null;

    @Override
    protected void onServiceConnected() {

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        lp = new WindowManager.LayoutParams();
        serviceIntent = new Intent(getApplicationContext(), CustomPointWidget.class);

        // Create an overlay and display the action bar
        mLayout = new FrameLayout(this);
        lp.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        lp.format = PixelFormat.TRANSLUCENT;
        lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.START;
        LayoutInflater inflater = LayoutInflater.from(this);
        inflater.inflate(R.layout.action_bar, mLayout);

        playButton = mLayout.findViewById(R.id.play);
        customSettings = mLayout.findViewById(R.id.custom_settings);
        selectTrack = mLayout.findViewById(R.id.select_track);

        //check
        configurePlayButton();
        configureSelectTrackButton();
        configureSettings();

    }

    static int viewToggle() {

        ImageButton playButton = mLayout.findViewById(R.id.play);
        ImageButton customSettings = mLayout.findViewById(R.id.custom_settings);
        ImageButton selectTrack = mLayout.findViewById(R.id.select_track);

        try {
            assert wm != null;
            if (mLayout.getWindowToken() == null) {
                wm.addView(mLayout, lp);
                Log.d("debug", "ViewAdded");
                TILE_STATE = 2;
            } else {
                stop = true;
                isPlaying = false;
                pause_var = 0;

                playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                customSettings.setImageResource(R.drawable.ic_baseline_settings_24);
                selectTrack.setEnabled(true);

                wm.removeView(mLayout);
                Log.d("debug", "ViewRemoved");
                TILE_STATE = 1;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return TILE_STATE;
    }

    private void configurePlayButton() {

        keyChords = buildGestureKeyChords();

        try {
            playButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    pause_var = 0;
                    Toast.makeText(getApplicationContext(), "Song Reset Successfully", Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
            playButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (CustomPointWidget.isServiceRunning) {
                        if (pointNo == 0) {
                            pointUtil.savePref(pointNo, getApplicationContext());
                            pointNo++;
                            Toast.makeText(getApplicationContext(), "Now please select 2nd point", Toast.LENGTH_SHORT).show();
                        } else {
                            pointUtil.savePref(pointNo, getApplicationContext());
                            pointNo = 0;
                            Toast.makeText(getApplicationContext(), "Both point selected ! ", Toast.LENGTH_SHORT).show();
                            toggleCustomService();
                            keyChords = buildGestureKeyChords();
                        }
                    } else {
                        if (!isPlaying) {
                            playButton.setImageResource(R.drawable.ic_baseline_pause_24);
                            stop = false;
                            if (f != null)
                                if (f.isDone())
                                    playMusic();
                                else
                                    Log.d("sky", "onClick: Thread already running");
                            else
                                playMusic();
                        } else {
                            stop = true;
                            playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                        }
                        isPlaying = !isPlaying;
                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
    }


    private void configureSelectTrackButton() {

        //Create Dialog Box
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setTitle("Select music sheet");
        String[] songs = null;
        try {
            final AssetManager assets = this.getAssets();
            songs = assets.list("musicSheet/");
        } catch (Exception e) {
            e.printStackTrace();
        }

        final String[] finalSongs = songs;
        builder.setItems(songs, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                assert finalSongs != null;
                musicSheetSelected = finalSongs[i];
                Toast.makeText(getApplicationContext(), finalSongs[i] + " is selected", Toast.LENGTH_SHORT).show();
                dialogInterface.dismiss();
            }
        });

        final AlertDialog alert = builder.create();

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        Objects.requireNonNull(alert.getWindow()).setType(LAYOUT_FLAG);

        //selectTrack
        selectTrack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (stop) {
                    pause_var = 0;
                    alert.show();
                } else
                    Toast.makeText(getApplicationContext(), "Your Song is playing", Toast.LENGTH_SHORT).show();
            }
        });

    }
    private void configureSettings() {

        customSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleCustomService();
            }
        });
    }


    private void toggleCustomService() {

        if (!CustomPointWidget.isServiceRunning) {
            playButton.setImageResource(R.drawable.ic_twotone_add_circle_24);
            customSettings.setImageResource(R.drawable.ic_baseline_close_24);
            Toast.makeText(getApplicationContext(), "Please select initial point()", Toast.LENGTH_SHORT).show();
            selectTrack.setEnabled(false);
            this.startService(serviceIntent);
        } else {
            this.stopService(serviceIntent);
            playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            customSettings.setImageResource(R.drawable.ic_baseline_settings_24);
            selectTrack.setEnabled(true);
        }
    }


    private void playMusic() {
        try {
            //JSON Objects % arrays
            JSONArray array = new JSONArray(LoadMusicSheet());
            JSONObject object = array.getJSONObject(0);
            int bpm = Integer.parseInt(object.getString("bpm"));
            ms = 60000 / bpm;
            musicSheetArray = object.getJSONArray("songNotes");
            keys = null;

            final int[] nextMusicTime = new int[1];
            final int[] currentMusicTime = new int[1];
            playButton = mLayout.findViewById(R.id.play);

            mRunnable = new Runnable() {
                @SuppressWarnings("BusyWait")
                @Override
                public void run() {
                    for (int i = pause_var; i < musicSheetArray.length(); i++) {
                        if (stop) {
                            pause_var = i;
                            break;
                        }
                        try {
                            keys = musicSheetArray.getJSONObject(i);
                            keyIndexString = keys.getString("key");

                            if (i + 1 < musicSheetArray.length()) {
                                nextMusicTime[0] = musicSheetArray.getJSONObject(i + 1).getInt("time");
                                currentMusicTime[0] = keys.getInt("time");
                                findGap = nextMusicTime[0] - currentMusicTime[0];

                            }

                            m = p.matcher(keyIndexString);
                            if (m.find())
                                s = m.group();
                            keyIndex = Integer.parseInt(s);

                            dispatchGesture(keyChords[keyIndex], null, null);

                            if (i + 1 < musicSheetArray.length()) {
                                if (findGap > ms) {
                                    sleep(findGap);
                                } else if (currentMusicTime[0] == nextMusicTime[0]) {
                                    sleep(20);
                                } else {
                                    sleep(ms);
                                }
                            } else {
                                playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                                isPlaying = false;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (!stop)
                        pause_var = 0;
                }
            };
            ExecutorService executorService = Executors.newFixedThreadPool(1);
            f = executorService.submit(mRunnable);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String LoadMusicSheet() {
        String json;
        try {
            InputStream in = this.getAssets().open("musicSheet/" + musicSheetSelected);
            int size = in.available();
            byte[] buffer = new byte[size];
            //noinspection ResultOfMethodCallIgnored
            in.read(buffer);
            in.close();
            json = new String(buffer, StandardCharsets.UTF_8);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }


    //Map Coordinates into keyChords & build the gesture
    private GestureDescription[] buildGestureKeyChords() {
    
        int i;
        //Default Duration for keyStroke is 100 ms
        final long DURATION = 100;
        int[] xCordsArr = pointUtil.readPrefPoint(getApplicationContext());

        //Final Coordinates for Piano lol
        int[] xCords = {672, 875, 1175, 1416, 1661};
        int[] yCords = {219, 453, 661};

        int diffXYCords = xCordsArr[2] - xCordsArr[0];

        if (diffXYCords > 20) {
            xCords[0] = xCordsArr[0] + 100;
            yCords[0] = xCordsArr[1] + 100;

            xCords[1] = xCords[0] + diffXYCords;
            xCords[2] = xCords[1] + diffXYCords;
            xCords[3] = xCords[2] + diffXYCords;
            xCords[4] = xCords[3] + diffXYCords;

            yCords[1] = yCords[0] + diffXYCords;
            yCords[2] = yCords[1] + diffXYCords;
        }

        Log.d("sky", "buildGestureKeyChords:" + xCords[0] + " " + xCords[1] + " " + xCords[2] + " " + xCords[3] + " " + xCords[4]);

        //Array object for pathChords
        final Path[] keyChordsPath = new Path[15];

        //Array of taps stock object
        GestureDescription.StrokeDescription[] keyChordsStroke = new GestureDescription.StrokeDescription[15];
        GestureDescription.Builder[] keyChordsBuilder = new GestureDescription.Builder[15];
        GestureDescription[] keyChords = new GestureDescription[15];

        i = 0;
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 5; x++) {
                keyChordsPath[i] = new Path();
                keyChordsPath[i].moveTo(xCords[x], yCords[y]);
                keyChordsStroke[i] = new GestureDescription.StrokeDescription(keyChordsPath[i], 0, DURATION);
                keyChordsBuilder[i] = new GestureDescription.Builder();
                keyChordsBuilder[i].addStroke(keyChordsStroke[i]);
                keyChords[i] = keyChordsBuilder[i].build();
                i++;
            }
        }
        return keyChords;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override
    public void onInterrupt() {
    }

}
