package org.maowtm.android.tagalarm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class AlertService extends Service {
    public static final String ACTION_PLAY_ALARM = "+";
    public static final String ACTION_STOP_ALARM = "-";
    public static final String ACTION_RECALCULATE = ".";
    protected Set<Integer> alarms;
    protected MediaPlayer player;
    protected AudioManager am;
    public final class ForceActivityFrontRunnable implements Runnable {
        @Override
        public void run() {
            AlertService.this.bringFront();
            AlertService.this.handler.postDelayed(this, 500);
        }
    }
    protected Handler handler;
    protected ForceActivityFrontRunnable runnable;
    @Override
    public void onCreate() {
        super.onCreate();
        this.alarms = new HashSet<>();
        this.am = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        this.runnable = new ForceActivityFrontRunnable();
        this.handler = new Handler();
        this.handler.postDelayed(this.runnable, 500);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        this.stop();
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            this.stopSelf();
            return Service.START_NOT_STICKY;
        }
        int alarmId = intent.getIntExtra(Alarms.INTENT_EXTRA_ALARM_ID, -1);
        if (alarmId < 0) {
            return Service.START_NOT_STICKY;
        }
        switch (intent.getAction()) {
            case ACTION_PLAY_ALARM:
                if (!this.alarms.contains(alarmId)) {
                    this.alarms.add(alarmId);
                }
                if (this.player == null) {
                    this.play();
                }
                break;
            case ACTION_STOP_ALARM:
                if (this.alarms.contains(alarmId)) {
                    this.alarms.remove(alarmId);
                }
                if (this.alarms.isEmpty()) {
                    stop();
                }
                break;
        }
        return START_NOT_STICKY;
    }
    public void play() {
        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (this.player != null)
            return;
        this.player = new MediaPlayer();
        player.setVolume(1f, 1f);
        try {
            player.setDataSource(this, alert);
            play(player);
        } catch (IOException e) {
            player.reset();
            try {
                throw new IOException();
//                AssetFileDescriptor afd = getResources().openRawResourceFd(com.android.internal.R.raw.fallbackring);
//                if (afd != null) {
//                    player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
//                    play(player);
//                } else {
//                    throw new IOException("aft == null");
//                }
            } catch (IOException e2) {
                player.release();
                player = null;
                // TODO: Vibrate
            }
        }
    }
    protected void play(MediaPlayer player) throws IOException {
        player.setAudioStreamType(AudioManager.STREAM_ALARM);
        player.setLooping(true);
        player.prepare();
        player.start();
        Log.d("reach", "player.start()");
    }
    public void stop() {
        this.alarms.clear();
        if (player != null) {
            player.stop();
            player.release();
            player = null;
        }
        this.stopSelf();
    }
    public void bringFront() {
        if (this.alarms.isEmpty()) {
            this.stop();
            return;
        }
        Alarms.showAlertUI(this, this.alarms.iterator().next());
        int stream = AudioManager.STREAM_ALARM;
        // TODO: Read custom volume settings
        this.am.setStreamVolume(stream, this.am.getStreamMaxVolume(stream), 0);
    }
}
