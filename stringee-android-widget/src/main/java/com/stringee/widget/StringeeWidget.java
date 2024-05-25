package com.stringee.widget;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.common.SocketAddress;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.listener.StringeeConnectionListener;
import com.stringee.video.StringeeVideoTrack;

import org.json.JSONObject;
import org.webrtc.ThreadUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class StringeeWidget {
    private static StringeeWidget stringeeWidget;
    private static StringeeListener listener;
    private List<SocketAddress> host = new ArrayList<>();
    private static Context mContext;
    private static StringeeClient client;
    private boolean isInCall;
    public static final ConcurrentHashMap<Integer, StatusListener> statusListenerMap = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, StringeeCall> callMap = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, StringeeCall2> callMap2 = new ConcurrentHashMap<>();
    private int requestId;

    /**
     * Get a single instance
     *
     * @return
     */
    public static StringeeWidget getInstance(Context context) {
        if (stringeeWidget == null) {
            stringeeWidget = new StringeeWidget();
        }
        mContext = context;
        if (client == null) {
            client = new StringeeClient(context);
            client.setConnectionListener(new StringeeConnectionListener() {
                @Override
                public void onConnectionConnected(StringeeClient stringeeClient, boolean b) {
                    Log.d("Stringee", "onConnectionConnected, reconnecting: " + b);
                    if (!b && listener != null) {
                        listener.onConnectionConnected();
                    }
                }

                @Override
                public void onConnectionDisconnected(StringeeClient stringeeClient, boolean b) {
                    if (!b && listener != null) {
                        listener.onConnectionDisconnected();
                    }
                }

                @Override
                public void onIncomingCall(StringeeCall stringeeCall) {
                    Log.d("Stringee", "onIncomingCall");
                    Utils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (stringeeWidget.isInCall) {
                                stringeeCall.reject(null);
                                return;
                            }
                            String callId = stringeeCall.getCallId();
                            callMap.put(callId, stringeeCall);
                            stringeeCall.setCallListener(new StringeeCall.StringeeCallListener() {
                                @Override
                                public void onSignalingStateChange(StringeeCall stringeeCall2, StringeeCall.SignalingState signalingState, String s, int i, String s1) {
                                    Utils.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (signalingState == StringeeCall.SignalingState.ENDED) {
                                                NotificationManager nm = (NotificationManager) context.getSystemService
                                                        (Context.NOTIFICATION_SERVICE);
                                                nm.cancel(Constant.INCOMING_CALL_NOTIFICATION_ID);
                                                RingtoneUtils.getInstance(mContext).stopRinging();
                                                stringeeWidget.setInCall(false);
                                            }
                                            Intent intent = new Intent();
                                            intent.putExtra(Constant.PARAM_CALL_ID, callId);
                                            intent.putExtra(Constant.PARAM_CALL_SIGNAL_STATE, signalingState.getValue());
                                            EventManager.getInstance().sendEvent(Notify.CALL_SIGNAL_CHANGE.getValue(), intent);
                                        }
                                    });
                                }

                                @Override
                                public void onError(StringeeCall stringeeCall, int i, String s) {

                                }

                                @Override
                                public void onHandledOnAnotherDevice(StringeeCall stringeeCall, StringeeCall.SignalingState signalingState, String s) {
                                    Utils.runOnUiThread(() -> {
                                        Log.d("Stringee", "onHandledOnAnotherDevice: " + s);
                                        switch (signalingState) {
                                            case ANSWERED:
                                            case BUSY:
                                            case ENDED:
                                                NotificationManager nm = (NotificationManager) mContext.getSystemService
                                                        (Context.NOTIFICATION_SERVICE);
                                                nm.cancel(Constant.INCOMING_CALL_NOTIFICATION_ID);
                                                RingtoneUtils.getInstance(mContext).stopRinging();
                                                stringeeWidget.setInCall(false);

                                                Intent intent = new Intent();
                                                intent.putExtra(Constant.PARAM_CALL_ID, stringeeCall.getCallId());
                                                EventManager.getInstance().sendEvent(Notify.CALL_HANDLED_ON_OTHER_DEVICE.getValue(), intent);
                                                stringeeCall.hangup(null);
                                                break;
                                        }
                                    });
                                }

                                @Override
                                public void onMediaStateChange(StringeeCall stringeeCall, StringeeCall.MediaState mediaState) {
                                    Utils.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Intent intent = new Intent();
                                            intent.putExtra(Constant.PARAM_CALL_ID, callId);
                                            intent.putExtra(Constant.PARAM_CALL_MEDIA_STATE, mediaState.getValue());
                                            EventManager.getInstance().sendEvent(Notify.CALL_MEDIA_CHANGE.getValue(), intent);
                                        }
                                    });
                                }

                                @Override
                                public void onLocalStream(StringeeCall stringeeCall) {

                                }

                                @Override
                                public void onRemoteStream(StringeeCall stringeeCall) {

                                }

                                @Override
                                public void onCallInfo(StringeeCall stringeeCall, JSONObject jsonObject) {

                                }
                            });

                            stringeeWidget.setInCall(true);
                            RingtoneUtils.getInstance(context).ringing();
                            stringeeCall.ringing(new StatusListener() {
                                @Override
                                public void onSuccess() {

                                }
                            });
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                NotificationService.notifyIncomingCall(context, stringeeCall.getCallId(), stringeeCall.getFrom(), stringeeCall.getFromAlias());
                            } else {
                                Intent intent = new Intent(context, IncomingCallActivity.class);
                                intent.putExtra(Constant.PARAM_CALL_ID, callId);
                                context.startActivity(intent);
                            }
                        }
                    });
                }

                @Override
                public void onIncomingCall2(StringeeCall2 stringeeCall2) {
                    Log.d("Stringee", "onIncomingCall2");
                    Utils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (stringeeWidget.isInCall) {
                                stringeeCall2.reject(null);
                                return;
                            }
                            String callId = stringeeCall2.getCallId();
                            callMap2.put(callId, stringeeCall2);
                            stringeeCall2.setCallListener(new StringeeCall2.StringeeCallListener() {
                                @Override
                                public void onSignalingStateChange(StringeeCall2 stringeeCall2, StringeeCall2.SignalingState signalingState, String s, int i, String s1) {
                                    Utils.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (signalingState == StringeeCall2.SignalingState.ENDED) {
                                                NotificationManager nm = (NotificationManager) context.getSystemService
                                                        (Context.NOTIFICATION_SERVICE);
                                                nm.cancel(Constant.INCOMING_CALL_NOTIFICATION_ID);
                                                RingtoneUtils.getInstance(mContext).stopRinging();
                                                stringeeWidget.setInCall(false);
                                            }
                                            Intent intent = new Intent();
                                            intent.putExtra(Constant.PARAM_CALL_ID, callId);
                                            intent.putExtra(Constant.PARAM_CALL_SIGNAL_STATE, signalingState.getValue());
                                            EventManager.getInstance().sendEvent(Notify.CALL_SIGNAL_CHANGE2.getValue(), intent);
                                        }
                                    });
                                }

                                @Override
                                public void onError(StringeeCall2 stringeeCall2, int i, String s) {

                                }

                                @Override
                                public void onHandledOnAnotherDevice(StringeeCall2 stringeeCall2, StringeeCall2.SignalingState signalingState, String s) {
                                    Utils.runOnUiThread(() -> {
                                        Log.d("Stringee", "onHandledOnAnotherDevice: " + s);
                                        switch (signalingState) {
                                            case ANSWERED:
                                            case BUSY:
                                            case ENDED:
                                                NotificationManager nm = (NotificationManager) mContext.getSystemService
                                                        (Context.NOTIFICATION_SERVICE);
                                                nm.cancel(Constant.INCOMING_CALL_NOTIFICATION_ID);
                                                RingtoneUtils.getInstance(mContext).stopRinging();
                                                stringeeWidget.setInCall(false);

                                                Intent intent = new Intent();
                                                intent.putExtra(Constant.PARAM_CALL_ID, stringeeCall2.getCallId());
                                                EventManager.getInstance().sendEvent(Notify.CALL_HANDLED_ON_OTHER_DEVICE.getValue(), intent);
                                                stringeeCall2.hangup(null);
                                                break;
                                        }
                                    });
                                }

                                @Override
                                public void onMediaStateChange(StringeeCall2 stringeeCall2, StringeeCall2.MediaState mediaState) {
                                    Utils.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Intent intent = new Intent();
                                            intent.putExtra(Constant.PARAM_CALL_ID, callId);
                                            intent.putExtra(Constant.PARAM_CALL_MEDIA_STATE, mediaState.getValue());
                                            EventManager.getInstance().sendEvent(Notify.CALL_MEDIA_CHANGE2.getValue(), intent);
                                        }
                                    });
                                }

                                @Override
                                public void onLocalStream(StringeeCall2 stringeeCall2) {

                                }

                                @Override
                                public void onRemoteStream(StringeeCall2 stringeeCall2) {

                                }

                                @Override
                                public void onVideoTrackAdded(StringeeVideoTrack stringeeVideoTrack) {

                                }

                                @Override
                                public void onVideoTrackRemoved(StringeeVideoTrack stringeeVideoTrack) {

                                }

                                @Override
                                public void onCallInfo(StringeeCall2 stringeeCall2, JSONObject jsonObject) {

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

                            stringeeWidget.setInCall(true);
                            RingtoneUtils.getInstance(context).ringing();
                            stringeeCall2.ringing(new StatusListener() {
                                @Override
                                public void onSuccess() {

                                }
                            });
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                NotificationService.notifyIncomingCall(context, stringeeCall2.getCallId(), stringeeCall2.getFrom(), stringeeCall2.getFromAlias());
                            } else {
                                Intent intent = new Intent(context, IncomingCallActivity.class);
                                intent.putExtra(Constant.PARAM_CALL_ID, callId);
                                context.startActivity(intent);
                            }
                        }
                    });
                }

                @Override
                public void onConnectionError(StringeeClient stringeeClient, StringeeError stringeeError) {
                    if (listener != null) {
                        listener.onConnectionError(stringeeError);
                    }
                }

                @Override
                public void onRequestNewToken(StringeeClient stringeeClient) {
                    if (listener != null) {
                        listener.onRequestNewToken();
                    }
                }

                @Override
                public void onCustomMessage(String s, JSONObject jsonObject) {

                }

                @Override
                public void onTopicMessage(String s, JSONObject jsonObject) {

                }
            });
        }
        return stringeeWidget;
    }

    public StringeeClient getClient() {
        return client;
    }

    public StringeeListener getListener() {
        return listener;
    }

    public void setListener(StringeeListener listener) {
        this.listener = listener;
    }

    public boolean isInCall() {
        return isInCall;
    }

    public void setInCall(boolean inCall) {
        isInCall = inCall;
    }

    /**
     * Set host
     *
     * @param addresses
     */
    public void setHost(final List<SocketAddress> addresses) {
        if (!addresses.isEmpty()) {
            host.clear();
        }

        host.addAll(addresses);
    }

    public List<SocketAddress> getHost() {
        return host;
    }

    /**
     * Check whether client connect to Stringee server yet
     *
     * @return
     */
    public boolean isConnected() {
        if (client != null && client.isAlreadyConnected()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Connect Stringee server
     *
     * @param accessToken
     */
    public void connect(String accessToken) {
        String userIdFromToken = Utils.getUserId(accessToken);
        if (Utils.isTextEmpty(userIdFromToken)) {
            if (listener != null) {
                listener.onConnectionError(new StringeeError(2, "ACCESS_TOKEN_CANNOT_BE_DECODE"));
            }
            return;
        }

        if (host != null && host.size() > 0) {
            client.setHost(host);
        }

        if (!client.isAlreadyConnected()) {
            client.connect(accessToken);
        }
    }

    /**
     * Disconnect Stringee server
     */
    public void disconnect() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    /**
     * Start a call
     *
     * @param callConfig
     */
    public void makeCall(CallConfig callConfig, StatusListener listener) {
        ThreadUtils.checkIsOnMainThread();
        if (isInCall) {
            if (listener != null) {
                listener.onError(new StringeeError(100, "You're in another call"));
            }
            return;
        }
        isInCall = true;
        requestId++;
        if (listener != null) {
            statusListenerMap.put(requestId, listener);
        }
        Intent intent = new Intent(mContext, OutgoingCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constant.PARAM_CALL_CONFIG, callConfig);
        intent.putExtra(Constant.PARAM_REQUEST_ID, requestId);
        mContext.startActivity(intent);
    }

    /**
     * Connect and make call
     *
     * @param accessToken
     * @param callConfig
     */
    public void makeCall(String accessToken, CallConfig callConfig, StatusListener listener) {
        ThreadUtils.checkIsOnMainThread();
        String userIdFromToken = Utils.getUserId(accessToken);
        if (Utils.isTextEmpty(userIdFromToken)) {
            StringeeError error = new StringeeError(2, "ACCESS_TOKEN_CANNOT_BE_DECODE");
            if (listener != null) {
                listener.onError(error);
            }
            if (getListener() != null) {
                getListener().onConnectionError(error);
            }
            return;
        }
        if (isInCall) {
            if (listener != null) {
                listener.onError(new StringeeError(100, "You're in another call"));
            }
            return;
        }
        isInCall = true;
        if (!client.isAlreadyConnected()) {
            if (host != null && host.size() > 0) {
                client.setHost(host);
            }
            client.connect(accessToken);
        }
        requestId++;
        if (listener != null) {
            statusListenerMap.put(requestId, listener);
        }
        if (Utils.isTextEmpty(callConfig.getFrom())) {
            callConfig.setFrom(userIdFromToken);
        }
        Intent intent = new Intent(mContext, OutgoingCallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(Constant.PARAM_CALL_CONFIG, callConfig);
        intent.putExtra(Constant.PARAM_REQUEST_ID, requestId);
        mContext.startActivity(intent);
    }

    /**
     * Finalize
     */
    public void finalize() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    /**
     * Register push notification token
     *
     * @param token
     * @param listener
     */
    public void registerPushNotification(String token, StatusListener listener) {
        if (client != null && client.isAlreadyConnected()) {
            client.registerPushToken(token, listener);
        } else {
            if (listener != null) {
                listener.onError(new StringeeError(-1, "StringeeClient is not connected yet"));
            }
        }
    }
}