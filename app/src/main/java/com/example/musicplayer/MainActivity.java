package com.example.musicplayer;
import static com.example.musicplayer.Notifications.CHANNEL_ID_1;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.View;
import android.widget.Button;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import androidx.media.app.NotificationCompat.MediaStyle;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class MainActivity
        extends AppCompatActivity
        implements View.OnClickListener{
    Button play, pause, stop;
    MediaPlayer mediaPlayer;
    Context mContext;
    AudioManager mAudioManager;
    AudioAttributes mAudioAttributes;
    AudioFocusRequest mAudioFocusRequest;
    boolean mPlayOnAudioFocus;
    int pauseCurrentPosition;
    private MediaSessionCompat mediaSession;
    private NotificationManagerCompat notificationManager;

    @Override
    @TargetApi(26)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();

        play = findViewById((R.id.btn_play));
        pause = findViewById((R.id.btn_pause));
        stop = findViewById((R.id.btn_stop));

        play.setOnClickListener(this);
        pause.setOnClickListener(this);
        stop.setOnClickListener(this);

        mediaSession = new MediaSessionCompat(this, "media");
        notificationManager = NotificationManagerCompat.from(this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        mAudioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
        mAudioFocusRequest =
                new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(mAudioAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(
                            new AudioManager.OnAudioFocusChangeListener() {
                                @Override
                                public void onAudioFocusChange(int focusChange) {
                                    switch (focusChange) {
                                        case AudioManager.AUDIOFOCUS_GAIN:
                                            if (mPlayOnAudioFocus && !mediaPlayer.isPlaying()) {
                                                mediaPlayer.seekTo(pauseCurrentPosition);
                                                mediaPlayer.start();
                                            } else if (mediaPlayer.isPlaying()) {
                                                mediaPlayer.setVolume(1.0f, 1.0f);
                                            }
                                            mPlayOnAudioFocus = false;
                                            break;
                                        // case may not be necessary as Android O and above supports auto ducking automatically
                                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                                            mediaPlayer.setVolume(0.05f, 0.05f);
                                            break;
                                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                                            if (mediaPlayer.isPlaying()) {
                                                mPlayOnAudioFocus = true;
                                                mediaPlayer.pause();
                                                pauseCurrentPosition = mediaPlayer.getCurrentPosition();
                                            }
                                            break;
                                        case AudioManager.AUDIOFOCUS_LOSS:
                                            mAudioManager.abandonAudioFocus(this);
                                            mPlayOnAudioFocus = false;
                                            mediaPlayer.stop();
                                            break;
                                    }
                                }
                        }).build();
    }

    @Override
    @TargetApi(26)
    public void onClick(View view){

        switch(view.getId()){
            case R.id.btn_play:
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer.create(mContext, R.raw.aft);
                    // keep CPU from sleeping and play music with screen off
                    mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
                    int focusRequest = mAudioManager.requestAudioFocus(mAudioFocusRequest);
                    switch (focusRequest) {
                        case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                            break;
                        case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
                            mediaPlayer.start();
                            break;
                    }
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
