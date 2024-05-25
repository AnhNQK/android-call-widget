package com.stringee.widget;

import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.exception.StringeeError;

public interface StringeeListener {

    public void onConnectionConnected();

    public void onConnectionDisconnected();

    public void onConnectionError(StringeeError error);

    public void onRequestNewToken();

    public void onCallStateChange(StringeeCall stringeeCall, StringeeCall.SignalingState signalingState);

    public void onCallStateChange2(StringeeCall2 stringeeCall, StringeeCall2.SignalingState signalingState);

}