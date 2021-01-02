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
    private Messenger mainActivity_messenger;
    private Messenger musicList_messenger;

    public static final Uri artURI = Uri.parse("content://media/external/audio/albumart");

    public static final int UPDATE_PLAY = 0;
    public static final int UPDATE_PAUSE = 1;
    public static final int UPDATE_SEEKBAR_DURATION = 2;
    public static final int UPDATE_SEEKBAR_PROGRESS = 3;
    public static final int UPDATE_SONG = 4;
    public static final int UPDATE_NIGHT = 5;
    public static final int UPDATE_LIGHT = 6;
    public static final int UPDATE_MESSENGER_MUSICLISTACTIVITY = 7;
    public static final int UPDATE_HIGHLIGHT = 8;



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
            Bundle bundle_extra;
            for (String key : b.keySet()) {
                //System.out.println(key);
                switch (key){
                    case "pauseplay":
                        // update the pauseplay button icon via messenger and toggle music
                        mainActivity_messenger = intent.getParcelableExtra("pauseplay");
                        if (mediaPlayer.isPlaying()) {
                            sendUpdateMessage(mainActivity_messenger, UPDATE_PLAY);
                        }
                        else{
                            sendUpdateMessage(mainActivity_messenger, UPDATE_PAUSE);
                        }
                        audioFocusToggleMedia();
                        break;
                    case "prev":
                        bundle_extra = intent.getBundleExtra("prev");
                        mainActivity_messenger = bundle_extra.getParcelable("mainActivityMessenger");
                        musicList_messenger = bundle_extra.getParcelable("musicListActivityMessenger");
                        if (MusicListActivity.playlist != null){
                            // change current song in main activity
                            Song current_song = MusicListActivity.getCurrent_song();
                            SongNode songNode = MusicListActivity.playlist.get(current_song);
                            Song prev_song = songNode.getPrev();
                            if (mediaPlayer.isPlaying()) { // keep it playing
                                sendSongUpdateMessage(mainActivity_messenger, prev_song);
                                sendUpdateMessage(mainActivity_messenger, UPDATE_PAUSE);
                            }
                            else{ // keep it paused
                                sendSongUpdateMessage(mainActivity_messenger, prev_song);
                                mediaPlayer.pause();
                                sendUpdateMessage(mainActivity_messenger, UPDATE_PLAY);
                            }

                            // change highlighted song from list view
                            sendUpdateMessage(musicList_messenger, UPDATE_HIGHLIGHT);
                        }
                        else {
                            Toast.makeText(this, "received notification: prev", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "next":
                        bundle_extra = intent.getBundleExtra("next");
                        mainActivity_messenger = bundle_extra.getParcelable("mainActivityMessenger");
                        musicList_messenger = bundle_extra.getParcelable("musicListActivityMessenger");
                        if (MusicListActivity.playlist != null){
                            // change current song in main activity
                            Song current_song = MusicListActivity.getCurrent_song();
                            SongNode songNode = MusicListActivity.playlist.get(current_song);
                            Song next_song = songNode.getNext();
                            if (mediaPlayer.isPlaying()) { // keep it playing
                                sendSongUpdateMessage(mainActivity_messenger, next_song);
                                sendUpdateMessage(mainActivity_messenger, UPDATE_PAUSE);
                            }
                            else{ // keep it paused
                                sendSongUpdateMessage(mainActivity_messenger, next_song);
                                mediaPlayer.pause();
                                sendUpdateMessage(mainActivity_messenger, UPDATE_PLAY);
                            }

                            // change highlighted song from list view
                            sendUpdateMessage(musicList_messenger, UPDATE_HIGHLIGHT);
                        }
                        else {
                            Toast.makeText(this, "received notification: next", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "notificationPrev":
                        mainActivity_messenger = intent.getParcelableExtra("notificationPrev");
                        if (MusicListActivity.playlist != null){
                            // change current song in main activity
                            Song current_song = MusicListActivity.getCurrent_song();
                            SongNode songNode = MusicListActivity.playlist.get(current_song);
                            Song next_song = songNode.getPrev();
                            if (mediaPlayer.isPlaying()) { // keep it playing
                                sendSongUpdateMessage(mainActivity_messenger, next_song);
                                sendUpdateMessage(mainActivity_messenger, UPDATE_PAUSE);
                            }
                            else{ // keep it paused
                                sendSongUpdateMessage(mainActivity_messenger, next_song);
                                mediaPlayer.pause();
                                sendUpdateMessage(mainActivity_messenger, UPDATE_PLAY);
                            }

                            // change highlighted song from list view (if it exists)
                            if (musicList_messenger != null) {
                                sendUpdateMessage(musicList_messenger, UPDATE_HIGHLIGHT);
                            }
                        }
                        else {
                            Toast.makeText(this, "received notification: prev", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "notificationNext":
                        mainActivity_messenger = intent.getParcelableExtra("notificationNext");
                        if (MusicListActivity.playlist != null){
                            // change current song in main activity
                            Song current_song = MusicListActivity.getCurrent_song();
                            SongNode songNode = MusicListActivity.playlist.get(current_song);
                            Song next_song = songNode.getNext();
                            if (mediaPlayer.isPlaying()) { // keep it playing
                                sendSongUpdateMessage(mainActivity_messenger, next_song);
                                sendUpdateMessage(mainActivity_messenger, UPDATE_PAUSE);
                            }
                            else{ // keep it paused
                                sendSongUpdateMessage(mainActivity_messenger, next_song);
                                mediaPlayer.pause();
                                sendUpdateMessage(mainActivity_messenger, UPDATE_PLAY);
                            }

                            // change highlighted song from list view (if it exists)
                            if (musicList_messenger != null) {
                                sendUpdateMessage(musicList_messenger, UPDATE_HIGHLIGHT);
                            }
                        }
                        else {
                            Toast.makeText(this, "received notification: next", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "seekbarDuration":
                        mainActivity_messenger = intent.getParcelableExtra("seekbarDuration");
                        Object[] durationMessage = new Object[2];
                        durationMessage[0] = UPDATE_SEEKBAR_DURATION;
                        durationMessage[1] = mediaPlayer.getDuration();
                        sendUpdateMessage(mainActivity_messenger, durationMessage);
                        break;
                    case "seekbarProgress":
                        mainActivity_messenger = intent.getParcelableExtra("seekbarProgress");
                        Object[] progressMessage = new Object[2];
                        progressMessage[0] = UPDATE_SEEKBAR_PROGRESS;
                        progressMessage[1] = mediaPlayer.getCurrentPosition();
                        sendUpdateMessage(mainActivity_messenger, progressMessage);
                        break;
                    case "seekbarSeek":
                        int seekbar_position = intent.getIntExtra("seekbarSeek", 0);
                        mediaPlayer.seekTo(seekbar_position);
                        break;
                    case "musicListActivity":
                        bundle_extra = intent.getBundleExtra("musicListActivity");
                        mainActivity_messenger = (Messenger) bundle_extra.get("mainActivityMessenger");
                        Song song = (Song) bundle_extra.get("song");

                        sendSongUpdateMessage(mainActivity_messenger, song);
                        sendUpdateMessage(mainActivity_messenger, UPDATE_PAUSE);
                        break;
                    case "musicListMessenger":
                        bundle_extra = intent.getBundleExtra("musicListMessenger");
                        mainActivity_messenger = (Messenger) bundle_extra.get("mainActivityMessenger");
                        musicList_messenger = (Messenger) bundle_extra.get("musicListActivityMessenger");
                        sendMessengerUpdateMessage(mainActivity_messenger, musicList_messenger, UPDATE_MESSENGER_MUSICLISTACTIVITY);
                        break;
                    case "musicListNightToggle":
                        bundle_extra = intent.getBundleExtra("musicListNightToggle");
                        mainActivity_messenger = (Messenger) bundle_extra.get("mainActivityMessenger");
                        boolean nightMode = (boolean) bundle_extra.get("nightmode");
                        if (nightMode){
                            sendUpdateMessage(mainActivity_messenger, UPDATE_NIGHT);
                        }
                        else{
                            sendUpdateMessage(mainActivity_messenger, UPDATE_LIGHT);
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
        stopSelf();
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

    /**
     * sends an int message to the main thread
     * @param messenger the messenger to send update to
     * @param message the update code
     */
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

    /**
     * overloaded method to send a message containing an update and int
     * @param messenger the messenger to send update to
     * @param message 2-part message containing update code and seekbar time
     */
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

    /**
     * send a message containing a string and Song
     * @param messenger the messenger to send update to
     * @param song the song to update the messenger's activity with
     */
    private void sendSongUpdateMessage(Messenger messenger, Song song) {
        // find the song uri and start playing the song
        int songID = song.getID();
        Uri audioURI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri songURI = ContentUris.withAppendedId(audioURI, songID);
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, songURI);
            mediaPlayer.prepare();
            audioFocusToggleMedia();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt("update", UPDATE_SONG);
        bundle.putParcelable("song", song);
        msg.setData(bundle);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * send a message to the messenger's activity to update its messenger
     * @param messenger - the messenger to receive the update messenger from another activity
     * @param update - the messenger from the other activity
     * @param message - the update code
     */
    private void sendMessengerUpdateMessage(Messenger messenger, Messenger update, int message){
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();

        bundle.putInt("update", message);
        bundle.putParcelable("messenger", update);
        msg.setData(bundle);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    /**
     *  checks for audio focus before toggling media
     */
    @TargetApi(26)
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
            mediaPlayer.start();
        }
        else if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }

    }

    private void stopMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    /**
     * what to do when the current song finishes playing
     * @param mp (unused) the mediaplayer object responsible for playing the song
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        if (MusicListActivity.playlist != null){
            // change current song in main activity
            Song current_song = MusicListActivity.getCurrent_song();
            SongNode songNode = MusicListActivity.playlist.get(current_song);
            Song next_song = songNode.getNext();

            // notify main activity of the next song and keep it playing
            if (mainActivity_messenger != null) {
                sendSongUpdateMessage(mainActivity_messenger, next_song);
                sendUpdateMessage(mainActivity_messenger, UPDATE_PAUSE);
            }

            // change highlighted song from list view
            if (musicList_messenger != null) {
                sendUpdateMessage(musicList_messenger, UPDATE_HIGHLIGHT);
            }
        }
    }
}