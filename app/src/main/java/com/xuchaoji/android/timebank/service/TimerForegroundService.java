package com.xuchaoji.android.timebank.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.xuchaoji.android.timebank.R;
import com.xuchaoji.android.timebank.manager.TimeBankManager;
import com.xuchaoji.android.timebank.ui.AlertActivity;
import com.xuchaoji.android.timebank.ui.MainActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TimerForegroundService extends Service {

    private static final String CHANNEL_ID = "kidtime_timer_channel";
    private static final int NOTIFICATION_ID = 1;

    public static final String ACTION_STOP = "com.xuchaoji.android.timebank.STOP_TIMER";
    public static final String ACTION_TICK = "com.xuchaoji.android.timebank.TIMER_TICK";
    public static final String ACTION_TIME_UP = "com.xuchaoji.android.timebank.TIME_UP";
    public static final String ACTION_SECOND_TICK = "com.xuchaoji.android.timebank.SECOND_TICK";
    public static final String EXTRA_REMAINING_MINUTES = "remaining_minutes";
    public static final String EXTRA_REMAINING_SECONDS = "remaining_seconds";

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> tickFuture;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopTimer();
            return START_NOT_STICKY;
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_timer)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
        startTicking();

        return START_STICKY;
    }

    private void startTicking() {
        TimeBankManager manager = TimeBankManager.getInstance();
        manager.checkAndApplyDailyReset();
        startMinuteTicking();
    }

    private void startMinuteTicking() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        tickFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                TimeBankManager mgr = TimeBankManager.getInstance();
                int balance = mgr.getCurrentBalance();

                if (balance > 1) {
                    mgr.consumeTime(1);
                    int remaining = mgr.getCurrentBalance();
                    sendMinuteTickBroadcast(remaining);
                } else if (balance == 1) {
                    mgr.consumeTime(1);
                    sendMinuteTickBroadcast(0);
                    cancelCurrentTick();
                    startSecondCountdown();
                } else {
                    cancelCurrentTick();
                    triggerTimeUp();
                }
            }
        }, 0, 60, TimeUnit.SECONDS);
    }

    private void startSecondCountdown() {
        final int[] secondsLeft = {60};
        scheduler = Executors.newSingleThreadScheduledExecutor();
        tickFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                secondsLeft[0]--;
                if (secondsLeft[0] > 0) {
                    sendSecondTickBroadcast(secondsLeft[0]);
                } else {
                    cancelCurrentTick();
                    triggerTimeUp();
                }
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void cancelCurrentTick() {
        if (tickFuture != null) {
            tickFuture.cancel(false);
            tickFuture = null;
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void triggerTimeUp() {
        sendTimeUpBroadcast();
        mainHandler.post(() -> {
            showAlertActivity();
            mainHandler.postDelayed(this::stopTimer, 500);
        });
    }

    private void sendMinuteTickBroadcast(int remainingMinutes) {
        Intent intent = new Intent(ACTION_TICK);
        intent.putExtra(EXTRA_REMAINING_MINUTES, remainingMinutes);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendSecondTickBroadcast(int remainingSeconds) {
        Intent intent = new Intent(ACTION_SECOND_TICK);
        intent.putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendTimeUpBroadcast() {
        Intent intent = new Intent(ACTION_TIME_UP);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void showAlertActivity() {
        Intent alertIntent = new Intent(this, AlertActivity.class);
        alertIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(alertIntent);
    }

    private void stopTimer() {
        if (tickFuture != null) {
            tickFuture.cancel(false);
            tickFuture = null;
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (tickFuture != null) {
            tickFuture.cancel(false);
            tickFuture = null;
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.channel_description));
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
