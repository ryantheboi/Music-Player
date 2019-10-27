package com.example.musicplayer;
import static com.example.musicplayer.Notifications.CHANNEL_ID_1;

import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import androidx.media.app.NotificationCompat.MediaStyle;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    Button play, pause, stop;
    MediaPlayer mediaPlayer;
    int pauseCurrentPosition;
    private NotificationManagerCompat notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        play = findViewById((R.id.btn_play));
        pause = findViewById((R.id.btn_pause));
        stop = findViewById((R.id.btn_stop));

        play.setOnClickListener(this);
        pause.setOnClickListener(this);
        stop.setOnClickListener(this);

        notificationManager = NotificationManagerCompat.from(this);
    }

    @Override
    public void onClick(View view){

        switch(view.getId()){
            case R.id.btn_play:
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.aft);
                    mediaPlayer.start();
                }

                else if (!mediaPlayer.isPlaying()){
                    mediaPlayer.seekTo(pauseCurrentPosition);
                    mediaPlayer.start();
                }

                break;

            case R.id.btn_pause:
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                    pauseCurrentPosition = mediaPlayer.getCurrentPosition();
                }
                break;

            case R.id.btn_stop:
                if (mediaPlayer != null) {
                    mediaPlayer.stop();
                    mediaPlayer = null;
                }
                break;
        }
    }

    public void sendNotification(View view){
        Bitmap largeImage = BitmapFactory.decodeResource(getResources(), R.drawable.music_default);
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
                        .setShowActionsInCompactView(0, 2))
                .build();
        notificationManager.notify(1, channel1);
    }
}
