package com.pratikrathod.sky;

import static java.lang.Thread.sleep;
import static java.util.regex.Pattern.compile;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GlobalActionBarService extends AccessibilityService {

    @SuppressLint("StaticFieldLeak")
    private static FrameLayout mLayout = null;
    //static int Global_I
    static volatile int pause_var = 0;
    static volatile boolean stop = true;
    private static boolean expand = true;
    private GestureDescription[] keyChords;
    private JSONArray musicSheetArray = null;
    private JSONObject keys = null;
    private int keyIndex = 0;
    private String keyIndexString = null;
    private int ms = 250;
    //Extract Keycode
    private final Pattern p = compile("[0-9]+$");
    private Matcher m = null;
    private String s = null;
    private int findGap = 0;
    String musicSheetSelected = "1test.json";
    static int TILE_STATE = 1;
    protected static WindowManager wm;
    protected static WindowManager.LayoutParams[] lp;
    //Image button objects
    private ImageButton playButton;
    private ImageButton selectTrack;
    private ImageButton customSettings;
    private ImageButton closeBtn;
    private ImageButton expandBtn;
    //CustomPointWidget
    private Intent serviceIntent;
    private static Future<?> f = null;

    @Override
    protected void onServiceConnected() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        serviceIntent = new Intent(getApplicationContext(), CustomPointWidget.class);
        mLayout = new FrameLayout(this);
        lp = setLayoutParams();

        lp[0].gravity = Gravity.START;
        lp[0].flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        LayoutInflater.from(this).inflate(R.layout.action_bar, mLayout);

        playButton = mLayout.findViewById(R.id.play);
        customSettings = mLayout.findViewById(R.id.custom_settings);
        selectTrack = mLayout.findViewById(R.id.select_track);
        closeBtn = mLayout.findViewById(R.id.close_view);
        expandBtn = mLayout.findViewById(R.id.expand_view);
        closeBtn.setVisibility(View.GONE);
        customSettings.setVisibility(View.GONE);
        //configuring all buttons
        configurePlayButton();
        configureSelectTrackButton();
        configureSettings();
        configureClose();
        configureExpand();
    }

    private void configureExpand() {
        expandBtn.setOnClickListener(v -> {
            int visible;
            if (expand) {
                visible = View.VISIBLE;
                expandBtn.setImageResource(R.drawable.ic_baseline_collapse_24);
                expand = false;
            } else {
                visible = View.GONE;
                expandBtn.setImageResource(R.drawable.ic_baseline_expand_24);
                expand = true;
            }
            closeBtn.setVisibility(visible);
            customSettings.setVisibility(visible);
        });
    }

    private WindowManager.LayoutParams[] setLayoutParams() {
        WindowManager.LayoutParams[] layoutParams = new WindowManager.LayoutParams[4];
        for (int i = 0; i < 4; i++) {
            layoutParams[i] = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }
        return layoutParams;
    }

    static int viewToggle() {
        ImageButton playButton = mLayout.findViewById(R.id.play);
        ImageButton customSettings = mLayout.findViewById(R.id.custom_settings);
        ImageButton selectTrack = mLayout.findViewById(R.id.select_track);
        ImageButton closeBtn = mLayout.findViewById(R.id.close_view);
        ImageButton expandBtn = mLayout.findViewById(R.id.expand_view);
        try {
            assert wm != null;
            if (mLayout.getWindowToken() == null) {
                wm.addView(mLayout, lp[0]);
                Log.d("debug", "ViewAdded");
                TILE_STATE = 2;
            } else {
                stop = true;
                pause_var = 0;

                playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
                customSettings.setImageResource(R.drawable.ic_baseline_settings_24);
                expandBtn.setImageResource(R.drawable.ic_baseline_expand_24);
                selectTrack.setEnabled(true);
                closeBtn.setVisibility(View.GONE);
                customSettings.setVisibility(View.GONE);
                expand = true;
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
            playButton.setOnLongClickListener(view -> {
                pause_var = 0;
                Toast.makeText(getApplicationContext(), "Song Reset Successfully", Toast.LENGTH_SHORT).show();
                return true;
            });
            playButton.setOnClickListener(view -> {
                if (CustomPointWidget.isServiceRunning) {
                    PreferencesPointUtil.savePref(getApplicationContext());
                    toggleCustomService();
                    keyChords = buildGestureKeyChords();
                    Toast.makeText(getApplicationContext(), "Done", Toast.LENGTH_SHORT).show();
                } else {
                    if (stop) {
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
                }
            });
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
    }


    private void configureSelectTrackButton() {
        //Create Dialog Box
        String[] songs = null;
        try {
            final AssetManager assets = this.getAssets();
            songs = assets.list("musicSheet/");
        } catch (Exception e) { e.printStackTrace(); }

        View selectTrackView= LayoutInflater.from(this).inflate(R.layout.select_track_overlay, new FrameLayout(this));
        ListView lv = selectTrackView.findViewById(R.id.select_track_over);
        lp[3].gravity = Gravity.CENTER;

        final String[] finalSongs = songs;
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1,finalSongs);
        lv.setAdapter(arrayAdapter);

        lv.setOnItemClickListener((parent, view, position, id) -> {
            musicSheetSelected = finalSongs[(int) id];
            Toast.makeText(getApplicationContext(), musicSheetSelected.replace(".txt","")+" Selected", Toast.LENGTH_SHORT).show();
            wm.removeView(selectTrackView);
        });

        //selectTrack
        selectTrack.setOnClickListener(view -> {
            if (stop) {
                pause_var = 0;
                if(selectTrackView.getWindowToken() == null)
                    wm.addView(selectTrackView,lp[3]);
            } else
                Toast.makeText(getApplicationContext(), "Can't select music sheet while playing", Toast.LENGTH_SHORT).show();
        });
    }

    private void configureSettings() {
        customSettings.setOnClickListener(view -> {
            if (stop) toggleCustomService();
        });
    }

    private void toggleCustomService() {
        int visible;
        if (!CustomPointWidget.isServiceRunning) {
            playButton.setImageResource(R.drawable.ic_round_done_all_24);
            customSettings.setImageResource(R.drawable.ic_baseline_close_24);
            visible = View.GONE;
            selectTrack.setEnabled(false);
            this.startService(serviceIntent);
        } else {
            this.stopService(serviceIntent);
            visible = View.VISIBLE;
            playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24);
            customSettings.setImageResource(R.drawable.ic_baseline_settings_24);
            selectTrack.setEnabled(true);
        }
        closeBtn.setVisibility(visible);
        expandBtn.setVisibility(visible);
    }

    private void configureClose() {
        closeBtn.setOnClickListener(v -> viewToggle());
    }

    @SuppressWarnings("BusyWait")
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

            Runnable mRunnable = () -> {
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
                            stop = true;
                            pause_var = 0;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (!stop)
                    pause_var = 0;
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
        int[] xCordsArr = PreferencesPointUtil.readPrefPoint(getApplicationContext());

        //Final Coordinates for Piano lol
        float[] xCords = {672, 875, 1175, 1416, 1661};
        float[] yCords = {219, 453, 661};

        int diffXYCords = xCordsArr[2] - xCordsArr[0];

        xCords[0] = xCordsArr[0] + 100;
        yCords[0] = xCordsArr[1] + 100;

        xCords[1] = xCords[0] + diffXYCords;
        xCords[2] = xCords[1] + diffXYCords;
        xCords[3] = xCords[2] + diffXYCords;
        xCords[4] = xCords[3] + diffXYCords;

        yCords[1] = yCords[0] + diffXYCords;
        yCords[2] = yCords[1] + diffXYCords;

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
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {}

    @Override
    public void onInterrupt() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
