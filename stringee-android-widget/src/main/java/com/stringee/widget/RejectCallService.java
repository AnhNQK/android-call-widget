package com.stringee.widget;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;

import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;

public class RejectCallService extends IntentService {

    public RejectCallService() {
        super("com.stringee.widget.service.call.reject");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String callId = intent.getStringExtra(Constant.PARAM_CALL_ID);
        StringeeCall stringeeCall = StringeeWidget.callMap.get(callId);
        if (stringeeCall != null) {
            stringeeCall.reject(null);
        }
        StringeeCall2 stringeeCall2 = StringeeWidget.callMap2.get(callId);
        if (stringeeCall2 != null) {
            stringeeCall2.reject(null);
        }

        RingtoneUtils.getInstance(getApplicationContext()).stopRinging();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(Constant.INCOMING_CALL_NOTIFICATION_ID);
        StringeeWidget.getInstance(this).setInCall(false);
    }
}