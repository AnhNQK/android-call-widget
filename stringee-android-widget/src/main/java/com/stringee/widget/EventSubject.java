package com.stringee.widget;

import android.content.Intent;

public interface EventSubject {
    void registerEvent(EventReceiver receiver, String flag);

    void unregisterEvent(String flag);

    void sendEvent(String flag, Intent intent);
}
