package com.stringee.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class RingtoneUtils {

    private static RingtoneUtils ringtoneUtils;
    private Context context;

    private Uri incomingRingtoneUri;
    private Uri endCallPlayerUri;
    private Uri waitingPlayerUri;

    private MediaPlayer incomingRingtone;
    private MediaPlayer endCallPlayer;
    private MediaPlayer ringingPlayer;
    private Vibrator incomingVibrator;

    private AudioManager am;
    private int previousAudioModel;
    private boolean previousSpeaker;

    public static RingtoneUtils getInstance(Context context) {
        if (ringtoneUtils == null) {
            ringtoneUtils = new RingtoneUtils(context);
        }
        return ringtoneUtils;
    }

    public RingtoneUtils(Context context) {
        this.context = context;
        this.incomingRingtoneUri = Uri.parse(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString());
        this.endCallPlayerUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.stringee_call_end);
        this.waitingPlayerUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.stringee_call_ringing);

        this.incomingRingtone = new MediaPlayer();
        this.ringingPlayer = new MediaPlayer();
        this.endCallPlayer = new MediaPlayer();
    }

    public void playWaitingSound() {
        Utils.runOnUiThread(() -> {
            if (ringingPlayer.isPlaying() || ringingPlayer.isLooping()) {
                ringingPlayer.stop();
                ringingPlayer.reset();
            }
            ringingPlayer.setOnPreparedListener(mediaPlayer -> ringingPlayer.start());
            ringingPlayer.setLooping(true);
            ringingPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            try {
                ringingPlayer.setDataSource(context, waitingPlayerUri);
                ringingPlayer.prepareAsync();
            } catch (Exception e) {
                stopWaitingSound();
            }
        });
    }

    public void stopWaitingSound() {
        Utils.runOnUiThread(() -> {
            if (ringingPlayer != null) {
                ringingPlayer.stop();
                ringingPlayer.reset();
            }
        });
    }

    public void playEndCallSound() {
        Utils.runOnUiThread(() -> {
            if (endCallPlayer != null) {
                if (endCallPlayer.isPlaying()) {
                    return;
                }
            }
            endCallPlayer.setOnPreparedListener(mediaPlayer -> endCallPlayer.start());
            endCallPlayer.setLooping(false);
            endCallPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            try {
                endCallPlayer.setDataSource(context, endCallPlayerUri);
                endCallPlayer.setOnCompletionListener(mediaPlayer -> {
                    stopEndCallSound();
                });
                endCallPlayer.prepareAsync();
            } catch (Exception e) {
                stopEndCallSound();
            }
        });
    }

    public void stopEndCallSound() {
        Utils.runOnUiThread(() -> {
            if (endCallPlayer != null) {
                endCallPlayer.stop();
                endCallPlayer.reset();
            }
        });
    }

    @SuppressLint("MissingPermission")
    public void ringing() {
        Utils.runOnUiThread(() -> {
            am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            previousAudioModel = am.getMode();
            previousSpeaker = am.isSpeakerphoneOn();
            am.setMode(AudioManager.MODE_RINGTONE);
            am.setSpeakerphoneOn(true);
            boolean isHeadsetPlugged = false;
            if (VERSION.SDK_INT < VERSION_CODES.M) {
                isHeadsetPlugged = am.isWiredHeadsetOn();
            } else {
                final AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                for (AudioDeviceInfo device : devices) {
                    final int type = device.getType();
                    if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                        isHeadsetPlugged = true;
                        break;
                    }
                }
            }
            boolean needRing = am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
            boolean needVibrate = am.getRingerMode() != AudioManager.RINGER_MODE_SILENT;

            if (needRing) {
                if (incomingRingtone.isPlaying() || incomingRingtone.isLooping()) {
                    incomingRingtone.stop();
                    incomingRingtone.reset();
                }
                incomingRingtone.setOnPreparedListener(mediaPlayer -> incomingRingtone.start());
                incomingRingtone.setLooping(true);
                if (isHeadsetPlugged) {
                    incomingRingtone.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
                } else {
                    incomingRingtone.setAudioStreamType(AudioManager.STREAM_RING);
                }
                try {
                    incomingRingtone.setDataSource(context, incomingRingtoneUri);
                    incomingRingtone.prepareAsync();
                } catch (Exception e) {
                    if (incomingRingtone != null) {
                        incomingRingtone.stop();
                        incomingRingtone.reset();
                    }
                }
            }
            if (needVibrate) {
                incomingVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    incomingVibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 350, 500}, 0), new Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build());
                } else {
                    incomingVibrator.vibrate(new long[]{0, 350, 500}, 0);
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    public void stopRinging() {
        Utils.runOnUiThread(() -> {
            if (am != null) {
                am.setMode(previousAudioModel);
                am.setSpeakerphoneOn(previousSpeaker);
            }
            if (incomingRingtone != null) {
                incomingRingtone.stop();
                incomingRingtone.reset();
            }
            if (incomingVibrator != null) {
                incomingVibrator.cancel();
            }
        });
    }
}
