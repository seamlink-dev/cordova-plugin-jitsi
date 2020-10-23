package com.cordova.plugin.jitsi;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.jitsi.meet.sdk.JitsiMeetActivity;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class JitsiMeetPluginActivity extends JitsiMeetActivity {
    
    private boolean mRestoreStack;

    public static final String APP_HAS_STOPPED = "APP_HAS_STOPPED";

    @Override
    public void onDestroy() {
        super.onDestroy();
        JitsiPluginModel.getInstance().changeState("onConferenceTerminated");
    }

    @Override
    public void onPictureInPictureModeChanged(
            boolean isInPictureInPictureMode) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode);
        if (!isInPictureInPictureMode) {
            this.startActivity(new Intent(this, getClass())
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
            mRestoreStack = true;
        }
    }

    public static void navToLauncherTask(Context appContext) {
        ActivityManager activityManager = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
        assert activityManager != null;
        final List<ActivityManager.AppTask> appTasks = activityManager.getAppTasks();
        for (ActivityManager.AppTask task : appTasks) {
            final Intent baseIntent = task.getTaskInfo().baseIntent;
            final Set<String> categories = baseIntent.getCategories();
            if (categories != null && categories.contains(Intent.CATEGORY_LAUNCHER)) {
                task.moveToFront();
                return;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        IntentFilter filter = new IntentFilter();
        filter.addAction(APP_HAS_STOPPED);

        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                finish();
            }
        };

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, filter);
        startService(new Intent(getApplicationContext(), JitsiService.class));
    }

    @Override
    public void finish() {
        if (mRestoreStack) {
            navToLauncherTask(getApplicationContext());
        }
        stopService(new Intent(getApplicationContext(), JitsiService.class));
        finishAndRemoveTask();
        JitsiPluginModel.getInstance().changeState("onConferenceTerminated");
        super.finish();

    }

    @Override
    public void onConferenceJoined(Map<String, Object> data) {
        JitsiPluginModel.getInstance().changeState("onConferenceJoined");
        super.onConferenceJoined(data);
    }

    @Override
    public void onConferenceTerminated(Map<String, Object> data) {
        JitsiPluginModel.getInstance().changeState("onConferenceTerminated");
        super.onConferenceTerminated(data);
    }

    @Override
    public void onConferenceWillJoin(Map<String, Object> data) {
        JitsiPluginModel.getInstance().changeState("onConferenceWillJoin");
        super.onConferenceWillJoin(data);
    }
}