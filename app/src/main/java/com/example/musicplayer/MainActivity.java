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
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity
        extends AppCompatActivity{

    private ImageButton musicList;
    private ImageButton pauseplay;
    private SeekBar seekBar;
    private TextView musicPosition;
    private TextView musicDuration;
    private MediaSessionCompat mediaSession;
    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private Notification notificationChannel1;
    private Intent serviceIntent;
    private Intent pauseplayIntent;
    private Intent prevIntent;
    private Intent nextIntent;
    private Intent seekBarProgressIntent;
    private Intent seekBarSeekIntent;

    @Override
    @TargetApi(26)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        serviceIntent = new Intent(this, MusicPlayerService.class);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaSession = new MediaSessionCompat(this, "media");
        notificationManager = NotificationManagerCompat.from(this);
        showNotification();

        // init button for displaying music list
        musicList = findViewById(R.id.btn_musiclist);
        musicList.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                openMusicList();
            }
        });

        // init seekbar and textviews
        seekBar = findViewById(R.id.seekBar);
        musicPosition = findViewById(R.id.music_position);
        musicDuration = findViewById(R.id.music_duration);

        // set the seekbar & textview duration and sync with mediaplayer
        Intent seekBarDurationIntent = new Intent(this, MusicPlayerService.class);
        seekBarProgressIntent = new Intent(this, MusicPlayerService.class);
        seekBarSeekIntent = new Intent(this, MusicPlayerService.class);
        Messenger mainMessenger = new Messenger(new MessageHandler());
        seekBarDurationIntent.putExtra("seekbarDuration", mainMessenger);
        startService(seekBarDurationIntent);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String time = convertTime(progress);
                musicPosition.setText(time);
                if (fromUser) {
                    seekBarSeekIntent.putExtra("seekbarSeek", progress * 1000);
                    startService(seekBarSeekIntent);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        pauseplay = findViewById((R.id.btn_play));

        pauseplay.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){

                switch(view.getId()){
                    case R.id.btn_play:
                        Messenger mainMessenger = new Messenger(new MessageHandler());
                        serviceIntent.putExtra("pauseplay", mainMessenger);
                        startService(serviceIntent);
                        break;
                }
            }
        });
    }

    // helper function to convert time in seconds to HH:MM:SS format
    public static String convertTime(int timeInSeconds){
        int seconds = timeInSeconds % 3600 % 60;
        int minutes = timeInSeconds % 3600 / 60;
        int hours = timeInSeconds / 3600;

        String HH, MM, SS;
        if (hours == 0){
            MM = ((minutes  < 10) ? "" : "") + minutes;
            SS = ((seconds  < 10) ? "0" : "") + seconds;
            return MM + ":" + SS;
        }
        else {
            HH = ((hours    < 10) ? "0" : "") + hours;
            MM = ((minutes  < 10) ? "0" : "") + minutes;
            SS = ((seconds  < 10) ? "0" : "") + seconds;
        }

        return HH + ":" + MM + ":" + SS;
    }

    @TargetApi(19)
    public void showNotification(){
        Bitmap largeImage = BitmapFactory.decodeResource(getResources(), R.drawable.kaminomanimani);
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

        // create intents for the notification action buttons
        Messenger notificationMessenger = new Messenger(new MessageHandler());
        prevIntent = new Intent(this, MusicPlayerService.class).putExtra("notificationPrev", notificationMessenger);
        pauseplayIntent =  new Intent(this, MusicPlayerService.class).putExtra("pauseplay", notificationMessenger);
        nextIntent = new Intent(this, MusicPlayerService.class).putExtra("notificationNext", notificationMessenger);

        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID_1)
                .setSmallIcon(R.drawable.ic_music)
                .setContentTitle("song")
                .setContentText("artist")
                .setLargeIcon(largeImage)
                .addAction(R.drawable.ic_prev, "prev", PendingIntent.getService(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_play24dp, "play", PendingIntent.getService(this, 1, pauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_next, "next", PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .setStyle(new MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.getSessionToken()));

        notificationChannel1 = notificationBuilder.build();
        notificationManager.notify(1, notificationChannel1);
    }

    // function to launch the music list activity
    public void openMusicList(){
        Intent musicListIntent = new Intent(this, MusicListActivity.class);
        startActivity(musicListIntent);
    }

    class MessageHandler extends Handler
    {
        @Override
        @TargetApi(19)
        public void handleMessage(Message msg) {

            Bundle bundle = msg.getData();
            String updateOperation = (String) bundle.get("update");
            switch (updateOperation) {
                case "update_main_play":
                    pauseplay.setImageResource(R.drawable.ic_play);
                    break;
                case "update_main_pause":
                    pauseplay.setImageResource(R.drawable.ic_pause);
                    break;
                case "update_notification_play":
                    notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_play24dp, "play", PendingIntent.getService(getApplicationContext(), 1, pauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                    notificationChannel1 = notificationBuilder.build();
                    notificationManager.notify(1, notificationChannel1);
                    break;
                case "update_notification_pause":
                    notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_pause24dp, "pause", PendingIntent.getService(getApplicationContext(), 1, pauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                    notificationChannel1 = notificationBuilder.build();
                    notificationManager.notify(1, notificationChannel1);
                    break;
                case "update_seekbar_duration":
                    // init the seekbar & textview max duration and begin thread to track progress
                    int musicMaxDuration = (int) bundle.get("time");
                    seekBar.setMax(musicMaxDuration);
                    String time = convertTime(musicMaxDuration);
                    musicDuration.setText(time);

                    // spawn a thread to update seekbar progress each second
                    final Messenger seekMessenger = new Messenger(new MessageHandler());
                    seekBarProgressIntent.putExtra("seekbarProgress", seekMessenger);
                    Thread seekbarUpdateThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                startService(seekBarProgressIntent);
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    seekbarUpdateThread.start();
                    break;
                case "update_seekbar_progress":
                    int musicCurrentPosition = (int) bundle.get("time");
                    seekBar.setProgress(musicCurrentPosition);
                    break;
            }
        }
    }
}
