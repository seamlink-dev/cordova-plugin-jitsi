package com.cordova.plugin.jitsi;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PermissionHelper;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Intent;

import java.net.MalformedURLException;
import java.net.URL;

import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;
import org.jitsi.meet.sdk.JitsiMeetActivityDelegate;
import org.jitsi.meet.sdk.JitsiMeetActivityInterface;
import android.view.View;

import org.apache.cordova.CordovaWebView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import com.facebook.react.modules.core.PermissionListener;

import timber.log.Timber;

public class JitsiPlugin extends CordovaPlugin 
      implements JitsiMeetActivityInterface, JitsiPluginModel.OnJitsiPluginStateListener {

  private CallbackContext _callback;
  private static final String TAG = "cordova-plugin-jitsi";


  final static String[] permissions = { Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO };
  public static final int PERMISSION_DENIED_ERROR = 20;
  public static final int TAKE_PIC_SEC = 0;
  public static final int REC_MIC_SEC = 1;

  private static final String JOIN_ACTION = "join";
  private static final String DESTROY_ACTION = "destroy";

  private String serverUrl;
  private String roomId;
  private boolean audioOnly;
  private String token;

  private String _conferenceState = "";

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    // your init code here
    JitsiPluginModel.getInstance().setListener(this);
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    // CB-10120: The CAMERA permission does not need to be requested unless it is
    // declared
    // in AndroidManifest.xml. This plugin does not declare it, but others may and
    // so we must
    // check the package info to determine if the permission is present.
    _conferenceState = JitsiPluginModel.getInstance().getState();

    _callback = callbackContext;

    if (action.equals(JOIN_ACTION)) {
      this.serverUrl = args.getString(0);
      this.roomId = args.getString(1);
      this.audioOnly = args.getBoolean(2);
      this.token = args.getString(3);
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

    private void callJoin(String serverUrl, String roomId, Boolean audioOnly, String token) throws NameNotFoundException {
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
    }
    else{
      this.join(serverUrl, roomId, audioOnly, token);
    }
  }

  public void onRequestPermissionResult(int requestCode, String[] permissions,
                                        int[] grantResults) throws JSONException
  {
    for(int r:grantResults)
    {
      if(r == PackageManager.PERMISSION_DENIED)
      {
        this._callback.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
        return;
      }
    }
        if (requestCode == TAKE_PIC_SEC) {
        join(this.serverUrl, this.roomId, this.audioOnly, this.token);
    }
  }

  private void join(final String serverUrl, final String roomId, final Boolean audioOnly, final String token) {
        Timber.e("join called! Server: " + serverUrl + ", room : " + roomId);

        cordova.getActivity().runOnUiThread(() -> {
        URL serverUrlObject;
        try {
          serverUrlObject = new URL(serverUrl);
        } catch (MalformedURLException e) {
          e.printStackTrace();
          throw new RuntimeException("Invalid server URL!");
        }

        JitsiMeetConferenceOptions options = new JitsiMeetConferenceOptions.Builder()
            .setRoom(serverUrl + "/" + roomId)
            .setSubject(" ")
            .setToken(token)
            .setAudioOnly(audioOnly)
            .setFeatureFlag("chat.enabled", true)
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
            .build();


            Intent intent = new Intent(cordova.getActivity(), JitsiMeetPluginActivity.class);
            intent.setAction("org.jitsi.meet.CONFERENCE");
            intent.putExtra("JitsiMeetConferenceOptions", options);
            cordova.getContext().startActivity(intent);
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
  public void onRequestPermissionsResult(final int requestCode, final String[] permissions, final int[] grantResults) {
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
  public void stateChanged() {
    _conferenceState = JitsiPluginModel.getInstance().getState();
        Timber.d("MainActivity says: Model state changed: %s", _conferenceState);
    cordova.getActivity().setContentView(getView());
    String m = "";

    switch (_conferenceState){
		case "onConferenceJoined":
		    m = "CONFERENCE_JOINED";
		    break;
		case "onConferenceWillJoin":
		    m = "CONFERENCE_WILL_JOIN";
		    break;
		case "onConferenceTerminated":
			m = "CONFERENCE_TERMINATED";
	        break;
		case "onConferenceFinished":
		    m = "CONFERENCE_FINISHED";
		    break;
		case "onConferenceDestroyed":
		    m = "CONFERENCE_DESTROYED";
		    break;
    }

    if (!m.equals("")) {
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, m);
      pluginResult.setKeepCallback(true);
      _callback.sendPluginResult(pluginResult);
    }
  }
}
