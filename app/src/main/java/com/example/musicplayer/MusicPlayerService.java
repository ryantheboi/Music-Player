package com.example.musicplayer;

import android.annotation.TargetApi;
import android.app.Notification;
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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.widget.Toast;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MusicPlayerService
        extends Service
        implements OnCompletionListener, OnErrorListener {

    private static MediaPlayer mediaPlayer;
    static AudioManager mAudioManager;
    static AudioAttributes mAudioAttributes;
    static AudioFocusRequest mAudioFocusRequest;
    boolean mPlayOnAudioFocus;
    private static Messenger mainActivity_messenger;
    private static Notification notification;
    private HandlerThread musicPlayerHandlerThread;
    private PlaybackHandler playerHandler;
    private static boolean playing = false;
    private static int seekbar_position;
    private ScheduledExecutorService song_listener;
    private Song song_currentlyListening;
    private int song_secondsListened;
    private boolean song_isListened;

    public static final Uri artURI = Uri.parse("content://media/external/audio/albumart");

    public static final int UPDATE_PLAY = 0;
    public static final int UPDATE_PAUSE = 1;
    public static final int UPDATE_SEEKBAR_DURATION = 2;
    public static final int UPDATE_SEEKBAR_PROGRESS = 3;
    public static final int UPDATE_SONG = 4;
    public static final int UPDATE_SONG_PLAYED = 5;
    public static final int UPDATE_SONG_LISTENED = 6;

    private static final int SECONDS_LISTENED = 30;
    private static final int PREPARE_INIT_PAUSED = 0;
    private static final int PREPARE_INIT_PLAYING = 1;
    private static final int PREPARE_SONG = 2;
    private static final int PREPARE_PLAY = 3;
    private static final int PREPARE_PREV = 4;
    private static final int PREPARE_NEXT = 5;
    private static final int PREPARE_NEXT_PAUSE = 6;
    private static final int PREPARE_DURATION = 7;
    private static final int PREPARE_SEEK = 8;
    private static final int PREPARE_SEEKBAR_PROGRESS = 9;
    private static final int PREPARE_NOTIFICATION = 10;
    private static final int PREPARE_SONG_PLAYED = 11;
    private static final int PREPARE_SONG_LISTENED = 12;


    @Override
    @TargetApi(26)
    public void onCreate() {
        scheduleSongListener();
        Song current_song = MainActivity.getCurrent_song();
        if (current_song != Song.EMPTY_SONG) {
            // prepare mediaplayer for the current song
            int songID = current_song.getId();
            Uri audioURI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            Uri songURI = ContentUris.withAppendedId(audioURI, songID);
            mediaPlayer = MediaPlayer.create(this, songURI);
        }
        else{
            mediaPlayer = MediaPlayer.create(this, R.raw.aft);
        }

        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);

        musicPlayerHandlerThread = new HandlerThread("PlaybackHandler");
        musicPlayerHandlerThread.start();
        playerHandler = new PlaybackHandler(this, musicPlayerHandlerThread.getLooper());

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
            for (String key : b.keySet()) {
                switch (key){
                    case "musicListInitPaused":
                        mainActivity_messenger = intent.getParcelableExtra("musicListInitPaused");
                        playerHandler.removeMessages(PREPARE_INIT_PAUSED);
                        playerHandler.obtainMessage(PREPARE_INIT_PAUSED).sendToTarget();
                        break;
                    case "musicListInitPlaying":
                        mainActivity_messenger = intent.getParcelableExtra("musicListInitPlaying");
                        playerHandler.removeMessages(PREPARE_INIT_PLAYING);
                        playerHandler.obtainMessage(PREPARE_INIT_PLAYING).sendToTarget();
                        break;
                    case "pauseplay":
                        // update the pauseplay button icon via messenger and toggle music
                        playerHandler.removeMessages(PREPARE_PLAY);
                        playerHandler.obtainMessage(PREPARE_PLAY).sendToTarget();
                        break;
                    case "prev":
                        // change current song in main activity
                        playerHandler.removeMessages(PREPARE_PREV);
                        playerHandler.obtainMessage(PREPARE_PREV).sendToTarget();
                        break;
                    case "next":
                        // change current song in main activity
                        playerHandler.removeMessages(PREPARE_NEXT);
                        playerHandler.obtainMessage(PREPARE_NEXT).sendToTarget();
                        break;
                    case "seekbarDuration":
                        playerHandler.removeMessages(PREPARE_DURATION);
                        playerHandler.obtainMessage(PREPARE_DURATION).sendToTarget();
                        break;
                    case "seekbarProgress":
                        playerHandler.removeMessages(PREPARE_SEEKBAR_PROGRESS);
                        playerHandler.obtainMessage(PREPARE_SEEKBAR_PROGRESS).sendToTarget();
                        break;
                    case "seekbarSeek":
                        seekbar_position = intent.getIntExtra("seekbarSeek", 0);
                        playerHandler.removeMessages(PREPARE_SEEK);
                        playerHandler.obtainMessage(PREPARE_SEEK).sendToTarget();
                        break;
                    case "musicListSong":
                        playerHandler.removeMessages(PREPARE_SONG);
                        playerHandler.obtainMessage(PREPARE_SONG).sendToTarget();
                        break;
                    case "notification":
                        playerHandler.removeMessages(PREPARE_NOTIFICATION);
                        playerHandler.obtainMessage(PREPARE_NOTIFICATION).sendToTarget();
                        break;
                }
            }
        }

        // If the system kills the service after onStartCommand() returns,
        // do not recreate the service (which calls onStartCommand() with null intent)
        return START_NOT_STICKY;
    }

    @Override
    @TargetApi(26)
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }
        mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
        song_listener.shutdownNow();
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
     *  checks for audio focus before toggling media
     */
    @TargetApi(26)
    protected void audioFocusToggleMedia(){
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
            playing = true;

            Song curr_song = MainActivity.getCurrent_song();
            if (getSong_currentlyListening() == null || !getSong_currentlyListening().equals(curr_song)) {
                setSong_currentlyListening(curr_song);
                setSong_secondsListened(0);
                setSong_isListened(false);
                playerHandler.removeMessages(PREPARE_SONG_PLAYED);
                playerHandler.obtainMessage(PREPARE_SONG_PLAYED).sendToTarget();
            }
        }
        else if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playing = false;
        }
    }

    private void stopMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    private void scheduleSongListener() {
        // schedule song listener thread to execute every second
        song_listener = Executors.newSingleThreadScheduledExecutor();
        song_listener.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!getSong_isListened()) {
                    int seconds = getSong_secondsListened();
                    if (playing && seconds < SECONDS_LISTENED) {
                        setSong_secondsListened(seconds + 1);
                    } else if (seconds == SECONDS_LISTENED) {
                        setSong_isListened(true);
                        playerHandler.removeMessages(PREPARE_SONG_LISTENED);
                        playerHandler.obtainMessage(PREPARE_SONG_LISTENED).sendToTarget();
                    }
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private synchronized Song getSong_currentlyListening(){
        return song_currentlyListening;
    }


    private synchronized int getSong_secondsListened(){
        return song_secondsListened;
    }

    private synchronized boolean getSong_isListened(){
        return song_isListened;
    }

    /**
     * set the current song that is being listened
     * @param song the song currently being listened
     */
    private synchronized void setSong_currentlyListening(Song song){
        this.song_currentlyListening = song;
    }

    /**
     * set the number of seconds has the song been listened
     * @param seconds the number of seconds the song has been listened
     */
    private synchronized void setSong_secondsListened(int seconds){
        this.song_secondsListened = seconds;
    }

    /**
     * set whether the current song been listened to already
     * @param isListened true if already listened, false otherwise
     */
    private synchronized void setSong_isListened(boolean isListened){
        this.song_isListened = isListened;
    }

    /**
     * What to do when the current song finishes playing
     * Behavior depends on the repeat status from MainActivity
     * @param mp (unused) the mediaplayer object responsible for playing the song
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        switch(MainActivity.getRepeat_status()){
            // disable repeat
            case 0:
                Playlist curr_playlist = MainActivity.getCurrent_playlist();
                Song curr_song = MainActivity.getCurrent_song();
                int curr_playlist_size = curr_playlist.getSize();

                // if the current song is the last song in the playlist
                if (curr_playlist.getSongList().indexOf(curr_song) == curr_playlist_size - 1){
                    playerHandler.removeMessages(PREPARE_NEXT_PAUSE);
                    playerHandler.obtainMessage(PREPARE_NEXT_PAUSE).sendToTarget();
                }
                else{
                    playerHandler.removeMessages(PREPARE_NEXT);
                    playerHandler.obtainMessage(PREPARE_NEXT).sendToTarget();
                }
                break;

            // repeat playlist
            case 1:
                playerHandler.removeMessages(PREPARE_NEXT);
                playerHandler.obtainMessage(PREPARE_NEXT).sendToTarget();
                break;

            // repeat one song
            case 2:
                playerHandler.removeMessages(PREPARE_SONG);
                playerHandler.obtainMessage(PREPARE_SONG).sendToTarget();

                // reset song listener and consider the song played
                setSong_currentlyListening(MainActivity.getCurrent_song());
                playerHandler.removeMessages(PREPARE_SONG_PLAYED);
                playerHandler.obtainMessage(PREPARE_SONG_PLAYED).sendToTarget();
                if (getSong_secondsListened() == SECONDS_LISTENED) {
                    setSong_secondsListened(0);
                    setSong_isListened(false);
                }
                break;
        }
    }

    private final class PlaybackHandler extends Handler {
        private MusicPlayerService mService;

        public PlaybackHandler(final MusicPlayerService servicer, Looper looper) {
            super(looper);
            mService = servicer;
        }

        /**
         * sends an int message to the main thread
         * @param messenger the messenger to send update to
         * @param message the update code
         * @param updateDatabase true if this will push an update to the database, false otherwise
         * @param updateSeek the seek position to update the database (usually for paused next or prev song), -1 to ignore this
         */
        private void sendUpdateMessage(Messenger messenger, int message, boolean updateDatabase, int updateSeek) {
            Message msg = Message.obtain();
            Bundle bundle = new Bundle();

            bundle.putInt("update", message);
            bundle.putBoolean("updateDatabase", updateDatabase);
            bundle.putInt("updateSeek", updateSeek);
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
         * @param message the update code
         * @param song the song to update the messenger's activity with
         */
        private void sendSongUpdateMessage(Messenger messenger, int message, final Song song) {
            // find the song uri and start playing the song
            Message msg = Message.obtain();
            Bundle bundle = new Bundle();
            bundle.putInt("update", message);
            bundle.putParcelable("song", song);
            msg.setData(bundle);
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        /**
         * Recreates the mediaplayer with a new song URI by releasing the old one and creating anew
         * @param songId the id of the new song to create the new mediaplayer
         */
        private void recreateMediaPlayer(int songId){
            // release current mediaplayer to allow another to be created
            mediaPlayer.release();

            // prepare next song and keep it paused at seek position 0
            Uri audioURI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            Uri songURI = ContentUris.withAppendedId(audioURI, songId);
            MediaPlayer mp = MediaPlayer.create(mService, songURI);
            mp.setOnCompletionListener(mService);
            mp.setOnErrorListener(mService);
            mp.setWakeMode(mService, PowerManager.PARTIAL_WAKE_LOCK);
            mediaPlayer = mp;
        }

        @Override
        public void handleMessage(final Message msg) {
            final MusicPlayerService service = mService;
            if (service == null) {
                return;
            }
            switch (msg.what) {
                case PREPARE_INIT_PAUSED:
                    try {
                        // update main ui with current song
                        Song current_song = MainActivity.getCurrent_song();
                        if (current_song != Song.EMPTY_SONG) {
                            sendSongUpdateMessage(mainActivity_messenger, UPDATE_SONG, current_song);

                            // recreate mediaplayer for current song, but keep paused
                            recreateMediaPlayer(current_song.getId());
                            sendUpdateMessage(mainActivity_messenger, UPDATE_PLAY, false, -1);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case PREPARE_INIT_PLAYING:
                    try {
                        // update main ui with current song, which is still playing and being listened
                        Song current_song = MainActivity.getCurrent_song();
                        setSong_currentlyListening(current_song);
                        if (current_song != Song.EMPTY_SONG) {
                            sendSongUpdateMessage(mainActivity_messenger, UPDATE_SONG, current_song);
                            sendUpdateMessage(mainActivity_messenger, UPDATE_PAUSE, false, -1);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case PREPARE_SONG:
                    try {
                        // enable playback in background
                        mService.startForeground(1, notification);

                        // update main ui with current song
                        Song current_song = MainActivity.getCurrent_song();
                        sendSongUpdateMessage(mainActivity_messenger, UPDATE_SONG, current_song);

                        // recreate mediaplayer and play current song
                        recreateMediaPlayer(current_song.getId());
                        audioFocusToggleMedia();
                        sendUpdateMessage(mainActivity_messenger, UPDATE_PAUSE, true, -1);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case PREPARE_PLAY:
                    if (playing) {
                        // disable playback in background and update with current position
                        sendUpdateMessage(mainActivity_messenger, UPDATE_PLAY, true, mediaPlayer.getCurrentPosition());
                        mService.stopForeground(false);
                    }
                    else{
                        // enable playback in background
                        sendUpdateMessage(mainActivity_messenger, UPDATE_PAUSE, true, -1);
                        mService.startForeground(1, notification);
                    }
                    audioFocusToggleMedia();
                    break;
                case PREPARE_PREV:
                    if (MainActivity.getCurrent_playlist().getSize() > 0) {
                        try {
                            // update main ui with prev song
                            Song prev_song = MainActivity.getCurrent_playlist().getPrevSong();
                            sendSongUpdateMessage(mainActivity_messenger, UPDATE_SONG, prev_song);

                            // recreate mp and play prev song only if mediaplayer was playing before
                            recreateMediaPlayer(prev_song.getId());
                            if (playing) {
                                sendUpdateMessage(mainActivity_messenger, UPDATE_PAUSE, true, -1);
                                audioFocusToggleMedia();
                            } else {
                                sendUpdateMessage(mainActivity_messenger, UPDATE_PLAY, true, 0);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case PREPARE_NEXT:
                    if (MainActivity.getCurrent_playlist().getSize() > 0) {
                        try {
                            // update main ui with next song
                            Song next_song = MainActivity.getCurrent_playlist().getNextSong();
                            sendSongUpdateMessage(mainActivity_messenger, UPDATE_SONG, next_song);

                            // recreate mp and play next song only if mediaplayer was playing before
                            recreateMediaPlayer(next_song.getId());
                            if (playing) {
                                sendUpdateMessage(mainActivity_messenger, UPDATE_PAUSE, true, -1);
                                audioFocusToggleMedia();
                            } else {
                                sendUpdateMessage(mainActivity_messenger, UPDATE_PLAY, true, 0);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case PREPARE_NEXT_PAUSE:
                    if (MainActivity.getCurrent_playlist().getSize() > 0) {
                        try {
                            // update main ui with next song
                            Song next_song = MainActivity.getCurrent_playlist().getNextSong();
                            sendSongUpdateMessage(mainActivity_messenger, UPDATE_SONG, next_song);

                            // recreate mp for next song and keep it paused at seek position 0
                            recreateMediaPlayer(next_song.getId());
                            playing = false;
                            sendUpdateMessage(mainActivity_messenger, UPDATE_PLAY, true, 0);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case PREPARE_DURATION:
                    Object[] durationMessage = new Object[2];
                    durationMessage[0] = UPDATE_SEEKBAR_DURATION;
                    durationMessage[1] = mediaPlayer.getDuration();
                    sendUpdateMessage(mainActivity_messenger, durationMessage);
                    break;
                case PREPARE_SEEK:
                    mediaPlayer.seekTo(seekbar_position);
                    break;
                case PREPARE_SEEKBAR_PROGRESS:
                    Object[] progressMessage = new Object[2];
                    progressMessage[0] = UPDATE_SEEKBAR_PROGRESS;
                    progressMessage[1] = mediaPlayer.getCurrentPosition();
                    sendUpdateMessage(mainActivity_messenger, progressMessage);
                    break;
                case PREPARE_NOTIFICATION:
                    notification = MainActivity.getNotification();
                    break;
                case PREPARE_SONG_PLAYED:
                    sendSongUpdateMessage(mainActivity_messenger, UPDATE_SONG_PLAYED, getSong_currentlyListening());
                    break;
                case PREPARE_SONG_LISTENED:
                    sendSongUpdateMessage(mainActivity_messenger, UPDATE_SONG_LISTENED, getSong_currentlyListening());
                    break;
            }
        }
    }
}