package com.stringee.widget;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.call.StringeeCall2.MediaState;
import com.stringee.call.StringeeCall2.SignalingState;
import com.stringee.common.StringeeAudioManager;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.video.StringeeVideoTrack;
import com.stringee.widget.CallConfig.CameraFacing;
import com.stringee.widget.R.id;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class OutgoingCallActivity extends Activity implements View.OnClickListener {
    private TextView tvName;
    private TextView tvStatus;
    private ImageButton btnMute;
    private ImageButton btnCamera;
    private ImageButton btnSpeaker;
    private ImageButton btnEndcall;
    private ImageView imNetwork;
    private FrameLayout vLocal;
    private FrameLayout vRemote;
    private ScannerOverlayView vScanner;
    private View vControl;
    private LinearLayout vStatus;
    private RelativeLayout rootView;

    private boolean isMute;
    private boolean isVideoOn;
    private boolean isEnded;
    private boolean isFrontCamera = true;
    private boolean isSpeaker;

    private long startTime = 0;
    private TimerTask timerTask;
    private Timer timer;
    private Timer statsTimer;
    private TimerTask statsTimerTask;

    private CallConfig callConfig;
    private StringeeCall outgoingCall;
    private StringeeCall2 outgoingCall2;
    private StringeeAudioManager audioManager;

    private double mPrevCallTimestamp = 0;
    private long mPrevCallBytes = 0;
    private long mCallBw = 0;

    private SignalingState mSignalingState2 = SignalingState.CALLING;
    private MediaState mMediaState2 = MediaState.DISCONNECTED;
    private StringeeCall.SignalingState mSignalingState = StringeeCall.SignalingState.CALLING;
    private StringeeCall.MediaState mMediaState = StringeeCall.MediaState.DISCONNECTED;
    private int requestId;
    private boolean canSwitch = true;
    private boolean isVideoCall;
    String frontCameraName = "";
    String rearCameraName = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        callConfig = (CallConfig) getIntent().getSerializableExtra(Constant.PARAM_CALL_CONFIG);
        requestId = getIntent().getIntExtra(Constant.PARAM_REQUEST_ID, 0);

        isVideoCall = callConfig.isVideoCall();

        if (callConfig != null && isVideoCall) {
            if (!Utils.isTextEmpty(callConfig.getCustomData()) && !Utils.isTextEmpty(callConfig.getTo())) {
                try {
                    JSONObject customDataObj = new JSONObject(callConfig.getCustomData());
                    String company = customDataObj.optString("company");
                    if (company.equals("Viettel") && callConfig.getTo().equals("callbot")) {
                        ScanViewConfig scanViewConfig = new ScanViewConfig();
                        scanViewConfig.setWidthRatio(0.8f);
                        scanViewConfig.setAspectRatio(1.5f);
                        scanViewConfig.setBorderStrokeWidth(2f);
                        scanViewConfig.setBorderStrokeColor(Color.parseColor("#e83054"));
                        scanViewConfig.setBorderType(ScannerOverlayView.BorderType.OVAL);
                        scanViewConfig.setDashLength(10f);
                        callConfig.useScannerView(scanViewConfig);
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (isVideoCall) {
            setContentView(R.layout.stringee_activity_video_call);
        } else {
            setContentView(R.layout.stringee_activity_voice_call);
        }

        isVideoOn = isVideoCall;
        isSpeaker = isVideoCall;
        initViews();

        if (callConfig.getCameraFacing() != CameraFacing.BOTH) {
            getCameraName();
        }

        // Check permission
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

        if (isVideoCall) {
            startCall2();
        } else {
            startCall();
        }
    }

    private void getCameraName() {
        // get cameraId
        CameraEnumerator enumerator = Camera2Enumerator.isSupported(this) ? new Camera2Enumerator(this) : new Camera1Enumerator();
        String[] cameraNames = enumerator.getDeviceNames();
        if (cameraNames.length > 0) {
            // first front id is main front camera
            for (String name : cameraNames) {
                boolean isFrontFace = enumerator.isFrontFacing(name);
                if (isFrontFace) {
                    frontCameraName = name;
                    break;
                }
            }

            // first rear id is main rear camera
            for (String cameraName : cameraNames) {
                boolean isBackFace = enumerator.isBackFacing(cameraName);
                if (isBackFace) {
                    rearCameraName = cameraName;
                    break;
                }
            }
        }

        canSwitch = (!Utils.isTextEmpty(frontCameraName) && !Utils.isTextEmpty(rearCameraName));
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
                    finish();
                    StringeeWidget.getInstance(this).setInCall(false);
                    return;
                } else {
                    if (isVideoCall) {
                        startCall2();
                    } else {
                        startCall();
                    }
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
    }

    private void initViews() {
        rootView = findViewById(R.id.v_root);
        imNetwork = findViewById(R.id.im_network);
        tvName = findViewById(R.id.tv_name);
        tvStatus = findViewById(R.id.tv_status);
        btnMute = findViewById(R.id.btn_mute);
        btnMute.setOnClickListener(this);
        btnEndcall = findViewById(R.id.btn_end);
        btnEndcall.setOnClickListener(this);
        vControl = findViewById(R.id.v_control);
        vStatus = findViewById(R.id.v_status);
        if (callConfig.isVideoCall()) {
            vLocal = findViewById(R.id.v_local);
            vScanner = findViewById(R.id.v_scanner);
            if (callConfig.isUseScannerView()) {
                ScanViewConfig scanViewConfig = callConfig.getScannerViewOption();
                vScanner.setColor(scanViewConfig.getColor());
                vScanner.setRadius(scanViewConfig.getRadius());
                vScanner.setWidthRatio(scanViewConfig.getWidthRatio());
                vScanner.setAspectRatio(scanViewConfig.getAspectRatio());
                vScanner.setBorderStrokeWidth(scanViewConfig.getBorderStrokeWidth());
                vScanner.setBorderStrokeColor(scanViewConfig.getBorderStrokeColor());
                vScanner.setBorderType(scanViewConfig.getBorderType());
                vScanner.setDashLength(scanViewConfig.getDashLength());
            }
            vRemote = findViewById(R.id.v_remote);
            ImageButton btnSwitch = findViewById(R.id.btn_switch);
            btnSwitch.setOnClickListener(this);
            if (callConfig.getCameraFacing() != CameraFacing.BOTH) {
                findViewById(id.v_btn_switch).setVisibility(View.GONE);
            }
            btnCamera = findViewById(R.id.btn_camera);
            btnCamera.setOnClickListener(this);
        } else {
            btnSpeaker = findViewById(R.id.btn_speaker);
            btnSpeaker.setOnClickListener(this);
        }

        String name = callConfig.getTo();
        String toAlias = callConfig.getToAlias();
        if (toAlias != null && toAlias.trim().length() > 0) {
            name = toAlias;
        }
        tvName.setText(name);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_mute) {
            isMute = !isMute;
            btnMute.setBackgroundResource(isMute ? R.drawable.stringee_btn_ic_selector : R.drawable.stringee_btn_ic_selected_selector);
            btnMute.setImageResource(isMute ? R.drawable.stringee_ic_mic_off_black : R.drawable.stringee_ic_mic_on_white);
            if (outgoingCall != null) {
                outgoingCall.mute(isMute);
            }
            if (outgoingCall2 != null) {
                outgoingCall2.mute(isMute);
            }
        } else if (id == R.id.btn_end) {
            isEnded = true;
            tvStatus.setText(R.string.stringee_call_ended);
            StringeeListener listener = StringeeWidget.getInstance(this).getListener();
            if (listener != null) {
                listener.onCallStateChange2(outgoingCall2, SignalingState.ENDED);
            }
            endCall(true);
        } else if (id == R.id.btn_switch) {
            if (outgoingCall2 != null) {
                if (!canSwitch) {
                    return;
                }
                canSwitch = false;
                try {
                    outgoingCall2.switchCamera(new StatusListener() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    canSwitch = true;
                                    isFrontCamera = !isFrontCamera;
                                    if (outgoingCall2 != null) {
                                        try {
                                            outgoingCall2.getLocalView().setMirror(isFrontCamera);
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
            if (outgoingCall2 != null) {
                isVideoOn = !isVideoOn;
                vLocal.setVisibility(isVideoOn ? View.VISIBLE : View.GONE);
                if (callConfig.isUseScannerView()) {
                    vScanner.setVisibility(isVideoOn ? View.VISIBLE : View.GONE);
                }
                btnCamera.setBackgroundResource(isVideoOn ? R.drawable.stringee_btn_ic_selected_selector : R.drawable.stringee_btn_ic_selector);
                btnCamera.setImageResource(isVideoOn ? R.drawable.stringee_ic_cam_on_white : R.drawable.stringee_ic_cam_off_black);
                outgoingCall2.enableVideo(isVideoOn);
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

    private void startCall2() {
        if (isEnded) {
            return;
        }
        audioManager = StringeeAudioManager.create(this);
        audioManager.setSpeakerphoneOn(callConfig.isVideoCall());
        audioManager.start(new StringeeAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(StringeeAudioManager.AudioDevice audioDevice, Set<StringeeAudioManager.AudioDevice> set) {
                if (audioManager == null) {
                    return;
                }
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
            }
        });

        String from = StringeeWidget.getInstance(this).getClient().getUserId();
        String callFrom = callConfig.getFrom();
        if (callFrom != null && callFrom.trim().length() > 0) {
            from = callFrom;
        }
        String to = callConfig.getTo();
        outgoingCall2 = new StringeeCall2(StringeeWidget.getInstance(this).getClient(), from, to);
        outgoingCall2.setVideoCall(callConfig.isVideoCall());

        outgoingCall2.setCallListener(new StringeeCall2.StringeeCallListener() {
            @Override
            public void onSignalingStateChange(StringeeCall2 stringeeCall, final SignalingState signalingState, String reason, int sipCode, String sipReason) {
                Log.d("Stringee", "onSignalingStateChange: " + signalingState.toString());
                runOnUiThread(() -> {
                    mSignalingState2 = signalingState;
                    StringeeListener listener = StringeeWidget.getInstance(OutgoingCallActivity.this).getListener();
                    switch (mSignalingState2) {
                        case CALLING:
                            tvStatus.setText(R.string.stringee_calling);
                            StatusListener statusListener = StringeeWidget.statusListenerMap.get(requestId);
                            if (statusListener != null) {
                                statusListener.onSuccess();
                            }
                            if (listener != null) {
                                listener.onCallStateChange2(stringeeCall, SignalingState.CALLING);
                            }
                            RingtoneUtils.getInstance(OutgoingCallActivity.this).playWaitingSound();
                            break;
                        case RINGING:
                            tvStatus.setText(R.string.stringee_ringing);
                            if (listener != null) {
                                listener.onCallStateChange2(stringeeCall, SignalingState.RINGING);
                            }
                            break;
                        case ANSWERED:
                            if (listener != null) {
                                listener.onCallStateChange2(stringeeCall, SignalingState.ANSWERED);
                            }
                            tvStatus.setText(R.string.stringee_call_starting);
                            if (mMediaState2 == MediaState.CONNECTED) {
                                callStarted2();
                            }
                            break;
                        case BUSY:
                            if (listener != null) {
                                listener.onCallStateChange2(stringeeCall, SignalingState.BUSY);
                            }
                            tvStatus.setText(R.string.stringee_busy);
                            endCall(false);
                            break;
                        case ENDED:
                            tvStatus.setText(R.string.stringee_call_ended);
                            if (listener != null) {
                                listener.onCallStateChange2(stringeeCall, SignalingState.ENDED);
                            }
                            endCall(false);
                            break;
                    }
                });
            }

            @Override
            public void onError(final StringeeCall2 stringeeCall, int i, final String s) {
                runOnUiThread(() -> {
                    Utils.reportMessage(OutgoingCallActivity.this, s);
                    StatusListener statusListener = StringeeWidget.statusListenerMap.get(requestId);
                    if (statusListener != null) {
                        statusListener.onError(new StringeeError(i, s));
                    }
                    endCall(false);
                });
            }

            @Override
            public void onHandledOnAnotherDevice(StringeeCall2 stringeeCall, final SignalingState signalingState, String desc) {
                runOnUiThread(() -> {
                    switch (signalingState) {
                        case RINGING:
                            break;
                        case BUSY:
                            Utils.reportMessage(OutgoingCallActivity.this, R.string.stringee_another_device_rejected);
                            endCall(false);
                            break;
                        case ENDED:
                            Utils.reportMessage(OutgoingCallActivity.this, R.string.stringee_another_device_ended);
                            endCall(false);
                            break;
                    }
                });
            }

            @Override
            public void onMediaStateChange(StringeeCall2 stringeeCall, final MediaState mediaState) {
                runOnUiThread(() -> {
                    mMediaState2 = mediaState;
                    if (mediaState == MediaState.CONNECTED) {
                        RingtoneUtils.getInstance(OutgoingCallActivity.this).stopWaitingSound();
                        if (mSignalingState2 == SignalingState.ANSWERED) {
                            callStarted2();
                        }
                    }
                });
            }

            @Override
            public void onLocalStream(final StringeeCall2 stringeeCall) {
                if (stringeeCall.isVideoCall()) {
                    runOnUiThread(() -> {
                        try {
                            if (callConfig.getCameraFacing() != CameraFacing.BOTH && canSwitch) {
                                stringeeCall.switchCamera(new StatusListener() {
                                    @Override
                                    public void onSuccess() {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    outgoingCall2.getLocalView().setMirror(callConfig.getCameraFacing() == CameraFacing.FRONT);
                                                } catch (Exception ex) {
                                                    ex.printStackTrace();
                                                }
                                            }
                                        });
                                    }
                                }, callConfig.getCameraFacing() == CameraFacing.FRONT ? frontCameraName : rearCameraName);
                            }
                            rootView.setBackgroundColor(Color.BLACK);
                            SurfaceViewRenderer view = stringeeCall.getLocalView();
                            view.setMirror(true);
                            vLocal.removeAllViews();
                            vLocal.addView(view);
                            stringeeCall.renderLocalView(true, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                            if (callConfig.isUseScannerView()) {
                                vScanner.setVisibility(isVideoOn ? View.VISIBLE : View.GONE);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            }

            @Override
            public void onRemoteStream(final StringeeCall2 stringeeCall) {
                if (stringeeCall.isVideoCall()) {
                    runOnUiThread(() -> {
                        try {
                            LayoutParams p = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            p.setMargins(Utils.dpToPx(OutgoingCallActivity.this, 20), Utils.dpToPx(OutgoingCallActivity.this, 20), Utils.dpToPx(OutgoingCallActivity.this, 20), Utils.dpToPx(OutgoingCallActivity.this, 20));
                            vStatus.setLayoutParams(p);
                            vStatus.setGravity(Gravity.LEFT);

                            LayoutParams params = (LayoutParams) vLocal.getLayoutParams();
                            params.width = Utils.dpToPx(OutgoingCallActivity.this, 100);
                            params.height = Utils.dpToPx(OutgoingCallActivity.this, 150);
                            params.setMargins(0, Utils.dpToPx(OutgoingCallActivity.this, 20), Utils.dpToPx(OutgoingCallActivity.this, 20), 0);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                                params.removeRule(RelativeLayout.CENTER_IN_PARENT);
                            }
                            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                            vLocal.setLayoutParams(params);
                            if (callConfig.isUseScannerView()) {
                                vScanner.setLayoutParams(params);
                            }
                            rootView.setOnClickListener(view -> {
                                if (vControl.getVisibility() == View.VISIBLE) {
                                    vControl.setVisibility(View.INVISIBLE);
                                } else {
                                    vControl.setVisibility(View.VISIBLE);
                                }
                            });
                            vRemote.removeAllViews();
                            vRemote.addView(stringeeCall.getRemoteView());
                            stringeeCall.renderRemoteView(false, RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            }

            @Override
            public void onVideoTrackAdded(StringeeVideoTrack stringeeVideoTrack) {

            }

            @Override
            public void onVideoTrackRemoved(StringeeVideoTrack stringeeVideoTrack) {

            }

            @Override
            public void onCallInfo(StringeeCall2 stringeeCall, final JSONObject jsonObject) {
            }

            @Override
            public void onTrackMediaStateChange(String s, StringeeVideoTrack.MediaType mediaType, boolean b) {

            }

            @Override
            public void onLocalTrackAdded(StringeeCall2 stringeeCall2, StringeeVideoTrack stringeeVideoTrack) {

            }

            @Override
            public void onRemoteTrackAdded(StringeeCall2 stringeeCall2, StringeeVideoTrack stringeeVideoTrack) {

            }
        });

        String customData = callConfig.getCustomData();
        if (customData != null && customData.length() > 0) {
            outgoingCall2.setCustom(customData);
        }
        outgoingCall2.makeCall(null);
    }

    private void startCall() {
        if (isEnded) {
            return;
        }
        audioManager = StringeeAudioManager.create(this);
        audioManager.setSpeakerphoneOn(callConfig.isVideoCall());
        audioManager.start(new StringeeAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(StringeeAudioManager.AudioDevice audioDevice, Set<StringeeAudioManager.AudioDevice> set) {
                if (audioManager == null) {
                    return;
                }
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
        });

        String from = StringeeWidget.getInstance(this).getClient().getUserId();
        String callFrom = callConfig.getFrom();
        if (callFrom != null && callFrom.trim().length() > 0) {
            from = callFrom;
        }
        String to = callConfig.getTo();
        outgoingCall = new StringeeCall(StringeeWidget.getInstance(this).getClient(), from, to);
        outgoingCall.setCallListener(new StringeeCall.StringeeCallListener() {
            @Override
            public void onSignalingStateChange(StringeeCall stringeeCall, final StringeeCall.SignalingState signalingState, String reason, int sipCode, String sipReason) {
                Log.d("Stringee", "onSignalingStateChange: " + signalingState.toString());
                runOnUiThread(() -> {
                    mSignalingState = signalingState;
                    StringeeListener listener = StringeeWidget.getInstance(OutgoingCallActivity.this).getListener();
                    switch (mSignalingState) {
                        case CALLING:
                            tvStatus.setText(R.string.stringee_calling);
                            StatusListener statusListener = StringeeWidget.statusListenerMap.get(requestId);
                            if (statusListener != null) {
                                statusListener.onSuccess();
                            }
                            if (listener != null) {
                                listener.onCallStateChange(stringeeCall, StringeeCall.SignalingState.CALLING);
                            }
                            if (!stringeeCall.isAppToPhoneCall()) {
                                RingtoneUtils.getInstance(OutgoingCallActivity.this).playWaitingSound();
                            }
                            break;
                        case RINGING:
                            tvStatus.setText(R.string.stringee_ringing);
                            if (listener != null) {
                                listener.onCallStateChange(stringeeCall, StringeeCall.SignalingState.RINGING);
                            }
                            break;
                        case ANSWERED:
                            if (listener != null) {
                                listener.onCallStateChange(stringeeCall, StringeeCall.SignalingState.ANSWERED);
                            }
                            tvStatus.setText(R.string.stringee_call_starting);
                            if (mMediaState == StringeeCall.MediaState.CONNECTED) {
                                callStarted();
                            }
                            break;
                        case BUSY:
                            if (listener != null) {
                                listener.onCallStateChange(stringeeCall, StringeeCall.SignalingState.BUSY);
                            }
                            tvStatus.setText(R.string.stringee_busy);
                            endCall(false);
                            break;
                        case ENDED:
                            tvStatus.setText(R.string.stringee_call_ended);
                            if (listener != null) {
                                listener.onCallStateChange(stringeeCall, StringeeCall.SignalingState.ENDED);
                            }
                            endCall(false);
                            break;
                    }
                });
            }

            @Override
            public void onError(final StringeeCall stringeeCall, int i, final String s) {
                runOnUiThread(() -> {
                    Utils.reportMessage(OutgoingCallActivity.this, s);
                    StatusListener statusListener = StringeeWidget.statusListenerMap.get(requestId);
                    if (statusListener != null) {
                        statusListener.onError(new StringeeError(i, s));
                    }
                    endCall(false);
                });
            }

            @Override
            public void onHandledOnAnotherDevice(StringeeCall stringeeCall, final StringeeCall.SignalingState signalingState, String desc) {
                runOnUiThread(() -> {
                    switch (signalingState) {
                        case RINGING:
                            break;
                        case BUSY:
                            Utils.reportMessage(OutgoingCallActivity.this, R.string.stringee_another_device_rejected);
                            endCall(false);
                            break;
                        case ENDED:
                            Utils.reportMessage(OutgoingCallActivity.this, R.string.stringee_another_device_ended);
                            endCall(false);
                            break;
                    }
                });
            }

            @Override
            public void onMediaStateChange(StringeeCall stringeeCall, final StringeeCall.MediaState mediaState) {
                runOnUiThread(() -> {
                    mMediaState = mediaState;
                    if (mediaState == StringeeCall.MediaState.CONNECTED) {
                        if (mSignalingState == StringeeCall.SignalingState.ANSWERED) {
                            callStarted();
                        }
                    }
                });
            }

            @Override
            public void onLocalStream(final StringeeCall stringeeCall) {
            }

            @Override
            public void onRemoteStream(final StringeeCall stringeeCall) {
            }

            @Override
            public void onCallInfo(StringeeCall stringeeCall, final JSONObject jsonObject) {
            }
        });

        String customData = callConfig.getCustomData();
        if (customData != null && customData.length() > 0) {
            outgoingCall.setCustom(customData);
        }
        outgoingCall.makeCall(new StatusListener() {
            @Override
            public void onSuccess() {
                Log.d("Stringee", "Make call successfully");
            }
        });
    }

    private void endCall(boolean isHangup) {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        vControl.setVisibility(View.GONE);
        imNetwork.setVisibility(View.GONE);

        if (!callConfig.isCallout()) {
            RingtoneUtils.getInstance(this).stopWaitingSound();
        }

        RingtoneUtils.getInstance(this).playEndCallSound();

        startTime = 0;
        if (timer != null) {
            timer.cancel();
        }
        if (statsTimer != null) {
            statsTimer.cancel();
        }

        if (outgoingCall != null && isHangup) {
            outgoingCall.hangup(null);
        }
        outgoingCall = null;

        if (outgoingCall2 != null && isHangup) {
            outgoingCall2.hangup(null);
        }
        outgoingCall2 = null;

        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }

        finish();
        StringeeWidget.getInstance(this).setInCall(false);
    }

    private void checkCallStats2(StringeeCall2.StringeeCallStats stats) {
        if (outgoingCall2 == null) {
            return;
        }
        double videoTimestamp = stats.timeStamp / 1000d;
        long bytesReceived = outgoingCall2.isVideoCall() ? (long) stats.videoBytesReceived : (long) stats.callBytesReceived;
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

    private void checkCallStats(StringeeCall.StringeeCallStats stats) {
        if (outgoingCall == null) {
            return;
        }
        double videoTimestamp = stats.timeStamp / 1000d;
        long bytesReceived = outgoingCall.isVideoCall() ? (long) stats.videoBytesReceived : (long) stats.callBytesReceived;
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

        imNetwork.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.stringee_call_started);

        RingtoneUtils.getInstance(this).stopWaitingSound();

        startTime = System.currentTimeMillis();
        timer = new Timer();
        timerTask = new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(() -> tvStatus.setText(DateTimeUtils.getCallTime(System.currentTimeMillis(), startTime)));
            }
        };
        timer.schedule(timerTask, 0, 1000);

        statsTimer = new Timer();
        statsTimerTask = new TimerTask() {

            @Override
            public void run() {
                if (outgoingCall != null) {
                    outgoingCall.getStats(statsReport -> runOnUiThread(() -> checkCallStats(statsReport)));
                }
            }
        };
        statsTimer.schedule(statsTimerTask, 0, 2000);
    }

    private void callStarted2() {
        if (startTime > 0) {
            return;
        }

        imNetwork.setVisibility(View.VISIBLE);
        tvStatus.setText(R.string.stringee_call_started);

        RingtoneUtils.getInstance(this).stopWaitingSound();

        startTime = System.currentTimeMillis();
        timer = new Timer();
        timerTask = new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(() -> tvStatus.setText(DateTimeUtils.getCallTime(System.currentTimeMillis(), startTime)));
            }
        };
        timer.schedule(timerTask, 0, 1000);

        statsTimer = new Timer();
        statsTimerTask = new TimerTask() {

            @Override
            public void run() {
                if (outgoingCall2 != null) {
                    outgoingCall2.getStats(statsReport -> runOnUiThread(() -> checkCallStats2(statsReport)));
                }
            }
        };
        statsTimer.schedule(statsTimerTask, 0, 2000);
    }
}