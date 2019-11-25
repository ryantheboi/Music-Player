package com.example.musicplayer;
import static com.example.musicplayer.Notifications.CHANNEL_ID_1;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.widget.Button;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import androidx.media.app.NotificationCompat.MediaStyle;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

public class MainActivity
        extends AppCompatActivity
        implements View.OnClickListener{

    private Button play, pause, stop;
    private Intent serviceIntent;
    private MediaSessionCompat mediaSession;
    private NotificationManagerCompat notificationManager;

    @Override
    @TargetApi(26)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        play = findViewById((R.id.btn_play));
        pause = findViewById((R.id.btn_pause));
        stop = findViewById((R.id.btn_stop));

        play.setOnClickListener(this);
        pause.setOnClickListener(this);
        stop.setOnClickListener(this);

        serviceIntent = new Intent(this, MusicPlayerService.class);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaSession = new MediaSessionCompat(this, "media");
        notificationManager = NotificationManagerCompat.from(this);
    }

    @Override
    @TargetApi(26)
    public void onClick(View view){

        switch(view.getId()){
            case R.id.btn_play:
                startService(serviceIntent);
                break;

            case R.id.btn_pause:
                startService(serviceIntent);
                break;

            case R.id.btn_stop:
                stopService(serviceIntent);
                break;
        }
    }

    public void sendNotification(View view){
        Bitmap largeImage = BitmapFactory.decodeResource(getResources(), R.drawable.kaminomanimani);
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

        Notification channel1 = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID_1)
                .setSmallIcon(R.drawable.ic_music)
                .setContentTitle("song3")
                .setContentText("artist3")
                .setLargeIcon(largeImage)
                .addAction(R.drawable.ic_prev, "prev", null)
                .addAction(R.drawable.ic_pause, "pause", null)
                .addAction(R.drawable.ic_next, "next", null)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.getSessionToken()))
                .build();
        notificationManager.notify(1, channel1);
    }
}
