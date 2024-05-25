package com.stringee.widget;

/**
 * Created by luannguyen on 7/27/2017.
 */

public enum Notify {
    CALL_SIGNAL_CHANGE("com.stringee.call.signal.change"),
    CALL_MEDIA_CHANGE("com.stringee.call.media.change"),
    CALL_SIGNAL_CHANGE2("com.stringee.call2.signal.change"),
    CALL_MEDIA_CHANGE2("com.stringee.call2.media.change"),
    CALL_HANDLED_ON_OTHER_DEVICE("com.stringee.call.handled"),
    TELEPHONY_STATE_CHANGE("com.stringee.telephony.state.change");

    private String value;

    private Notify(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
