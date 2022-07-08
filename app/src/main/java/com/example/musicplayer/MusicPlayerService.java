package com.example.musicplayer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.media.MediaBrowserServiceCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.media.session.MediaButtonReceiver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MusicPlayerService
        extends MediaBrowserServiceCompat
implements OnCompletionListener, OnErrorListener {

    private static final String MEDIA_ROOT_ID = "media_root_id";
    private static final String EMPTY_MEDIA_ROOT_ID = "empty_root_id";

    private MediaSessionCompat mediaSession;
    private MediaMetadataCompat.Builder metadataBuilder;
    private PlaybackStateCompat.Builder playbackStateBuilder;
    private MusicPlayerSessionCallback mediaPlayerSessionCallback;
    private final IntentFilter noisyIntentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private final MusicPlayerNoisyReceiver noisyAudioStreamReceiver = new MusicPlayerNoisyReceiver();
    private final MusicPlayerBluetoothReceiver bluetoothReceiver = new MusicPlayerBluetoothReceiver();
    private HashSet<BluetoothDevice> bluetoothConnectedDevices = new HashSet<>();
    private BluetoothProfile bluetoothProxy;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothConnectedDevice;
    private BluetoothProfile.ServiceListener bluetoothServiceListener;
    private static MediaPlayer mediaPlayer;
    private static AudioManager mAudioManager;
    private static AudioAttributes mAudioAttributes;
    private static AudioFocusRequest mAudioFocusRequest;
    private boolean mPlayOnAudioFocus;
    private static Messenger mainActivity_messenger;
    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private PendingIntent notificationPause_intent;
    private PendingIntent notificationPlay_intent;
    private PendingIntent notificationNext_intent;
    private PendingIntent notificationPrev_intent;
    private HandlerThread musicPlayerHandlerThread;
    private PlaybackHandler playerHandler;
    private static boolean continuePlaying = false;
    private ScheduledExecutorService song_listener;
    private ScheduledExecutorService progress_listener;
    private Future<?> song_listener_future;
    private Future<?> progress_listener_future;
    private Song song_currentlyListening;
    private int song_secondsListened;
    private boolean song_isListened;
    private int song_progress;
    private final IBinder binder = new LocalBinder();

    public static final Uri artURI = Uri.parse("content://media/external/audio/albumart");
    private static final int SECONDS_LISTENED = 30;
    private static final String MEDIABROWSER_SERVICE = "android.media.browse.MediaBrowserService";

    public static final int UPDATE_HANDSHAKE = 0;
    public static final int UPDATE_SEEKBAR_PROGRESS = 1;
    public static final int UPDATE_SONG_INDEX = 2;
    public static final int UPDATE_SONG_PLAYED = 3;
    public static final int UPDATE_SONG_LISTENED = 4;
    public static final int UPDATE_BLUETOOTH_CONNECTED = 5;
    public static final int UPDATE_BLUETOOTH_DISCONNECTED = 6;

    public static final String CUSTOM_ACTION_PLAY_SONG = "play_song";

    private static final int PREPARE_HANDSHAKE = 0;
    private static final int PREPARE_SONG_PLAYED = 2;
    private static final int PREPARE_SONG_LISTENED = 3;

    private static final int NOTIFICATION_PLAY = 0;
    private static final int NOTIFICATION_PAUSE = 1;
    private static final int NOTIFICATION_NEXT = 2;
    private static final int NOTIFICATION_PREV = 3;
    private static final int NOTIFICATION_CUSTOM_PLAY_SONG = 4;

    private ServiceConnection musicPlayerServiceConnection;
    private MusicPlayerService musicPlayerService;
    private boolean isBound = false;

    @Override
    @TargetApi(26)
    public void onCreate() {
        super.onCreate();
        try {
            // init listeners
            song_listener = Executors.newSingleThreadScheduledExecutor();
            progress_listener = Executors.newSingleThreadScheduledExecutor();
            setSongListenerActive(true);
            setProgressListenerActive(true);

            initBluetooth();

            musicPlayerHandlerThread = new HandlerThread("PlaybackHandler");
            musicPlayerHandlerThread.start();
            playerHandler = new PlaybackHandler(this, musicPlayerHandlerThread.getLooper());

            mediaSession = new MediaSessionCompat(MusicPlayerService.this, "MusicPlayerService-MediaSession");

            // enable callbacks from MediaButtons and TransportControls
            mediaSession.setFlags(
                    MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

            // init mediametadata builder with default metadata keys and values
            metadataBuilder = new MediaMetadataCompat.Builder();
            Song current_song = MainActivity.getCurrent_song();
            if (current_song.equals(Song.EMPTY_SONG)) {
                metadataBuilder
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, Song.EMPTY_SONG.getTitle())
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, Song.EMPTY_SONG.getArtist())
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, Song.EMPTY_SONG.getAlbum())
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, String.valueOf(R.drawable.default_albumart))
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeResource(getResources(), R.drawable.default_albumart))
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, Song.EMPTY_SONG.getDuration());
                mediaSession.setMetadata(metadataBuilder.build());
            }
            else{
                // use existing song to set media session metadata
                setMediaSessionMetadata(current_song);
            }

            // init playbackstate builder with supported actions
            playbackStateBuilder = new PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PLAY |
                            PlaybackStateCompat.ACTION_PAUSE |
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                            PlaybackStateCompat.ACTION_SEEK_TO |
                            PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE |
                            PlaybackStateCompat.ACTION_SET_REPEAT_MODE |
                            PlaybackStateCompat.ACTION_STOP);
            mediaSession.setPlaybackState(playbackStateBuilder.build());

            // MusicPlayerSessionCallback() has methods that handle callbacks from a media controller
            mediaPlayerSessionCallback = new MusicPlayerSessionCallback(this);
            mediaSession.setCallback(mediaPlayerSessionCallback, playerHandler);

            // set the session's token so that client activities can communicate with it.
            setSessionToken(mediaSession.getSessionToken());

            initAudioFocus();
        }catch (Exception e){
            Logger.logException(e, "MusicPlayerService");
        }
    }

    @TargetApi(26)
    public int onStartCommand(Intent intent, int flags, int startId) {

        // correct callbacks to MediaSessionCompat.Callback will be triggered based on the incoming KeyEvent
        MediaButtonReceiver.handleIntent(mediaSession, intent);

        // begin responding to the messenger based on message received
        try {
            Bundle b = intent.getExtras();
            if (b != null) {
                for (String key : b.keySet()) {
                    switch (key) {
                        case "handshake":
                            // receive messenger from main activity
                            mainActivity_messenger = intent.getParcelableExtra("handshake");
                            playerHandler.removeMessages(PREPARE_HANDSHAKE);
                            playerHandler.obtainMessage(PREPARE_HANDSHAKE).sendToTarget();
                            break;
                    }
                }
            }
        }catch (Exception e){
            Logger.logException(e, "MusicPlayerService");
        }

        // If the system kills the service after onStartCommand() returns,
        // do not recreate the service (which calls onStartCommand() with null intent)
        return START_NOT_STICKY;
    }

    @Override
    @TargetApi(26)
    public void onDestroy() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
            mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);
            song_listener.shutdownNow();
            progress_listener.shutdownNow();
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothProxy);
            unregisterReceiver(bluetoothReceiver);
            doUnbindService();
            stopSelf();
            super.onDestroy();
        }catch (Exception e){
            Logger.logException(e, "MusicPlayerService");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (intent != null && MEDIABROWSER_SERVICE.equals(intent.getAction())) {
            // for Android auto, need to call super, or onGetRoot won't be called.
            return super.onBind(intent);
        }
        else if (intent != null && BLUETOOTH_SERVICE.equals(intent.getAction())){
            return binder;
        }
        return binder;
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot(MEDIA_ROOT_ID, null);
    }

    /*
      Implementations must call result.sendResult with the list of children.
      If loading the children will be an expensive operation that should be performed on another thread,
      result.detach may be called before returning from this function,
      and then result.sendResult called when the loading is complete.
     */
    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(new ArrayList<>());
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
     * What to do when the current song finishes playing
     * Behavior depends on the repeat mode from MainActivity
     * @param mp (unused) the mediaplayer object responsible for playing the song
     */
    @Override
    public void onCompletion(MediaPlayer mp) {
        try {
            switch (MainActivity.getRepeat_mode()) {
                case PlaybackStateCompat.REPEAT_MODE_NONE:
                    Playlist curr_playlist = MainActivity.getCurrent_playlist();
                    Song curr_song = MainActivity.getCurrent_song();
                    int curr_playlist_size = curr_playlist.getSize();

                    // if the current song is the last song in the playlist
                    if (curr_playlist.getSongList().indexOf(curr_song) == curr_playlist_size - 1) {
                        mediaSession.getController().getTransportControls().skipToNext();
                        mediaSession.getController().getTransportControls().pause();
                    } else {
                        mediaSession.getController().getTransportControls().skipToNext();
                    }
                    break;
                case PlaybackStateCompat.REPEAT_MODE_ONE:
                    mediaPlayer.seekTo(0);
                    mediaPlayer.start();

                    // reset song listener and consider the song played
                    setSong_currentlyListening(MainActivity.getCurrent_song());
                    playerHandler.removeMessages(PREPARE_SONG_PLAYED);
                    playerHandler.obtainMessage(PREPARE_SONG_PLAYED).sendToTarget();
                    if (getSong_secondsListened() == SECONDS_LISTENED) {
                        setSong_secondsListened(0);
                        setSong_isListened(false);
                    }
                    break;
                case PlaybackStateCompat.REPEAT_MODE_ALL:
                    mediaSession.getController().getTransportControls().skipToNext();
                    break;
            }
        }catch (Exception e){
            Logger.logException(e, "MusicPlayerService");
        }
    }

    /**
     * Update the notification builder with appropriate changes depending on the action being performed
     * @param notificationAction the action being performed in this media session
     */
    @SuppressLint("RestrictedApi")
    private void updateNotificationBuilder(int notificationAction){
        notificationManager = NotificationManagerCompat.from(this);

        switch(notificationAction){
            case NOTIFICATION_PLAY:
                // initialize notification builder and appropriate intents for the first time
                if (notificationBuilder == null) {
                    initNotificationBuilder();
                }

                // update notification pause/play button
                notificationBuilder
                        .setOngoing(true)
                        .mActions.set(1, new NotificationCompat.Action(
                                R.drawable.ic_pause24dp, getString(R.string.Pause),
                                notificationPause_intent));
                break;
            case NOTIFICATION_PAUSE:
                if (notificationBuilder != null) {
                    // update notification pause/play button
                    notificationBuilder
                            .setOngoing(false)
                            .mActions.set(1, new NotificationCompat.Action(
                                    R.drawable.ic_play24dp, getString(R.string.Play),
                                    notificationPlay_intent));
                    notificationManager.notify(1, notificationBuilder.build());
                }
                break;
            case NOTIFICATION_NEXT:
            case NOTIFICATION_PREV:
                if (notificationBuilder != null) {
                    // update notifications with new song
                    MediaControllerCompat controller = mediaSession.getController();
                    MediaMetadataCompat mediaMetadata = controller.getMetadata();
                    MediaDescriptionCompat description = mediaMetadata.getDescription();
                    notificationBuilder
                            // add the metadata for the currently playing track
                            .setContentTitle(description.getTitle())
                            .setContentText(description.getSubtitle())
                            .setLargeIcon(description.getIconBitmap());
                }
                break;
            case NOTIFICATION_CUSTOM_PLAY_SONG:
                // initialize notification builder and appropriate intents for the first time
                if (notificationBuilder == null) {
                    initNotificationBuilder();

                    // update notification pause/play button
                    notificationBuilder
                            .setOngoing(true)
                            .mActions.set(1, new NotificationCompat.Action(
                                    R.drawable.ic_pause24dp, getString(R.string.Pause),
                                    notificationPause_intent));
                }

                else {
                    // update notifications with new song
                    MediaControllerCompat controller = mediaSession.getController();
                    MediaMetadataCompat mediaMetadata = controller.getMetadata();
                    MediaDescriptionCompat description = mediaMetadata.getDescription();
                    notificationBuilder
                            // add the metadata for the currently playing track
                            .setContentTitle(description.getTitle())
                            .setContentText(description.getSubtitle())
                            .setLargeIcon(description.getIconBitmap())

                            // update notification pause/play button
                            .setOngoing(true)
                            .mActions.set(1, new NotificationCompat.Action(
                                    R.drawable.ic_pause24dp, getString(R.string.Pause),
                                    notificationPause_intent));
                }
                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void initAudioFocus(){
        mAudioManager = (AudioManager) MusicPlayerService.this.getSystemService(Context.AUDIO_SERVICE);

        // request audio focus for playback, this registers the afChangeListener
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

    private void initBluetooth(){
        // define callbacks for music player service binding, passed to bindService()
        musicPlayerServiceConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                // bound to MusicPlayerService, cast the IBinder and get service instance
                MusicPlayerService.LocalBinder binder = (MusicPlayerService.LocalBinder) service;
                musicPlayerService = binder.getService();
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                musicPlayerService = null;
            }
        };

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothServiceListener = new BluetoothProfile.ServiceListener() {

            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                bluetoothProxy = proxy;
                List<BluetoothDevice> connectedDevices = proxy.getConnectedDevices();
                if (connectedDevices.size() > 0){
                    bluetoothConnectedDevices.addAll(proxy.getConnectedDevices());
                    bluetoothConnectedDevice = connectedDevices.get(0);

                    // bind to prevent system from destroying service while device still connected
                    doBindService();

                    // notify main activity that bluetooth device(s) already connected
                    sendUpdateMessage(mainActivity_messenger, UPDATE_BLUETOOTH_CONNECTED);
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                bluetoothProxy = null;
                doUnbindService();
            }
        };

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
        }

        else if (bluetoothAdapter.isEnabled()) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            registerReceiver(bluetoothReceiver, filter);
        }
        else{
            // enable Bluetooth through the system settings
//                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * Bind this service to prevent it from being destroyed by system
     */
    protected void doBindService(){
        Intent bluetooth_intent = new Intent(MusicPlayerService.this, MusicPlayerService.class);
        bluetooth_intent.setAction(BLUETOOTH_SERVICE);

        // attempt to establish a connection with the service
        // isBound will remain false if an error occurred
        if (bindService(bluetooth_intent, musicPlayerServiceConnection, Context.BIND_AUTO_CREATE)) {
            isBound = true;
        }
    }

    protected void doUnbindService(){
        if (isBound) {
            // release information about the service's state
            unbindService(musicPlayerServiceConnection);
            isBound = false;
        }
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
            continuePlaying = true;

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
            continuePlaying = false;
        }
    }

    private void stopMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    /**
     * Sets or stops the listener for counting the seconds until a song is considered to be listened
     * @param isActive true to start the listener, false to cancel the listener
     */
    private void setSongListenerActive(boolean isActive) {
        // schedule song listener thread to execute every second
        if (isActive) {
            song_listener_future = song_listener.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (!getSong_isListened()) {
                        int seconds = getSong_secondsListened();
                        if (continuePlaying && seconds < SECONDS_LISTENED) {
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
        else {
            // stop the listener and interrupt any work currently being done
            if (song_listener_future != null) {
                song_listener_future.cancel(true);
            }
        }
    }

    /**
     * Sets or stops the listener that updates Playback state with the mediaplayer's progress
     * @param isActive true to start the listener, false to cancel the listener
     */
    private void setProgressListenerActive(boolean isActive) {
        // schedule progress listener thread to execute every fifth of a second
        if (isActive) {
            progress_listener_future = progress_listener.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (continuePlaying) {
                        song_progress = mediaPlayer.getCurrentPosition();
                        setMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING, song_progress);
                    }
                }
            }, 0, 200, TimeUnit.MILLISECONDS);
        }
        else {
            // stop the listener and interrupt any work currently being done
            if (progress_listener_future != null) {
                progress_listener_future.cancel(true);
            }
        }
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
     * Initialize mediaplayer for the first time with the current song (if exists)
     * If current song doesn't exist, initialize with default song
     */
    private void initMediaPlayer(){
        // create mediaplayer with current song, if it already exists
        Song current_song = MainActivity.getCurrent_song();

        if (!current_song.equals(Song.EMPTY_SONG)){
            setMediaSessionMetadata(current_song);
            Uri audioURI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            Uri songURI = ContentUris.withAppendedId(audioURI, current_song.getId());
            mediaPlayer = MediaPlayer.create(MusicPlayerService.this, songURI);
        }
        else {
            mediaPlayer = MediaPlayer.create(MusicPlayerService.this, R.raw.aft);
        }
        mediaPlayer.setOnCompletionListener(MusicPlayerService.this);
        mediaPlayer.setOnErrorListener(MusicPlayerService.this);

        // keep CPU from sleeping and be able to play music with screen off
        mediaPlayer.setWakeMode(MusicPlayerService.this, PowerManager.PARTIAL_WAKE_LOCK);

        // seek to the progress that was saved before
        mediaPlayer.seekTo(song_progress);
    }

    /**
     * Initialize notification builder for the first time with proper intents, current song data,
     * and other necessary configurations that will remain for every notification update
     */
    private void initNotificationBuilder(){
        notificationPlay_intent = MediaButtonReceiver.buildMediaButtonPendingIntent(MusicPlayerService.this,
                PlaybackStateCompat.ACTION_PLAY);
        notificationPause_intent = MediaButtonReceiver.buildMediaButtonPendingIntent(MusicPlayerService.this,
                PlaybackStateCompat.ACTION_PAUSE);
        notificationNext_intent = MediaButtonReceiver.buildMediaButtonPendingIntent(MusicPlayerService.this,
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
        notificationPrev_intent = MediaButtonReceiver.buildMediaButtonPendingIntent(MusicPlayerService.this,
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);

        // init intent for launching main activity from notifications (or resuming if not destroyed)
        Intent mainActivityIntent = new Intent(MusicPlayerService.this, MainActivity.class);
        mainActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent notificationIntent = PendingIntent.getActivity(MusicPlayerService.this, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE);

        // update notification builder with current song
        Song song = MainActivity.getCurrent_song();

        notificationBuilder = new NotificationCompat.Builder(MusicPlayerService.this, Notifications.CHANNEL_ID_1);
        notificationBuilder
                // Add the metadata for the currently playing track
                .setContentTitle(song.getTitle())
                .setContentText(song.getArtist())
                .setLargeIcon(Song.getAlbumArtBitmap(this, song.getAlbumID()))

                // launch music player by clicking the notification
                .setContentIntent(notificationIntent)

                // transport controls visible on the lockscreen
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                // hide time when app was started
                .setShowWhen(false)

                // add app icon
                .setSmallIcon(R.drawable.ic_notification24dp)

                // add prev, pause, next buttons in order
                .addAction(new NotificationCompat.Action(
                        R.drawable.ic_prev24dp, getString(R.string.Previous),
                        notificationPrev_intent))
                .addAction(new NotificationCompat.Action(
                        R.drawable.ic_pause24dp, getString(R.string.Pause),
                        notificationPause_intent))
                .addAction(new NotificationCompat.Action(
                        R.drawable.ic_next24dp, getString(R.string.Next),
                        notificationNext_intent))

                // do not alert for every notification update
                .setOnlyAlertOnce(true)

                // no notification sound or vibrate
                .setSilent(true)

                // notification cannot be dismissed by swipe
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                // Take advantage of MediaStyle features
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));
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
        MediaPlayer mp = MediaPlayer.create(MusicPlayerService.this, songURI);
        mp.setOnCompletionListener(this);
        mp.setOnErrorListener(this);
        mp.setWakeMode(MusicPlayerService.this, PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer = mp;
    }

    private void setMediaSessionMetadata(Song song){
        metadataBuilder
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, song.getAlbumID())
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, Song.getAlbumArtBitmap(this, song.getAlbumID()))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, song.getDuration());
        mediaSession.setMetadata(metadataBuilder.build());
    }

    private void setMediaSessionPlaybackState(int state, int progress){
        // playbackSpeed 0 for paused, 1 for normal, negative for rewind
        if (state == PlaybackStateCompat.STATE_PAUSED){
            playbackStateBuilder.setState(state, progress, 0);
        }
        else {
            playbackStateBuilder.setState(state, progress, 1);
        }
        mediaSession.setPlaybackState(playbackStateBuilder.build());
    }

    /**
     * send a message to update the messenger
     * @param messenger the messenger to send update to
     * @param message the update code
     */
    private void sendUpdateMessage(Messenger messenger, int message) {
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt("update", message);
        try {
            switch (message) {
                case UPDATE_SEEKBAR_PROGRESS:
                    bundle.putInt("time", mediaPlayer.getCurrentPosition());
                    msg.setData(bundle);
                    messenger.send(msg);
                    break;
                case UPDATE_BLUETOOTH_CONNECTED:
                    bundle.putString("name", bluetoothConnectedDevice.getName());
                    msg.setData(bundle);
                    messenger.send(msg);
                    break;
                case UPDATE_BLUETOOTH_DISCONNECTED:
                    // only notify main activity if all bluetooth devices are disconnected
                    if (bluetoothConnectedDevices.size() == 0){
                        msg.setData(bundle);
                        messenger.send(msg);
                    }
                    break;
                default:
                    msg.setData(bundle);
                    messenger.send(msg);
                    break;
            }
        }catch (RemoteException e) {
            Logger.logException(e, "MusicPlayerService");
        }
    }

    private final class PlaybackHandler extends Handler {
        private MusicPlayerService mService;

        public PlaybackHandler(final MusicPlayerService servicer, Looper looper) {
            super(looper);
            mService = servicer;
        }

        @Override
        public void handleMessage(final Message msg) {
            final MusicPlayerService service = mService;
            if (service == null) {
                return;
            }
            switch (msg.what) {
                case PREPARE_HANDSHAKE:
                    // get bluetooth proxy to find any bluetooth devices already connected
                    if (bluetoothProxy == null) {
                        bluetoothAdapter.getProfileProxy(MusicPlayerService.this, bluetoothServiceListener, BluetoothProfile.A2DP);
                    }

                    // send update to main messenger to complete handshake
                    sendUpdateMessage(mainActivity_messenger, UPDATE_HANDSHAKE);
                    break;
                case PREPARE_SONG_PLAYED:
                    sendUpdateMessage(mainActivity_messenger, UPDATE_SONG_PLAYED);
                    break;
                case PREPARE_SONG_LISTENED:
                    sendUpdateMessage(mainActivity_messenger, UPDATE_SONG_LISTENED);
                    break;
            }
        }
    }

    private class MusicPlayerNoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // pause the playback
                mediaSession.getController().getTransportControls().pause();
            }
        }
    }

    private class MusicPlayerBluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
                bluetoothConnectedDevices.add(device);
                bluetoothConnectedDevice = device;

                doBindService();

                // notify main activity that bluetooth connected
                if (mainActivity_messenger != null) {
                    sendUpdateMessage(mainActivity_messenger, UPDATE_BLUETOOTH_CONNECTED);
                }
            }

            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected
                bluetoothConnectedDevices.remove(device);
                if (bluetoothConnectedDevices.size() > 0) {
                    bluetoothConnectedDevice = bluetoothConnectedDevices.iterator().next();
                }
                else {
                    bluetoothConnectedDevice = null;
                }

                doUnbindService();

                // notify main activity that bluetooth disconnected
                if (mainActivity_messenger != null) {
                    sendUpdateMessage(mainActivity_messenger, UPDATE_BLUETOOTH_DISCONNECTED);
                }
            }
        }
    }

    private class MusicPlayerSessionCallback extends MediaSessionCompat.Callback{

        private MusicPlayerService mService;

        public MusicPlayerSessionCallback(MusicPlayerService mService) {
            super();
            this.mService = mService;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onPlay() {
            super.onPlay();
            try {
                mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                int focusRequest = mAudioManager.requestAudioFocus(mAudioFocusRequest);
                if (focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    // create mediaplayer for the first time
                    if (mediaPlayer == null) {
                        initMediaPlayer();
                    }

                    // set the session active
                    mediaSession.setActive(true);
                    continuePlaying = true;

                    // start the player
                    toggleMedia();

                    // update playback state
                    setMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer.getCurrentPosition());

                    // Register BECOME_NOISY BroadcastReceiver
                    registerReceiver(noisyAudioStreamReceiver, noisyIntentFilter);

                    // display the notification and place the service in the foreground
                    updateNotificationBuilder(NOTIFICATION_PLAY);
                    Notification notification = notificationBuilder.build();
                    notificationManager.notify(1, notification);
                    mService.startForeground(1, notification);
                }
            } catch (Exception e){
                Logger.logException(e, "MusicPlayerService");
            }
        }

        @SuppressLint("RestrictedApi")
        @Override
        public void onPause() {
            super.onPause();
            try {
                mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                continuePlaying = false;

                // pause the player
                toggleMedia();

                // update playback state
                setMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED, mediaPlayer.getCurrentPosition());

                // unregister BECOME_NOISY BroadcastReceiver
                unregisterReceiver(noisyAudioStreamReceiver);

                // notify main messenger to update database with current position
                sendUpdateMessage(mainActivity_messenger, UPDATE_SEEKBAR_PROGRESS);

                updateNotificationBuilder(NOTIFICATION_PAUSE);

                // take service out of the foreground, retain the notification
                mService.stopForeground(false);
            } catch (Exception e){
                Logger.logException(e, "MusicPlayerService");
            }
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            try {
                if (MainActivity.getCurrent_playlist().getSize() > 0) {
                    // stop listening to the song progress
                    setProgressListenerActive(false);

                    // update main ui and mediaplayer with prev song
                    Song next_song = MainActivity.getCurrent_playlist().getNextSong();
                    MainActivity.setCurrent_song(next_song);

                    // update metadata and playback states
                    setMediaSessionMetadata(next_song);

                    updateNotificationBuilder(NOTIFICATION_NEXT);
                    Notification notification = notificationBuilder.build();
                    notificationManager.notify(1, notification);
                    mService.startForeground(1, notification);

                    // recreate mp and play next song only if mediaplayer was playing before
                    if (continuePlaying) {
                        setMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
                        mService.recreateMediaPlayer(next_song.getId());
                        audioFocusToggleMedia();
                    } else {
                        setMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED, 0);

                        // notify main messenger to update database with current position
                        if (mediaPlayer != null) {
                            mService.recreateMediaPlayer(next_song.getId());
                            sendUpdateMessage(mainActivity_messenger, UPDATE_SEEKBAR_PROGRESS);
                        }
                        mService.stopForeground(false);
                        song_progress = 0;
                    }

                    // notify main activity to update song index in database
                    sendUpdateMessage(mainActivity_messenger, UPDATE_SONG_INDEX);

                    // start listening to the song progress
                    setProgressListenerActive(true);
                }
            } catch (Exception e) {
                Logger.logException(e, "MusicPlayerService");
            }
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            try {
                if (MainActivity.getCurrent_playlist().getSize() > 0) {
                    // stop listening to the song progress
                    setProgressListenerActive(false);

                    // update main ui and mediaplayer with prev song
                    Song prev_song = MainActivity.getCurrent_playlist().getPrevSong();
                    MainActivity.setCurrent_song(prev_song);

                    // update metadata and playback states
                    setMediaSessionMetadata(prev_song);

                    updateNotificationBuilder(NOTIFICATION_PREV);
                    Notification notification = notificationBuilder.build();
                    notificationManager.notify(1, notification);
                    mService.startForeground(1, notification);

                    // recreate mp and play prev song only if mediaplayer was playing before
                    if (continuePlaying) {
                        setMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);
                        mService.recreateMediaPlayer(prev_song.getId());
                        audioFocusToggleMedia();
                    } else {
                        setMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED, 0);

                        // notify main messenger to update database with current position
                        if (mediaPlayer != null) {
                            mService.recreateMediaPlayer(prev_song.getId());
                            sendUpdateMessage(mainActivity_messenger, UPDATE_SEEKBAR_PROGRESS);
                        }
                        mService.stopForeground(false);
                        song_progress = 0;
                    }

                    // notify main activity to update song index in database
                    sendUpdateMessage(mainActivity_messenger, UPDATE_SONG_INDEX);

                    // start listening to the song progress
                    setProgressListenerActive(true);
                }
            } catch (Exception e) {
                Logger.logException(e, "MusicPlayerService");
            }
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            try {
                song_progress = (int) pos;
                setMediaSessionPlaybackState(mediaSession.getController().getPlaybackState().getState(), song_progress);
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo((int) pos);

                    // notify main messenger to update database with current position
                    sendUpdateMessage(mainActivity_messenger, UPDATE_SEEKBAR_PROGRESS);
                }
            } catch (Exception e){
                Logger.logException(e, "MusicPlayerService");
            }
        }

        @Override
        public void onSetRepeatMode(int repeatMode) {
            super.onSetRepeatMode(repeatMode);
            try{
                MainActivity.setRepeat_mode(repeatMode);
            } catch (Exception e){
                Logger.logException(e, "MusicPlayerService");
            }
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            super.onSetShuffleMode(shuffleMode);
            try{
                switch(shuffleMode){
                    case PlaybackStateCompat.SHUFFLE_MODE_NONE:
                        MainActivity.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
                        MainActivity.setCurrent_playlist(MainActivity.getCurrent_playlist().unshufflePlaylist(MainActivity.getRandom_seed()));
                        break;
                    case PlaybackStateCompat.SHUFFLE_MODE_ALL:
                        MainActivity.setRandom_seed(Math.abs(new Random().nextInt()));
                        MainActivity.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                        MainActivity.setCurrent_playlist(MainActivity.getCurrent_playlist().shufflePlaylist(MainActivity.getRandom_seed()));
                        break;
                }

                // notify main activity to update song index in database after shuffling or unshuffling
                sendUpdateMessage(mainActivity_messenger, UPDATE_SONG_INDEX);
            } catch (Exception e){
                Logger.logException(e, "MusicPlayerService");
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onStop() {
            super.onStop();
            try {
                mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

                // abandon audio focus
                mAudioManager.abandonAudioFocusRequest(mAudioFocusRequest);

                // unregister BECOME_NOISY BroadcastReceiver
                unregisterReceiver(noisyAudioStreamReceiver);

                // stop the service
                MusicPlayerService.this.stopSelf();

                // set the session inactive (and update metadata and state)
                mediaSession.setActive(false);

                // stop the player (custom call)
                if (mediaPlayer != null) {
                    song_progress = mediaPlayer.getCurrentPosition();
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }

                setMediaSessionPlaybackState(PlaybackStateCompat.STATE_STOPPED, song_progress);

                // take the service out of the foreground
                MusicPlayerService.this.stopForeground(false);
            } catch (Exception e){
                Logger.logException(e, "MusicPlayerService");
            }
        }

        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPrepare() {
            super.onPrepare();
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            super.onPrepareFromMediaId(mediaId, extras);
        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            super.onPrepareFromSearch(query, extras);
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            super.onPrepareFromUri(uri, extras);
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            super.onPlayFromSearch(query, extras);
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            super.onPlayFromUri(uri, extras);
        }

        @Override
        public void onSkipToQueueItem(long id) {
            super.onSkipToQueueItem(id);
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
        }

        @Override
        public void onRewind() {
            super.onRewind();
        }

        @Override
        public void onSetRating(RatingCompat rating) {
            super.onSetRating(rating);
        }

        @Override
        public void onSetRating(RatingCompat rating, Bundle extras) {
            super.onSetRating(rating, extras);
        }

        @Override
        public void onSetPlaybackSpeed(float speed) {
            super.onSetPlaybackSpeed(speed);
        }

        @Override
        public void onSetCaptioningEnabled(boolean enabled) {
            super.onSetCaptioningEnabled(enabled);
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onCustomAction(String action, Bundle extras) {
            super.onCustomAction(action, extras);
            if (action.equals(CUSTOM_ACTION_PLAY_SONG)) {
                try {
                    mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    int focusRequest = mAudioManager.requestAudioFocus(mAudioFocusRequest);
                    if (focusRequest == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        // create mediaplayer for the first time
                        if (mediaPlayer == null) {
                            initMediaPlayer();
                        }

                        // set the session active
                        mediaSession.setActive(true);
                        continuePlaying = true;

                        // stop listening to the song progress
                        setProgressListenerActive(false);

                        // update metadata and playback states with selected song
                        Song curr_song = MainActivity.getCurrent_song();
                        setMediaSessionMetadata(curr_song);
                        setMediaSessionPlaybackState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, mediaPlayer.getCurrentPosition());
                        setMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0);

                        // Register BECOME_NOISY BroadcastReceiver
                        registerReceiver(noisyAudioStreamReceiver, noisyIntentFilter);

                        // display the notification and place the service in the foreground
                        updateNotificationBuilder(NOTIFICATION_CUSTOM_PLAY_SONG);
                        Notification notification = notificationBuilder.build();
                        notificationManager.notify(1, notification);
                        mService.startForeground(1, notification);

                        // recreate mediaplayer and play the song
                        mService.recreateMediaPlayer(curr_song.getId());
                        toggleMedia();

                        // notify main activity to update song index in database
                        sendUpdateMessage(mainActivity_messenger, UPDATE_SONG_INDEX);

                        // start listening to the song progress
                        setProgressListenerActive(true);
                    }
                } catch (Exception e) {
                    Logger.logException(e, "MusicPlayerService");
                }
            }
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description) {
            super.onAddQueueItem(description);
        }

        @Override
        public void onAddQueueItem(MediaDescriptionCompat description, int index) {
            super.onAddQueueItem(description, index);
        }

        @Override
        public void onRemoveQueueItem(MediaDescriptionCompat description) {
            super.onRemoveQueueItem(description);
        }
    }

    public class LocalBinder extends Binder {
        MusicPlayerService getService() {
            // return this instance of LocalService so clients can call public methods
            return MusicPlayerService.this;
        }
    }
}