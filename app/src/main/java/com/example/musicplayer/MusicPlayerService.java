package com.example.musicplayer;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.widget.Toast;

public class MusicPlayerService
        extends Service
        implements OnCompletionListener, OnErrorListener {

    private MediaPlayer mediaPlayer;
    AudioManager mAudioManager;
    AudioAttributes mAudioAttributes;
    AudioFocusRequest mAudioFocusRequest;
    boolean mPlayOnAudioFocus;
    int pauseCurrentPosition;

    public static final String DARK_BACKGROUND = "#232123";
    public static final String GREY_BACKGROUND = "#8a8a8a";
    public static final String GREY_TEXT = "#737373";
    public static final String BLUE_TEXT = "#008BFF";
    public static final String DARK_TEXT = "#030303";
    public static final String ALMOST_WHITE = "#F4F4F4";
    public static final Uri artURI = Uri.parse("content://media/external/audio/albumart");

    public static final int UPDATE_PLAY = 0;
    public static final int UPDATE_PAUSE = 1;
    public static final int UPDATE_SEEKBAR_DURATION = 2;
    public static final int UPDATE_SEEKBAR_PROGRESS = 3;
    public static final int UPDATE_SONG = 4;
    public static final int UPDATE_NIGHT = 5;
    public static final int UPDATE_LIGHT = 6;



    @Override
    @TargetApi(26)
    public void onCreate() {
        mediaPlayer = MediaPlayer.create(this, R.raw.aft);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);

        // keep CPU from sleeping and be able to play music with screen off
        mediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

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
                                                stopMedia();
                                                break;
                                        }
                                    }
                                }).build();
    }

    @TargetApi(26)
    public int onStartCommand(Intent intent, int flags, int startId) {
        // begin responding to the messenger based on message received
        Bundle b = intent.getExtras();
        if (b != null) {
            Messenger messenger;
            Bundle musicListbundle;
            for (String key : b.keySet()) {
                //System.out.println(key);
                switch (key){
                    case "pauseplay":
                        // update the pauseplay button icon via messenger and toggle music
                        messenger = intent.getParcelableExtra("pauseplay");
                        if (mediaPlayer.isPlaying()) {
                            sendUpdateMessage(messenger, UPDATE_PLAY);
                        }
                        else{
                            sendUpdateMessage(messenger, UPDATE_PAUSE);

                        }
                        audioFocusToggleMedia();
                        break;
                    case "prev":
                        messenger = intent.getParcelableExtra("prev");
                        Toast.makeText(this, "received notification: prev", Toast.LENGTH_SHORT).show();
                        break;
                    case "next":
                        messenger = intent.getParcelableExtra("next");
                        Toast.makeText(this, "received notification: next", Toast.LENGTH_SHORT).show();
                        break;
                    case "seekbarDuration":
                        messenger = intent.getParcelableExtra("seekbarDuration");
                        Object[] durationMessage = new Object[2];
                        durationMessage[0] = UPDATE_SEEKBAR_DURATION;
                        durationMessage[1] = mediaPlayer.getDuration();
                        sendUpdateMessage(messenger, durationMessage);
                        break;
                    case "seekbarProgress":
                        messenger = intent.getParcelableExtra("seekbarProgress");
                        Object[] progressMessage = new Object[2];
                        progressMessage[0] = UPDATE_SEEKBAR_PROGRESS;
                        progressMessage[1] = mediaPlayer.getCurrentPosition();
                        sendUpdateMessage(messenger, progressMessage);
                        break;
                    case "seekbarSeek":
                        pauseCurrentPosition = intent.getIntExtra("seekbarSeek", 0);
                        mediaPlayer.seekTo(pauseCurrentPosition);
                        break;
                    case "musicListActivity":
                        musicListbundle = intent.getBundleExtra("musicListActivity");
                        messenger = (Messenger) musicListbundle.get("mainActivityMessenger");
                        Song song = (Song) musicListbundle.get("song");

                        // find the song uri and start playing the song
                        int songID = song.getID();
                        Uri audioURI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        Uri songURI = ContentUris.withAppendedId(audioURI, songID);
                        try {
                            mediaPlayer.reset();
                            mediaPlayer.setDataSource(this, songURI);
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                        }
                        catch (Exception e){
                            e.printStackTrace();
                        }
                        Object[] songMessage = new Object[2];
                        songMessage[0] = UPDATE_SONG;
                        songMessage[1] = song;
                        sendSongUpdateMessage(messenger, songMessage);
                        sendUpdateMessage(messenger, UPDATE_PAUSE);
                        break;
                    case "musicListNightToggle":
                        musicListbundle = intent.getBundleExtra("musicListNightToggle");
                        messenger = (Messenger) musicListbundle.get("mainActivityMessenger");
                        boolean nightMode = (boolean) musicListbundle.get("nightmode");
                        if (nightMode){
                            sendUpdateMessage(messenger, UPDATE_NIGHT);
                        }
                        else{
                            sendUpdateMessage(messenger, UPDATE_LIGHT);
                        }
                        break;
                }
            }
        }

        // Start Stick return means service will explicitly continue until user ends it
        return START_STICKY;

    }

    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Toast.makeText(this,
                        "Media Error: Not valid for Progressive Playback " + extra,
                        Toast.LENGTH_SHORT).show();
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Toast.makeText(this, "Media Error: Server Died!! " + extra,
                        Toast.LENGTH_SHORT).show();
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Toast.makeText(this, "Media Error: Unknown " + extra,
                        Toast.LENGTH_SHORT).show();

        }

        return false;
    }

    // sends an int message to the main thread
    private void sendUpdateMessage(Messenger messenger, int message) {
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();

        bundle.putInt("update", message);
        msg.setData(bundle);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // overloaded function to send a message containing an update and int
    private void sendUpdateMessage(Messenger messenger, Object[] message) {
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        int updateMessage = (int) message[0];
        int intMessage = (int) message[1];
        bundle.putInt("update", updateMessage);
        bundle.putInt("time", intMessage);
        msg.setData(bundle);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    // function to send a message containing a string and Song
    private void sendSongUpdateMessage(Messenger messenger, Object[] message) {
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        int updateMessage = (int) message[0];
        Song songMessage = (Song) message[1];
        bundle.putInt("update", updateMessage);
        bundle.putParcelable("song", songMessage);
        msg.setData(bundle);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    @TargetApi(26)
    // checks for audio focus before toggling media
    private void audioFocusToggleMedia(){
        int focusRequest = mAudioManager.requestAudioFocus(mAudioFocusRequest);
        switch (focusRequest) {
            case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                break;
            case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
                toggleMedia();
                break;
        }
    }

    private void toggleMedia() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(pauseCurrentPosition);
            mediaPlayer.start();
        }
        else if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            pauseCurrentPosition = mediaPlayer.getCurrentPosition();
        }

    }

    private void stopMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopMedia();
        stopSelf();

    }
}