package com.stringee.widget;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.common.StringeeAudioManager;
import com.stringee.listener.StatusListener;

import org.webrtc.RendererCommon;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class IncomingCallActivity extends Activity implements View.OnClickListener {
    private TextView tvName;
    private TextView tvState;

    private View vRejectAnswer;
    private ImageButton btnMute;
    private ImageButton btnCamera;
    private ImageButton btnEndCall;
    private ImageButton btnSpeaker;
    private ImageView imNetwork;
    private FrameLayout vLocal;
    private FrameLayout vRemote;
    private View vControl;
    private LinearLayout vStatus;
    private RelativeLayout rootView;

    private String name;
    private boolean isMute;
    private boolean isVideoOn;
    private boolean isVideoCall;
    private boolean canSwitch = true;
    private boolean isFrontCamera = true;
    private boolean isSpeaker;
    private boolean isAnswer;

    private long startTime = 0;
    private TimerTask timerTask;
    private Timer timer;
    private Timer statsTimer;
    private TimerTask statsTimerTask;

    private StringeeAudioManager audioManager;
    private StringeeCall incomingCall;
    private StringeeCall2 incomingCall2;

    private double mPrevCallTimestamp = 0;
    private long mPrevCallBytes = 0;
    private long mCallBw = 0;
    private short mState = StringeeCall.SignalingState.RINGING.getValue();
    private short mState2 = StringeeCall2.SignalingState.RINGING.getValue();
    private short mMediaState = StringeeCall.MediaState.DISCONNECTED.getValue();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String callId = extras.getString(Constant.PARAM_CALL_ID);
            isAnswer = extras.getBoolean(Constant.PARAM_ANSWER);

            incomingCall = StringeeWidget.callMap.get(callId);
            if (incomingCall != null) {
                isVideoCall = incomingCall.isVideoCall();
                name = incomingCall.getFromAlias();
                if (name == null) {
                    name = incomingCall.getFrom();
                }
            }

            incomingCall2 = StringeeWidget.callMap2.get(callId);
            if (incomingCall2 != null) {
                isVideoCall = incomingCall2.isVideoCall();
                name = incomingCall2.getFromAlias();
                if (name == null) {
                    name = incomingCall2.getFrom();
                }
            }
        }

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(Constant.INCOMING_CALL_NOTIFICATION_ID);

        if (incomingCall == null && incomingCall2 == null) {
            finish();
            return;
        }

        isVideoOn = isVideoCall;
        isSpeaker = isVideoCall;
        if (isVideoCall) {
            setContentView(R.layout.stringee_activity_video_call);
        } else {
            setContentView(R.layout.stringee_activity_voice_call);
        }

        initView();

        registerEvents();

        if (isVideoCall) {
            if (!PermissionsUtils.isVideoCallPermissionGranted(this)) {
                new StringeePermissions(this).requestVideoCallPermission();
                return;
            }
        } else {
            if (!PermissionsUtils.isVoiceCallPermissionGranted(this)) {
                new StringeePermissions(this).requestVoiceCallPermission();
                return;
            }
        }

        answer();
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGranted = false;
        if (grantResults.length > 0) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    isGranted = false;
                    break;
                } else {
                    isGranted = true;
                }
            }
        }
        switch (requestCode) {
            case PermissionsUtils.REQUEST_VIDEO_CALL:
            case PermissionsUtils.REQUEST_VOICE_CALL:
                if (!isGranted) {
                    Utils.reportMessage(this, R.string.stringee_recording_required);
                    endCall(true);
                    return;
                } else {
                    answer();
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_answer) {
            startCall();
        } else if (id == R.id.btn_reject) {
            tvState.setText(R.string.stringee_call_ended);
            endCall(true);
        } else if (id == R.id.btn_end) {
            tvState.setText(R.string.stringee_call_ended);
            endCall(true);
        } else if (id == R.id.btn_mute) {
            isMute = !isMute;
            btnMute.setBackgroundResource(isMute ? R.drawable.stringee_btn_ic_selector : R.drawable.stringee_btn_ic_selected_selector);
            btnMute.setImageResource(isMute ? R.drawable.stringee_ic_mic_off_black : R.drawable.stringee_ic_mic_on_white);
            if (incomingCall != null) {
                incomingCall.mute(isMute);
            }

            if (incomingCall2 != null) {
                incomingCall2.mute(isMute);
            }
        } else if (id == R.id.btn_switch) {
            if (incomingCall != null) {
                if (!canSwitch) {
                    return;
                }
                canSwitch = false;
                try {
                    incomingCall.switchCamera(new StatusListener() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    canSwitch = true;
                                    isFrontCamera = !isFrontCamera;
                                    if (incomingCall != null) {
                                        try {
                                            incomingCall.getLocalView().setMirror(isFrontCamera);
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                }
                            });
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            if (incomingCall2 != null) {
                if (!canSwitch) {
                    return;
                }
                canSwitch = false;
                try {
                    incomingCall2.switchCamera(new StatusListener() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    canSwitch = true;
                                    isFrontCamera = !isFrontCamera;
                                    if (incomingCall2 != null) {
                                        try {
                                            incomingCall2.getLocalView().setMirror(isFrontCamera);
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }
                                    }
                                }
                            });
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else if (id == R.id.btn_camera) {
            isVideoOn = !isVideoOn;
            vLocal.setVisibility(isVideoOn ? View.VISIBLE : View.GONE);
            btnCamera.setBackgroundResource(isVideoOn ? R.drawable.stringee_btn_ic_selected_selector : R.drawable.stringee_btn_ic_selector);
            btnCamera.setImageResource(isVideoOn ? R.drawable.stringee_ic_cam_on_white : R.drawable.stringee_ic_cam_off_black);
            if (incomingCall != null) {
                incomingCall.enableVideo(isVideoOn);
            }

            if (incomingCall2 != null) {
                incomingCall2.enableVideo(isVideoOn);
            }
        } else if (id == R.id.btn_speaker) {
            isSpeaker = !isSpeaker;
            btnSpeaker.setBackgroundResource(isSpeaker ? R.drawable.stringee_btn_ic_selector : R.drawable.stringee_btn_ic_selected_selector);
            btnSpeaker.setImageResource(isSpeaker ? R.drawable.stringee_ic_speaker_on_black : R.drawable.stringee_ic_speaker_off_white);
            if (audioManager != null) {
                audioManager.setSpeakerphoneOn(isSpeaker);
            }
        }
    }

    private void initView() {
        rootView = findViewById(R.id.v_root);
        tvName = findViewById(R.id.tv_name);
        tvState = findViewById(R.id.tv_status);
        tvState.setText(R.string.stringee_ringing);
        btnMute = findViewById(R.id.btn_mute);
        btnMute.setOnClickListener(this);
        btnEndCall = findViewById(R.id.btn_end);
        btnEndCall.setOnClickListener(this);
        ImageButton btnAnswer = findViewById(R.id.btn_answer);
        btnAnswer.setOnClickListener(this);
        ImageButton btnReject = findViewById(R.id.btn_reject);
        btnReject.setOnClickListener(this);
        imNetwork = findViewById(R.id.im_network);
        vRejectAnswer = findViewById(R.id.v_reject_answer);
        vControl = findViewById(R.id.v_control);
        vStatus = findViewById(R.id.v_status);
        if (isVideoCall) {
            vLocal = findViewById(R.id.v_local);
            vRemote = findViewById(R.id.v_remote);
            ImageButton btnSwitch = findViewById(R.id.btn_switch);
            btnSwitch.setOnClickListener(this);
            btnCamera = findViewById(R.id.btn_camera);
            btnCamera.setOnClickListener(this);
        } else {
            btnSpeaker = findViewById(R.id.btn_speaker);
            btnSpeaker.setOnClickListener(this);
        }

        if (name != null) {
            tvName.setText(name);
        }

        vControl.setVisibility(View.GONE);
        vRejectAnswer.setVisibility(View.VISIBLE);
    }

    private void answer() {
        if (isAnswer) {
            vControl.setVisibility(View.VISIBLE);
            vRejectAnswer.setVisibility(View.GONE);
            Utils.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startCall();
                }
            }, 500);
        }
    }

    private void startCall() {
        RingtoneUtils.getInstance(this).stopRinging();
        audioManager = StringeeAudioManager.create(this);
        audioManager.setSpeakerphoneOn(isVideoCall);
        audioManager.start(new StringeeAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(StringeeAudioManager.AudioDevice audioDevice, Set<StringeeAudioManager.AudioDevice> set) {
                if (audioManager == null) {
                    return;
                }
                if (isVideoCall) {
                    switch (audioDevice) {
                        case WIRED_HEADSET:
                            audioManager.setSpeakerphoneOn(false);
                            break;
                        case BLUETOOTH:
                            audioManager.setBluetoothScoOn(true);
                            break;
                        case EARPIECE:
                        case SPEAKER_PHONE:
                            audioManager.setSpeakerphoneOn(true);
                            break;
                    }
                } else {
                    switch (audioDevice) {
                        case WIRED_HEADSET:
                        case BLUETOOTH:
                            if (audioDevice == StringeeAudioManager.AudioDevice.BLUETOOTH) {
                                audioManager.setBluetoothScoOn(true);
                            } else {
                                audioManager.setSpeakerphoneOn(false);
                            }
                            btnSpeaker.setBackgroundResource(R.drawable.stringee_btn_ic_selected_selector);
                            btnSpeaker.setImageResource(audioDevice == StringeeAudioManager.AudioDevice.BLUETOOTH ? R.drawable.stringee_ic_bluetooth : R.drawable.stringee_ic_speaker_off_white);
                            btnSpeaker.setClickable(false);
                            break;
                        case EARPIECE:
                        case SPEAKER_PHONE:
                            btnSpeaker.setBackgroundResource(isSpeaker ? R.drawable.stringee_btn_ic_selector : R.drawable.stringee_btn_ic_selected_selector);
                            btnSpeaker.setImageResource(isSpeaker ? R.drawable.stringee_ic_speaker_on_black : R.drawable.stringee_ic_speaker_off_white);
                            btnSpeaker.setClickable(true);
                            audioManager.setSpeakerphoneOn(isSpeaker);
                            break;
                    }
                }
            }
        });


        vControl.setVisibility(View.VISIBLE);
        vRejectAnswer.setVisibility(View.GONE);
        imNetwork.setVisibility(View.VISIBLE);

        if (incomingCall != null) {
            incomingCall.answer(null);
        }
        if (incomingCall2 != null) {
            incomingCall2.answer(null);
        }
    }

    private void endCall(boolean isHangup) {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        imNetwork.setVisibility(View.GONE);
        vControl.setVisibility(View.GONE);
        vRejectAnswer.setVisibility(View.GONE);

        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
        RingtoneUtils.getInstance(this).stopRinging();
        RingtoneUtils.getInstance(this).playEndCallSound();

        startTime = 0;

        if (timer != null) {
            timer.cancel();
        }
        if (statsTimer != null) {
            statsTimer.cancel();
        }

        unregisterEvents();

        if (incomingCall != null && isHangup) {
            incomingCall.hangup(null);
        }
        incomingCall = null;

        if (incomingCall2 != null && isHangup) {
            incomingCall2.hangup(null);
        }
        incomingCall2 = null;

        finish();
        StringeeWidget.getInstance(this).setInCall(false);
    }

    private void checkCallStats(StringeeCall.StringeeCallStats stats) {
        if (incomingCall == null) {
            return;
        }
        double videoTimestamp = stats.timeStamp / 1000d;
        long bytesReceived = incomingCall.isVideoCall() ? (long) stats.videoBytesReceived : (long) stats.callBytesReceived;
        //initialize values
        if (mPrevCallTimestamp == 0) {
            mPrevCallTimestamp = videoTimestamp;
            mPrevCallBytes = bytesReceived;
        } else {
            //calculate video bandwidth
            mCallBw = (long) ((8 * (bytesReceived - mPrevCallBytes)) / (videoTimestamp - mPrevCallTimestamp));
            mPrevCallTimestamp = videoTimestamp;
            mPrevCallBytes = bytesReceived;

            checkNetworkQuality();
        }
    }

    private void checkCallStats2(StringeeCall2.StringeeCallStats stats) {
        if (incomingCall2 == null) {
            return;
        }
        double videoTimestamp = stats.timeStamp / 1000d;
        long bytesReceived = incomingCall2.isVideoCall() ? (long) stats.videoBytesReceived : (long) stats.callBytesReceived;
        //initialize values
        if (mPrevCallTimestamp == 0) {
            mPrevCallTimestamp = videoTimestamp;
            mPrevCallBytes = bytesReceived;
        } else {
            //calculate video bandwidth
            mCallBw = (long) ((8 * (bytesReceived - mPrevCallBytes)) / (videoTimestamp - mPrevCallTimestamp));
            mPrevCallTimestamp = videoTimestamp;
            mPrevCallBytes = bytesReceived;

            checkNetworkQuality();
        }
    }

    private void checkNetworkQuality() {
        if (mCallBw <= 0) {
            imNetwork.setImageResource(R.drawable.stringee_signal_no_connect);
        } else {
            if (mCallBw < 15000) {
                imNetwork.setImageResource(R.drawable.stringee_signal_poor);
            } else {
                if (mCallBw >= 35000) {
                    imNetwork.setImageResource(R.drawable.stringee_signal_exellent);
                } else {
                    if (mCallBw <= 25000) {
                        imNetwork.setImageResource(R.drawable.stringee_signal_average);
                    } else {
                        imNetwork.setImageResource(R.drawable.stringee_signal_good);
                    }
                }
            }
        }
    }

    private void callStarted() {
        if (startTime > 0) {
            return;
        }
        tvState.setText(R.string.stringee_call_started);
        startTime = System.currentTimeMillis();

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> tvState.setText(DateTimeUtils.getCallTime(System.currentTimeMillis(), startTime)));
            }
        };
        timer.schedule(timerTask, 0, 1000);

        statsTimer = new Timer();
        statsTimerTask = new TimerTask() {
            @Override
            public void run() {
                incomingCall.getStats(statsReport -> runOnUiThread(() -> checkCallStats(statsReport)));
            }
        };
        statsTimer.schedule(statsTimerTask, 0, 3000);

        if (isVideoCall) {
            RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.setMargins(Utils.dpToPx(IncomingCallActivity.this, 20), Utils.dpToPx(IncomingCallActivity.this, 20), Utils.dpToPx(IncomingCallActivity.this, 20), Utils.dpToPx(IncomingCallActivity.this, 20));
            vStatus.setLayoutParams(p);
            vStatus.setGravity(Gravity.LEFT);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) vLocal.getLayoutParams();
            params.width = Utils.dpToPx(IncomingCallActivity.this, 100);
            params.height = Utils.dpToPx(IncomingCallActivity.this, 150);
            params.setMargins(0, Utils.dpToPx(IncomingCallActivity.this, 20), Utils.dpToPx(IncomingCallActivity.this, 20), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.CENTER_IN_PARENT);
            }
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            vLocal.setLayoutParams(params);
            vLocal.removeAllViews();
            incomingCall.getLocalView().setMirror(true);
            vLocal.addView(incomingCall.getLocalView());
            incomingCall.renderLocalView(true);


            rootView.setOnClickListener(view -> {
                if (vControl.getVisibility() == View.VISIBLE) {
                    vControl.setVisibility(View.INVISIBLE);
                } else {
                    vControl.setVisibility(View.VISIBLE);
                }
            });
            try {
                vRemote.removeAllViews();
                vRemote.addView(incomingCall.getRemoteView());
                incomingCall.renderRemoteView(false, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void callStarted2() {
        if (startTime > 0) {
            return;
        }
        tvState.setText(R.string.stringee_call_started);
        startTime = System.currentTimeMillis();

        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> tvState.setText(DateTimeUtils.getCallTime(System.currentTimeMillis(), startTime)));
            }
        };
        timer.schedule(timerTask, 0, 1000);

        statsTimer = new Timer();
        statsTimerTask = new TimerTask() {
            @Override
            public void run() {
                incomingCall2.getStats(statsReport -> runOnUiThread(() -> checkCallStats2(statsReport)));
            }
        };
        statsTimer.schedule(statsTimerTask, 0, 3000);

        if (isVideoCall) {
            RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            p.setMargins(Utils.dpToPx(IncomingCallActivity.this, 20), Utils.dpToPx(IncomingCallActivity.this, 20), Utils.dpToPx(IncomingCallActivity.this, 20), Utils.dpToPx(IncomingCallActivity.this, 20));
            vStatus.setLayoutParams(p);
            vStatus.setGravity(Gravity.LEFT);

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) vLocal.getLayoutParams();
            params.width = Utils.dpToPx(IncomingCallActivity.this, 100);
            params.height = Utils.dpToPx(IncomingCallActivity.this, 150);
            params.setMargins(0, Utils.dpToPx(IncomingCallActivity.this, 20), Utils.dpToPx(IncomingCallActivity.this, 20), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                params.removeRule(RelativeLayout.CENTER_IN_PARENT);
            }
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            vLocal.setLayoutParams(params);
            vLocal.removeAllViews();
            incomingCall2.getLocalView().setMirror(true);
            vLocal.addView(incomingCall2.getLocalView());
            incomingCall2.renderLocalView(true);


            rootView.setOnClickListener(view -> {
                if (vControl.getVisibility() == View.VISIBLE) {
                    vControl.setVisibility(View.INVISIBLE);
                } else {
                    vControl.setVisibility(View.VISIBLE);
                }
            });
            try {
                vRemote.removeAllViews();
                vRemote.addView(incomingCall2.getRemoteView());
                incomingCall2.renderRemoteView(false, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void registerEvents() {
        EventReceiver signalStateObserver = new EventReceiver() {
            @Override
            public void onReceive(Intent intent) {
                String callId = intent.getStringExtra(Constant.PARAM_CALL_ID);
                short state = intent.getShortExtra(Constant.PARAM_CALL_SIGNAL_STATE, (short) 0);
                mState = state;
                if (callId != null && incomingCall != null && callId.equals(incomingCall.getCallId())) {
                    if (state == StringeeCall.SignalingState.ENDED.getValue()) {
                        tvState.setText(R.string.stringee_call_ended);
                        endCall(false);

                    }
                }
            }
        };
        EventManager.getInstance().registerEvent(signalStateObserver, Notify.CALL_SIGNAL_CHANGE.getValue());

        EventReceiver mediaStateObserver = new EventReceiver() {
            @Override
            public void onReceive(Intent intent) {
                String callId = intent.getStringExtra(Constant.PARAM_CALL_ID);
                if (callId != null && incomingCall != null && callId.equals(incomingCall.getCallId())) {
                    mMediaState = intent.getShortExtra(Constant.PARAM_CALL_MEDIA_STATE, (short) 0);
                    if (mMediaState == StringeeCall.MediaState.CONNECTED.getValue()) {
                        if (mState == StringeeCall.SignalingState.ANSWERED.getValue()) {
                            callStarted();
                        }
                    }
                }
            }
        };
        EventManager.getInstance().registerEvent(mediaStateObserver, Notify.CALL_MEDIA_CHANGE.getValue());

        EventReceiver signalStateObserver2 = new EventReceiver() {
            @Override
            public void onReceive(Intent intent) {
                String callId = intent.getStringExtra(Constant.PARAM_CALL_ID);
                short state = intent.getShortExtra(Constant.PARAM_CALL_SIGNAL_STATE, (short) 0);
                mState2 = state;
                if (callId != null && incomingCall2 != null && callId.equals(incomingCall2.getCallId())) {
                    if (state == StringeeCall2.SignalingState.ENDED.getValue()) {
                        tvState.setText(R.string.stringee_call_ended);
                        endCall(false);
                    }
                }
            }
        };
        EventManager.getInstance().registerEvent(signalStateObserver2, Notify.CALL_SIGNAL_CHANGE2.getValue());

        EventReceiver mediaStateObserver2 = new EventReceiver() {
            @Override
            public void onReceive(Intent intent) {
                String callId = intent.getStringExtra(Constant.PARAM_CALL_ID);
                if (callId != null && incomingCall2 != null && callId.equals(incomingCall2.getCallId())) {
                    short mediaState = intent.getShortExtra(Constant.PARAM_CALL_MEDIA_STATE, (short) 0);
                    if (mediaState == StringeeCall2.MediaState.CONNECTED.getValue()) {
                        if (mState2 == StringeeCall2.SignalingState.ANSWERED.getValue()) {
                            callStarted2();
                        }
                    }
                }
            }
        };
        EventManager.getInstance().registerEvent(mediaStateObserver2, Notify.CALL_MEDIA_CHANGE2.getValue());

        EventReceiver handleOnOtherDeviceReceiver = new EventReceiver() {
            @Override
            public void onReceive(Intent intent) {
                String callId = intent.getStringExtra(Constant.PARAM_CALL_ID);
                if (callId != null && ((incomingCall2 != null && callId.equals(incomingCall2.getCallId())) || (incomingCall != null && incomingCall.getCallId().equals(callId)))) {
                    Utils.reportMessage(IncomingCallActivity.this, R.string.msg_handled_on_another_device);
                    endCall(false);
                }
            }
        };
        EventManager.getInstance().registerEvent(handleOnOtherDeviceReceiver, Notify.CALL_HANDLED_ON_OTHER_DEVICE.getValue());
    }

    private void unregisterEvents() {
        EventManager.getInstance().unregisterEvent(Notify.CALL_SIGNAL_CHANGE.getValue());
        EventManager.getInstance().unregisterEvent(Notify.CALL_MEDIA_CHANGE.getValue());
        EventManager.getInstance().unregisterEvent(Notify.CALL_SIGNAL_CHANGE2.getValue());
        EventManager.getInstance().unregisterEvent(Notify.CALL_MEDIA_CHANGE2.getValue());
    }
}