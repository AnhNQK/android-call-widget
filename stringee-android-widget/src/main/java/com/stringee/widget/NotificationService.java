package com.stringee.widget;

import static android.content.Context.NOTIFICATION_SERVICE;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

public class NotificationService {
    private static final String CALL_CHANNEL_ID = "com.stringee.widget.call.notification";
    private static final String CHANNEL_NAME = "Stringee Widget Channel";
    private static final String CALL_CHANNEL_NAME = "Stringee Widget Call Channel";
    private static final String CHANNEL_DESC = "Widget channel for notification";

    public static void notifyIncomingCall(Context context, String callId, String from, String fromAlias) {
        NotificationManager mNotificationManager;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mNotificationManager = context.getSystemService(NotificationManager.class);
            NotificationChannel channel = mNotificationManager.getNotificationChannel(CALL_CHANNEL_ID);

            if (channel != null && channel.getImportance() < NotificationManager.IMPORTANCE_HIGH) {
                mNotificationManager.deleteNotificationChannel(CALL_CHANNEL_ID);
            }
            channel = new NotificationChannel(CALL_CHANNEL_ID, CALL_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setBypassDnd(true);
            mNotificationManager.createNotificationChannel(channel);
        } else {
            mNotificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        }

        int flag = PendingIntent.FLAG_UPDATE_CURRENT;
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            flag = PendingIntent.FLAG_IMMUTABLE;
        }

        Intent fullScreenIntent = new Intent(context, IncomingCallActivity.class);
        fullScreenIntent.putExtra(Constant.PARAM_CALL_ID, callId);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff),
                fullScreenIntent, flag);

        RemoteViews notifyView = new RemoteViews(context.getPackageName(), R.layout.stringee_incoming_call_big_notification);
        notifyView.setTextViewText(R.id.nameTextView, fromAlias);
        notifyView.setTextViewText(R.id.numberTextView, from);

        Intent rejectIntent = new Intent(context, RejectCallService.class);
        rejectIntent.putExtra(Constant.PARAM_CALL_ID, callId);
        PendingIntent rejectPendingIntent =
                PendingIntent.getService(context, (int) (System.currentTimeMillis() & 0xfffffff), rejectIntent, flag);
        notifyView.setOnClickPendingIntent(R.id.v_reject, rejectPendingIntent);

        Intent answerIntent = new Intent(context, IncomingCallActivity.class);
        answerIntent.putExtra(Constant.PARAM_CALL_ID, callId);
        answerIntent.putExtra(Constant.PARAM_ANSWER, true);
        PendingIntent answerPendingIntent = PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff),
                answerIntent, flag);
        notifyView.setOnClickPendingIntent(R.id.v_answer, answerPendingIntent);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, CALL_CHANNEL_ID)
                .setSmallIcon(R.mipmap.icon)
                .setOngoing(true)
                .setVibrate(new long[0])
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenPendingIntent, true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            notificationBuilder.setShowWhen(false);
        }

        Notification incomingCallNotification = notificationBuilder.getNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            incomingCallNotification.headsUpContentView = incomingCallNotification.bigContentView = notifyView;
        }

        mNotificationManager.notify(Constant.INCOMING_CALL_NOTIFICATION_ID, incomingCallNotification);
    }
}
