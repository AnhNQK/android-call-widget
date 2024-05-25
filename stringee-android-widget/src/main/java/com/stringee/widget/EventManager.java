package com.stringee.widget;

import android.content.Intent;

import java.util.HashMap;
import java.util.Map;

public class EventManager implements EventSubject {

    private static EventManager eventManager;
    private Map<String, EventReceiver> eventMap = new HashMap<>();

    public EventManager() {
    }

    public static EventManager getInstance() {
        if (eventManager == null) {
            eventManager = new EventManager();
        }
        return eventManager;
    }

    @Override
    public void registerEvent(EventReceiver receiver, String flag) {
        Utils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                eventMap.put(flag, receiver);
            }
        });
    }

    @Override
    public void unregisterEvent(String flag) {
        Utils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                eventMap.remove(flag);
            }
        });
    }

    @Override
    public void sendEvent(String flag, Intent intent) {
        Utils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                EventReceiver observer = eventMap.get(flag);
                if (observer != null) {
                    observer.onReceive(intent);
                }
            }
        });
    }
}
