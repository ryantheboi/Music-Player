package com.example.musicplayer;
import static com.example.musicplayer.Notifications.CHANNEL_ID_1;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import androidx.media.app.NotificationCompat.MediaStyle;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity
        extends AppCompatActivity{

    private ImageButton pauseplay;
    private Intent serviceIntent;
    private MediaSessionCompat mediaSession;
    private NotificationManagerCompat notificationManager;

    @Override
    @TargetApi(26)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceIntent = new Intent(this, MusicPlayerService.class);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaSession = new MediaSessionCompat(this, "media");
        notificationManager = NotificationManagerCompat.from(this);

        pauseplay = findViewById((R.id.btn_play));

        pauseplay.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                switch(view.getId()){
                    case R.id.btn_play:
                        Messenger messenger = new Messenger(new MessageHandler());
                        serviceIntent.putExtra("messenger", messenger);
                        startService(serviceIntent);
                        break;
                }
            }
        });
    }


    public void sendNotification(View view){
        Bitmap largeImage = BitmapFactory.decodeResource(getResources(), R.drawable.kaminomanimani);
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

        // create intents for the action buttons
        Intent prevIntent = new Intent(this, MusicPlayerService.class).putExtra("action", "prev");
        Intent pauseIntent = new Intent(this, MusicPlayerService.class).putExtra("action", "pauseplay");
        Intent nextIntent = new Intent(this, MusicPlayerService.class).putExtra("action", "next");

        Notification channel1 = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID_1)
                .setSmallIcon(R.drawable.ic_music)
                .setContentTitle("song3")
                .setContentText("artist3")
                .setLargeIcon(largeImage)
                .addAction(R.drawable.ic_prev, "prev", PendingIntent.getService(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_pause, "pause", PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_next, "next", PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.getSessionToken()))
                .build();
        notificationManager.notify(1, channel1);
    }

    class MessageHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {

            Bundle bundle = msg.getData();
            String hello = (String) bundle.get("update");
            if (hello.equals("update_play")){
                pauseplay.setImageResource(R.drawable.ic_play);
            }
            else{
                pauseplay.setImageResource(R.drawable.ic_pause);
            }
        }
    }

}
