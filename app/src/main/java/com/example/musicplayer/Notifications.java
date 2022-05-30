package com.example.musicplayer;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class Notifications extends Application{
    public static final String CHANNEL_ID_1 = "channel1";
    public static final String CHANNEL_NAME_1 = "MusicPlayer Notification Channel";

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
    }

    @TargetApi(23)
    private void createNotificationChannel(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel notificationChannel = new NotificationChannel(
                    CHANNEL_ID_1,
                    CHANNEL_NAME_1,
                    NotificationManager.IMPORTANCE_LOW
            );
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(notificationChannel);
        }

    }
}