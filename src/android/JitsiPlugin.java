package com.cordova.plugin.jitsi;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.CordovaWebView;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.json.JSONArray;
import org.json.JSONException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.View;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.net.MalformedURLException;
import java.net.URL;

import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetActivityDelegate;
import org.jitsi.meet.sdk.JitsiMeetActivityInterface;
import org.jitsi.meet.sdk.BroadcastEvent; // Import BroadcastEvent

import com.facebook.react.modules.core.PermissionListener;

import timber.log.Timber;

public class JitsiPlugin extends CordovaPlugin
        implements JitsiMeetActivityInterface {

    private CallbackContext _callback;
    private BroadcastReceiver jitsiBroadcastReceiver;

    public static final int TAKE_PIC_SEC = 0;

    private static final String JOIN_ACTION = "join";
    private static final String DESTROY_ACTION = "destroy";

    @Override
    public void pluginInitialize() {
        super.pluginInitialize();

        // Create the receiver to listen for Jitsi events
        jitsiBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    return;
                }

                BroadcastEvent event = new BroadcastEvent(intent);
                String eventName = "";

                Timber.d("Jitsi event received: %s", event.getType());

                switch (event.getType()) {
                    case CONFERENCE_JOINED:
                        eventName = "CONFERENCE_JOINED";
                        break;
                    case CONFERENCE_WILL_JOIN:
                        eventName = "CONFERENCE_WILL_JOIN";
                        break;
                    case CONFERENCE_TERMINATED:
                        eventName = "CONFERENCE_TERMINATED";
                        break;
                    case PARTICIPANT_JOINED:
                        eventName = "PARTICIPANT_JOINED";
                        break;
                    case PARTICIPANT_LEFT:
                        eventName = "PARTICIPANT_LEFT";
                        break;
                }

                if (!eventName.isEmpty() && _callback != null) {
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, eventName);
                    pluginResult.setKeepCallback(true);
                    _callback.sendPluginResult(pluginResult);
                }
            }
        };
        LocalBroadcastManager.getInstance(cordova.getActivity())
                .registerReceiver(jitsiBroadcastReceiver, createJitsiIntentFilter());
    }

    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
    }

    private IntentFilter createJitsiIntentFilter() {
        IntentFilter filter = new IntentFilter();
        // Add all actions that the receiver should listen for
        filter.addAction(BroadcastEvent.Type.CONFERENCE_JOINED.getAction());
        filter.addAction(BroadcastEvent.Type.CONFERENCE_WILL_JOIN.getAction());
        filter.addAction(BroadcastEvent.Type.CONFERENCE_TERMINATED.getAction());
        filter.addAction(BroadcastEvent.Type.PARTICIPANT_JOINED.getAction());
        filter.addAction(BroadcastEvent.Type.PARTICIPANT_LEFT.getAction());
        return filter;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        _callback = callbackContext;

        if (action.equals(JOIN_ACTION)) {
            String serverUrl = args.getString(0);
            String roomId = args.getString(1);
            boolean audioOnly = args.getBoolean(2);
            String token = args.getString(3);
            try {
                this.callJoin(serverUrl, roomId, audioOnly, token);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
            return true;
        } else if (action.equals(DESTROY_ACTION)) {
            this.destroy(callbackContext);
            return true;
        }
        return false;
    }

    // ... all your other methods (callJoin, join, destroy, etc.) remain the same
    // ...
    // You can now completely remove the stateChanged() method as it is no longer
    // needed.
    private void callJoin(String serverUrl, String roomId, Boolean audioOnly, String token)
            throws NameNotFoundException {
        boolean takePicturePermission = PermissionHelper.hasPermission(this, Manifest.permission.CAMERA);
        boolean micPermission = PermissionHelper.hasPermission(this, Manifest.permission.RECORD_AUDIO);

        // CB-10120: The CAMERA permission does not need to be requested unless it is
        // declared
        // in AndroidManifest.xml. This plugin does not declare it, but others may and
        // so we must
        // check the package info to determine if the permission is present.

        Timber.e("tp : %s", takePicturePermission);
        Timber.e("mp : %s", micPermission);

        if (!takePicturePermission) {
            takePicturePermission = true;

            PackageManager packageManager = this.cordova.getActivity().getPackageManager();
            String[] permissionsInPackage = packageManager.getPackageInfo(this.cordova.getActivity().getPackageName(),
                    PackageManager.GET_PERMISSIONS).requestedPermissions;

            if (permissionsInPackage != null) {
                for (String permission : permissionsInPackage) {
                    if (permission.equals(Manifest.permission.CAMERA)) {
                        takePicturePermission = false;
                        break;
                    }
                }
            }
        }

        if (!takePicturePermission) {
            PermissionHelper.requestPermissions(this, TAKE_PIC_SEC,
                    new String[] { Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO });
        } else {
            this.join(serverUrl, roomId, audioOnly, token);
        }
    }

    private void join(final String serverUrl, final String roomId, final Boolean audioOnly, final String token) {
        Timber.e("join called! Server: " + serverUrl + ", room : " + roomId);

        cordova.getActivity().runOnUiThread(() -> {
            URL serverUrlObject;
            try {
                serverUrlObject = new URL(serverUrl);
            } catch (MalformedURLException e) {
                Timber.e(e, "Invalid server URL");
                return;
            }

            JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(serverUrlObject)
                    .setRoom(roomId)
                    .setSubject(" ")
                    .setToken(token)
                    .setAudioOnly(audioOnly)
                    .setFeatureFlag("chat.enabled", true)
                    .setFeatureFlag("add-people.enabled", false)
                    .setFeatureFlag("invite.enabled", false)
                    .setFeatureFlag("calendar.enabled", false)
                    .setFeatureFlag("call-integration.enabled", false)
                    .setFeatureFlag("close-captions.enabled", false)
                    .setFeatureFlag("ios.recording.enabled", false)
                    .setFeatureFlag("kick-out.enabled", false)
                    .setFeatureFlag("live-streaming.enabled", false)
                    .setFeatureFlag("meeting-name.enabled", false)
                    .setFeatureFlag("meeting-password.enabled", false)
                    .setFeatureFlag("raise-hand.enabled", false)
                    .setFeatureFlag("recording.enabled", false)
                    .setFeatureFlag("server-url-change.enabled", false)
                    .setFeatureFlag("video-share.enabled", false)
                    .setFeatureFlag("welcomepage.enabled", false)
                    .setFeatureFlag("help.enabled", false)
                    .setFeatureFlag("lobby-mode.enabled", false)
                    .setFeatureFlag("server-url-change.enabled", false)
                    .setConfigOverride("requireDisplayName", false)
                    .setConfigOverride("disableModeratorIndicator", true)
                    .build();

            JitsiMeetActivity.launch(cordova.getActivity(), options);
        });
    }

    private void destroy(final CallbackContext callbackContext) {
        cordova.getActivity().runOnUiThread(() -> {
            JitsiMeetActivityDelegate.onHostDestroy(cordova.getActivity());
            cordova.getActivity().setContentView(getView());
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, "DESTROYED");
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
        });
    }

    private View getView() {
        try {
            return (View) webView.getClass().getMethod("getView").invoke(webView);
        } catch (Exception e) {
            return (View) webView;
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final String[] permissions,
            final int[] grantResults) {
        JitsiMeetActivityDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void requestPermissions(String[] permissions, int requestCode, PermissionListener listener) {
        JitsiMeetActivityDelegate.requestPermissions(cordova.getActivity(), permissions, requestCode, listener);
    }

    @Override
    public boolean shouldShowRequestPermissionRationale(String permissions) {
        return true;
    }

    @Override
    public int checkSelfPermission(String permission) {
        return 0;
    }

    @Override
    public int checkPermission(String permission, int pid, int uid) {
        return 0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister the receiver when the plugin is destroyed to prevent memory leaks
        if (jitsiBroadcastReceiver != null) {
            LocalBroadcastManager.getInstance(cordova.getActivity())
                    .unregisterReceiver(jitsiBroadcastReceiver);
            jitsiBroadcastReceiver = null;
        }
    }
}