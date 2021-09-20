package com.pratikrathod.sky;

import android.content.Intent;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

public class QuickPlayTile extends TileService {

    boolean access = false;
    @Override
    public void onTileAdded() {
        Log.d("tag", "OnTitle Added");
        super.onTileAdded();
    }

    @Override
    public void onTileRemoved() {
        Log.d("tag", "OnTitleRemoved");
        super.onTileRemoved();
    }

    @Override
    public void onStartListening() {
        access = MainActivity.isAccessibilityServiceEnabled(getApplicationContext(), GlobalActionBarService.class);
        Tile tile = getQsTile();
        if(access){
            tile.setState(GlobalActionBarService.TILE_STATE);
        }else{
            tile.setState(Tile.STATE_INACTIVE);
        }
        tile.updateTile();
        super.onStartListening();
        Log.d("tag", "TIle Listening");
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        Log.d("tag", "TileStop Listening");
    }

    @Override
    public void onClick() {
        super.onClick();
        Tile tile = getQsTile();
        if (access) {
            tile.setState(GlobalActionBarService.viewToggle());
            if(CustomPointWidget.isServiceRunning)
                stopService(new Intent(this,CustomPointWidget.class));
        }
        else {
            Toast.makeText(getApplicationContext(), "Need access", Toast.LENGTH_SHORT).show();
        }

        tile.updateTile();

        Intent closeIntent = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        getApplicationContext().sendBroadcast(closeIntent);

    }
}