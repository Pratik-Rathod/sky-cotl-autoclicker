package com.pratikrathod.sky;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    Switch aSwitch;
    public TextView msg;
    TextView checkFlag;
    // --Commented out by Inspection (9/7/2021 6:59 PM):AlertDialog.Builder builder;
    boolean checkEnabled;

    // --Commented out by Inspection (9/7/2021 6:52 PM):private final int EXTERNAL_STORAGE_PERMISSION_CODE = 23;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        aSwitch = findViewById(R.id.switchCheck);
        checkFlag = findViewById(R.id.checkFlag);
        checkEnabled = isAccessibilityServiceEnabled(getApplicationContext(), GlobalActionBarService.class);
        aSwitch.setClickable(checkEnabled);

        msg = findViewById(R.id.msg);


        if (!checkEnabled) {
            msg.setText(R.string.requestAccString);
            msg.setTextColor(Color.parseColor("#FF7043"));
        } else {
            msg.setText(R.string.requestTurnString);
            msg.setTextColor(Color.parseColor("#878787"));
        }


        // String[] files = getApplicationContext().fileList();
        //  Toast.makeText(this, "" + files, Toast.LENGTH_SHORT).show();
        //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},EXTERNAL_STORAGE_PERMISSION_CODE);
    }


    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        assert am != null;
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo enabledService : enabledServices) {
            ServiceInfo enabledServiceInfo = enabledService.getResolveInfo().serviceInfo;
            if (enabledServiceInfo.packageName.equals(context.getPackageName()) && enabledServiceInfo.name.equals(service.getName()))
                return true;
        }
        return false;
    }

    public void openAccessibility(View view) {
        Intent intent = new
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
    }

    public void checkSwitch(View view) {
        //  GlobalActionBarService.viewToggle();
        if (aSwitch.isChecked()) {
            try {
                //    checkFlag.setText("Enabled");
                msg.setText(R.string.requestTurnString);
                msg.setTextColor(Color.parseColor("#878787"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                checkFlag.setText("Disabled");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void onResume() {
        boolean checkEnabled = isAccessibilityServiceEnabled(getApplicationContext(), GlobalActionBarService.class);
        aSwitch.setClickable(checkEnabled);

        if (!checkEnabled) {
            msg.setText(R.string.requestAccString);
            msg.setTextColor(Color.parseColor("#FF7043"));
        } else {
            msg.setText(R.string.requestTurnString);
        }

        super.onResume();
    }
}