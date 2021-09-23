package com.pratikrathod.sky;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class StorageUtil {
    Context context;
    File fileSys;
    File fileFolder;

    StorageUtil(Context context) {
        this.context = context;
        if (checkStorage()) {
            fileSys = context.getExternalFilesDir(null);
        } else {
            fileSys = context.getFilesDir();
        }
        fileFolder = new File(fileSys, "Music sheets");
    }

    //Check external storage is available or not
    private boolean checkStorage() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) || Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY);
    }

    //create primary test file for user !
    void makePrimaryFile() {
        try {
            if (!fileFolder.exists()) {
                Log.d("mkdir", "Folder status" + fileFolder.mkdir());
            }
            File testFile = new File(fileFolder, "1test.json");
            if (testFile.createNewFile()) {
                FileOutputStream fos = new FileOutputStream(testFile);
                InputStream in = context.getAssets().open("musicSheet/1test.json");
                int size = in.available();
                byte[] buffer = new byte[size];
                int read = in.read(buffer);
                fos.write(buffer, 0, read);
                in.close();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String[] listOfMusicSheets() {
        return fileFolder.list();
    }

    JSONArray LoadMusicSheet(String selectedSheet) {
        try {
            FileInputStream in = new FileInputStream(new File(fileFolder,selectedSheet));
            InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            Log.d("SKU", "LoadMusicSheet: "+builder.toString());
            in.close();
            isr.close();
            return new JSONArray(builder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
