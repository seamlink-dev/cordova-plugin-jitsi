package com.cordova.plugin.jitsi;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class JitsiService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int
            startId) {
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent i = new Intent(JitsiMeetPluginActivity.APP_HAS_STOPPED);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
        stopSelf();
    }

}
