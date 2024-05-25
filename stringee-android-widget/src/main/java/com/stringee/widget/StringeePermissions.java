package com.stringee.widget;

import android.app.Activity;
import android.os.Build;

/**
 * Created by sunil on 22/1/16.
 */
public class StringeePermissions {
    private Activity activity;

    public StringeePermissions(Activity activity) {
        this.activity = activity;
    }

    public void requestVideoCallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSION_VIDEO_CALL_ANDROID_13, PermissionsUtils.REQUEST_VIDEO_CALL);
        } else {
            PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSION_VIDEO_CALL, PermissionsUtils.REQUEST_VIDEO_CALL);
        }
    }

    public void requestVoiceCallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSION_VOICE_CALL_ANDROID_13, PermissionsUtils.REQUEST_VIDEO_CALL);
        } else {
            PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSION_VOICE_CALL, PermissionsUtils.REQUEST_VOICE_CALL);
        }
    }
}
