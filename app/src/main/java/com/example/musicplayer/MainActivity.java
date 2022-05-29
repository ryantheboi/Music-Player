package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.ActionMode;
import android.view.View;
import android.widget.Toast;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import static com.example.musicplayer.Notifications.CHANNEL_ID_1;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_REQUEST = 1;
    private boolean isPermissionGranted = false;
    private boolean isInfoDisplaying;
    private static Song current_song = Song.EMPTY_SONG;
    private Bitmap current_albumImage;
    private ArrayList<Song> fullSongList;
    private HashMap<Integer, SongMetadata> fullSongMetadataHashMap;
    private static ArrayList<Playlist> playlistList;
    private static Playlist current_playlist;
    private static Playlist fullPlaylist;
    private Messenger mainActivityMessenger;
    private Intent musicServiceIntent;
    private Intent notificationIntent;
    private Intent notificationPauseplayIntent;
    private Intent notificationPrevIntent;
    private Intent notificationNextIntent;
    private Intent seekBar_progressIntent;
    private MediaSessionCompat mediaSession;
    private static Notification notification;
    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    public static boolean isActionMode = false;
    public static ActionMode actionMode = null;
    private boolean isDestroyed = false;
    private Metadata metadata;
    private boolean isViewModelReady = false;
    private static int random_seed;
    private static boolean isShuffled;
    private boolean isAlbumArtCircular;
    private static int repeat_status;
    private MessageHandler messageHandler;
    private MainActivity.MessageHandler seekbarHandler;
    private SlidingUpPanelLayout mainActivityLayout;

    // database
    private DatabaseRepository databaseRepository;

    // fragments
    private MainFragmentPrimary mainFragmentPrimary;
    private MainFragmentSecondary mainFragmentSecondary;

    // service
    private MediaBrowserCompat mediaBrowser;
    private MediaBrowserCompat.ConnectionCallback mediaBrowserConnectionCallback;
    private MediaControllerCompat mediaController;
    private MediaControllerCompat.Callback mediaControllerCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setTheme(R.style.ThemeOverlay_AppCompat_MusicLight);
            ThemeColors.generateThemeValues(this, R.style.ThemeOverlay_AppCompat_MusicLight);

            System.out.println("created");

            initMediaBrowserClient();

            // initialize database repository to handle all retrievals and transactions
            databaseRepository = new DatabaseRepository(this, this);

            // init thread for message handling
            HandlerThread messageHandlerThread = new HandlerThread("MessageHandler");
            messageHandlerThread.start();
            messageHandler = new MessageHandler(messageHandlerThread.getLooper());
            mainActivityMessenger = new Messenger(messageHandler);

            // init thread for seekbar updates
            HandlerThread seekbarHandlerThread = new HandlerThread("SeekbarHandler");
            seekbarHandlerThread.start();
            seekbarHandler = new MainActivity.MessageHandler(seekbarHandlerThread.getLooper());

            // init seekbar intent
            seekBar_progressIntent = new Intent(this, MusicPlayerService.class);

            // when a configuration change occurs and activity is recreated, fragment is auto restored
            if (savedInstanceState == null) {
                mainFragmentPrimary = MainFragmentPrimary.getInstance(mainActivityMessenger);
                mainFragmentSecondary = MainFragmentSecondary.getInstance(mainActivityMessenger);
                getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .add(R.id.fragment_main_primary, mainFragmentPrimary)
                        .add(R.id.fragment_main_secondary, mainFragmentSecondary)
                        .commit();
            }

            setContentView(R.layout.activity_main);
            mainActivityLayout = findViewById(R.id.slidingPanel);

        }catch (Exception e){
            Logger.logException(e);
        }
    }

    @Override
    @TargetApi(16)
    protected void onStart() {
        try {
            super.onStart();
            System.out.println("started");
            mediaBrowser.connect();

            isPermissionGranted = ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            // check and request for read permissions
            if (isPermissionGranted) {
                setVolumeControlStream(AudioManager.STREAM_MUSIC);

                musicServiceIntent = new Intent(this, MusicPlayerService.class);

                // retrieve metadata values from database (blocks until db is available)
                databaseRepository.asyncGetMetadata();

                // init listview functionality and playlist
                if (!isViewModelReady) {
                    initMusicList();
                }
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // present rationale to user and then request for permissions
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            } else {
                // request for permissions
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            }
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    @Override
    protected void onResume() {
        try {
            System.out.println("resumed");
            super.onResume();
            setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    @Override
    protected void onPause() {
        try {
            System.out.println("paused");
            if (isPermissionGranted) {
                if (isViewModelReady) {
                    // update current metadata values in database
                    int theme_resourceid = ThemeColors.getThemeResourceId();
                    int songtab_scrollindex = SongListTab.getScrollIndex();
                    int songtab_scrolloffset = SongListTab.getScrollOffset();
                    databaseRepository.updateMetadataTheme(theme_resourceid);
                    databaseRepository.updateMetadataSongtab(songtab_scrollindex, songtab_scrolloffset);

                    // save the current random seed
                    databaseRepository.updateMetadataRandomSeed(random_seed);

                    // save the current shuffle and repeat option
                    databaseRepository.updateMetadataIsShuffled(isShuffled);
                    databaseRepository.updateMetadataRepeatStatus(repeat_status);

                    // save current song index and playlist to database
                    databaseRepository.insertPlaylist(current_playlist);
                    databaseRepository.updateMetadataSongIndex(current_playlist.getSongList().indexOf(current_song));
                }
            }
            super.onPause();
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    @Override
    protected void onStop() {
        try {
            System.out.println("stopped");
            if (MediaControllerCompat.getMediaController(this) != null) {
                MediaControllerCompat.getMediaController(this).unregisterCallback(mediaControllerCallback);
            }
            mediaBrowser.disconnect();
            if (isPermissionGranted) {
                databaseRepository.finish();
            }
            super.onStop();
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            System.out.println("destroyed");
            isDestroyed = true;
            super.onDestroy();
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    public void initMediaBrowserClient(){
        // Create MediaBrowserServiceCompat for music service
        mediaBrowserConnectionCallback =
                new MediaBrowserCompat.ConnectionCallback() {
                    @Override
                    public void onConnected() {
                        System.out.println("CONNECTION ESTABLISHED");
                        // get the token for the MediaSession
                        MediaSessionCompat.Token token = mediaBrowser.getSessionToken();

                        mediaController =
                                new MediaControllerCompat(MainActivity.this, token);
                        mediaController.registerCallback(new MediaControllerCompat.Callback() {
                        });

                        // save the controller
                        MediaControllerCompat.setMediaController(MainActivity.this, mediaController);

                        // metadata and playback states
                        MediaMetadataCompat metadata = mediaController.getMetadata();
                        PlaybackStateCompat pbState = mediaController.getPlaybackState();

                        // register a Callback to stay in sync
                        mediaControllerCallback =
                                new MediaControllerCompat.Callback() {
                                    @Override
                                    public void onMetadataChanged(MediaMetadataCompat metadata) {
                                    }

                                    @Override
                                    public void onSessionDestroyed() {
                                        mediaBrowser.disconnect();
                                        // maybe schedule a reconnection using a new MediaBrowser instance
                                    }

                                    @Override
                                    public void onPlaybackStateChanged(PlaybackStateCompat state) {
                                        switch (state.getState()){
                                            case PlaybackStateCompat.STATE_PLAYING:
                                                System.out.println("STATE_PLAYING");
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            if (!isDestroyed) {
                                                                mainFragmentSecondary.setPausePlayBtns(R.drawable.ic_pause28dp, R.drawable.ic_pause24dp);
                                                            }
                                                        } catch (Exception e) {
                                                            Logger.logException(e);
                                                        }
                                                    }
                                                });
//                                                notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_play24dp, "play", PendingIntent.getService(getApplicationContext(), 1, notificationPauseplayIntent, PendingIntent.FLAG_IMMUTABLE)));
//                                                notification = notificationBuilder.build();
//                                                notificationManager.notify(1, notification);
//
//                                                // update the isPlaying and seekPosition values in the metadata
//                                                if ((boolean) bundle.get("updateDatabase")) {
//                                                    databaseRepository.updateMetadataIsPlaying(false);
//
//                                                    // use the seek position given by the music player service
//                                                    if ((int) bundle.get("updateSeek") >= 0) {
//                                                        databaseRepository.updateMetadataSeek((int) bundle.get("updateSeek"));
//                                                    }
//                                                    // use the seek position from the seekbar view
//                                                    else {
//                                                        databaseRepository.updateMetadataSeek(mainFragmentSecondary.getSeekbarProgress());
//                                                    }
                                                break;
                                            case PlaybackStateCompat.STATE_PAUSED:
                                                System.out.println("STATE_PAUSED");
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        try {
                                                            if (!isDestroyed) {
                                                                mainFragmentSecondary.setPausePlayBtns(R.drawable.ic_play28dp, R.drawable.ic_play24dp);
                                                            }
                                                        } catch (Exception e) {
                                                            Logger.logException(e);
                                                        }
                                                    }
                                                });
//                                                notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_pause24dp, "pause", PendingIntent.getService(getApplicationContext(), 1, notificationPauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
//                                                notification = notificationBuilder.build();
//                                                notificationManager.notify(1, notification);

                                                // update the isPlaying status in the metadata
//                                                databaseRepository.updateMetadataIsPlaying(true);
                                                break;
                                        }
                                    }
                                };
                        mediaController.registerCallback(mediaControllerCallback);

                        // Finish building the UI
                        initMusicUI();
                    }

                    @Override
                    public void onConnectionSuspended() {
                        // The Service has crashed. Disable transport controls until it automatically reconnects
                    }

                    @Override
                    public void onConnectionFailed() {
                        // The Service has refused our connection
                    }
                };

        mediaBrowser = new MediaBrowserCompat(this,
                            new ComponentName(this, MusicPlayerService.class),
                            mediaBrowserConnectionCallback,
                    null); // optional Bundle
    }

    /**
     * Initializes all songs from the device and all playlists from the database
     */
    @TargetApi(24)
    public void initMusicList() {
        // gets all songs from device and sorts them
        fullSongList = new ArrayList<>();
        getMusic(); // populates fullSongList
        fullSongList.sort(new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                String o1_title = o1.getTitle().toUpperCase();
                String o2_title = o2.getTitle().toUpperCase();
                return o1_title.compareTo(o2_title);
            }
        });

        fullPlaylist = new Playlist("FULL_PLAYLIST", fullSongList);

        // asynchronously gets all songs and playlists from database, then updates main activity
        databaseRepository.asyncGetAllSongMetadata();
        databaseRepository.asyncInitAllPlaylists();
    }

    public void getMusic() {
        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri, null, null, null, null);

        if (songCursor != null && songCursor.moveToFirst()) {
            SongHelper.setSongCursorIndexes(songCursor);

            do {
                Song song = SongHelper.createSong(songCursor);
                databaseRepository.insertSongMetadataIfNotExist(song);
                fullSongList.add(song);
            } while (songCursor.moveToNext());

            songCursor.close();
        }
    }

    @TargetApi(24)
    public void getMusicPlaylistsAsync() {
        CompletableFuture<ArrayList<Playlist>> cf;

        // perform asynchronously to return all playlists and their songs from mediastore
        cf = CompletableFuture.supplyAsync(new Supplier<ArrayList<Playlist>>() {
            @Override
            public ArrayList<Playlist> get() {
                // query for cursor to iterate over all playlists in mediastore
                ArrayList<Playlist> mediastorePlaylists = new ArrayList<>();
                ContentResolver contentResolver = getContentResolver();
                Cursor playlistCursor = contentResolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null, null, null, null);
                if (playlistCursor != null && playlistCursor.moveToFirst()) {

                    int playlistId = playlistCursor.getColumnIndex(MediaStore.Audio.Playlists._ID);
                    int playlistName = playlistCursor.getColumnIndex(MediaStore.Audio.Playlists.NAME);

                    do {
                        // query for cursor to iterate over each member in a playlist (using its id)
                        long current_playlistId = playlistCursor.getLong(playlistId);
                        String current_playlistName = playlistCursor.getString(playlistName);

                        Uri playListUri = MediaStore.Audio.Playlists.Members.getContentUri("external", current_playlistId);
                        Cursor playlistMembersCursor = contentResolver.query(playListUri, null, null, null, null);
                        if (playlistMembersCursor != null) {
                            if (playlistMembersCursor.moveToFirst()) {
                                ArrayList<Song> current_playlistSongs = new ArrayList<>();
                                Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                                String selection = MediaStore.Audio.Media._ID + "=?";
                                do {
                                    // query for cursor to identify playlist members that are songs
                                    @SuppressLint("Range") String track_id = playlistMembersCursor.getString(playlistMembersCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));
                                    String[] selectionArgs = new String[]{track_id};
                                    Cursor songCursor = contentResolver.query(songUri, null, selection, selectionArgs, null);
                                    SongHelper.setSongCursorIndexes(songCursor);

                                    if (songCursor.getCount() >= 0 && songCursor.moveToFirst()) {
                                        Song song = SongHelper.createSong(songCursor);
                                        current_playlistSongs.add(song);
                                    }
                                    songCursor.close();
                                } while (playlistMembersCursor.moveToNext());

                                // add the playlist and its songs to the async return
                                Playlist current_playlist = new Playlist(DatabaseRepository.generatePlaylistId(), current_playlistName, current_playlistSongs, 0);
                                mediastorePlaylists.add(current_playlist);

                                // add the playlist and its songs to the database
                                databaseRepository.insertPlaylist(current_playlist);
                            }
                            playlistMembersCursor.close();
                        }
                    } while (playlistCursor.moveToNext());
                    playlistCursor.close();
                }
                return mediastorePlaylists;
            }
        });

        // perform after the asynchronous operation is complete
        cf.thenAccept(new Consumer<ArrayList<Playlist>>() {
            @Override
            public void accept(final ArrayList<Playlist> arr) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (Playlist mediastorePlaylist : arr){
                            // update the viewpager adapter with the mediastore playlists
                            mainFragmentPrimary.addPlaylist(mediastorePlaylist);
                        }
                    }
                });
                // update db to notify that mediastore playlists (if any) have been imported
                databaseRepository.updateMetadataIsMediaStorePlaylistsImported(true);
            }
        });
    }

    /**
     * Method used to initialize all UI components that are related to the device's music
     */
    public void initMusicUI(){
        // init main ui
        initNotification();
        mainFragmentSecondary.initMainFragmentSecondaryUI();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSION_REQUEST:
                // if request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                    onStart();
                } else {
                    Toast.makeText(this, "Permission denied..", Toast.LENGTH_SHORT).show();
                    // gracefully degrade app experience and explain what features are unavailable
                }
                break;
        }
    }

    @TargetApi(19)
    public void initNotification() {
        mediaSession = new MediaSessionCompat(this, "media");
        notificationManager = NotificationManagerCompat.from(this);

        Bitmap largeImage = BitmapFactory.decodeResource(getResources(), R.drawable.default_albumart);
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

        // create intents for the notification action buttons
        Messenger notificationMessenger = new Messenger(messageHandler);
        notificationPrevIntent = new Intent(this, MusicPlayerService.class).putExtra("prev", notificationMessenger);
        notificationPauseplayIntent = new Intent(this, MusicPlayerService.class).putExtra("pauseplay", notificationMessenger);
        notificationNextIntent = new Intent(this, MusicPlayerService.class).putExtra("next", notificationMessenger);

        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID_1)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_music)
                .setContentTitle("song")
                .setContentText("artist")
                .setLargeIcon(largeImage)
                .addAction(R.drawable.ic_prev24dp, "prev", PendingIntent.getService(this, 0, notificationPrevIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_play24dp, "play", PendingIntent.getService(this, 1, notificationPauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_next24dp, "next", PendingIntent.getService(this, 2, notificationNextIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(false)
                .setContentIntent(contentIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.getSessionToken()));

        notification = notificationBuilder.build();
        notificationManager.notify(1, notification);

        // notify music player service about the notification
        notificationIntent = new Intent(this, MusicPlayerService.class);
        notificationIntent.putExtra("notification", mainActivityMessenger);
        startService(notificationIntent);
    }

    /**
     * Set the theme to the resource id provided and apply its colors to this activity
     * @param theme_resid the resource id of the theme to apply
     */
    public void updateTheme(int theme_resid) {
        setTheme(theme_resid);
        mainFragmentPrimary.updateFragmentColors();
        mainFragmentSecondary.updateFragmentColors();
    }



    /**
     * Cleans playlist database of any songs that can no longer be found in the full list of songs
     */
    private void cleanPlaylistDatabase(){
        HashMap<Song, SongNode> fullSongHashMap = new HashMap<>(fullPlaylist.getSongHashMap());
        for (Playlist playlist : playlistList){
            // accumulate list of removed songs for this playlist (if any)
            ArrayList<Song> playlist_songs = playlist.getSongList();
            ArrayList<Song> playlist_removed_songs = new ArrayList<>();
            for (Song song : playlist_songs){
                if (!fullSongHashMap.containsKey(song)){
                    playlist_removed_songs.add(song);
                }
            }

            // recreate the playlist excluding its removed songs (if any) and replace in database
            if (playlist_removed_songs.size() > 0){
                for (Song removed_song : playlist_removed_songs){
                    // removes song from the playlist reference
                    playlist_songs.remove(removed_song);
                }
                Playlist updated_playlist = new Playlist(playlist.getId(), playlist.getName(), playlist_songs, 0);
                databaseRepository.insertPlaylist(updated_playlist);
            }
        }
    }

    /**
     * Method used to initialize various app components using values stored in the metadata
     * E.g. views, random seed, current song, etc.
     */
    public void setupMetadata(){
        boolean isPlaying = metadata.getIsPlaying();
        int seekPosition = metadata.getSeekPosition();
        int songIndex = metadata.getSongIndex();
        int themeResourceId = metadata.getThemeResourceId();
        int songtab_scrollindex = metadata.getSongtab_scrollindex();
        int songtab_scrolloffset = metadata.getSongtab_scrolloffset();
        isShuffled = metadata.getIsShuffled();
        repeat_status = metadata.getRepeatStatus();
        boolean isMediaStorePlaylistsImported = metadata.getIsMediaStorePlaylistsImported();
        isAlbumArtCircular = metadata.getIsAlbumArtCircular();
        random_seed = metadata.getRandom_seed();

        // set current song using the song index metadata
        if (current_playlist.getSize() > 0) {
            current_song = current_playlist.getSongList().get(songIndex);
        }

        // set shuffle button transparency using the isShuffled metadata
        mainFragmentSecondary.setShuffleBtnAlpha(isShuffled ? 255 : 40);

        // set repeat button appearance using repeatStatus metadata
        mainFragmentSecondary.toggleRepeatButton(repeat_status);

        // retrieve all playlists that exist in mediastore if this hasn't been done before
        if (!isMediaStorePlaylistsImported){
            getMusicPlaylistsAsync();
        }

        // music player is playing, start music service but keep playing
        if (isPlaying) {
            musicServiceIntent.putExtra("musicListInitPlaying", mainActivityMessenger);
            startService(musicServiceIntent);

            initMusicUI();
        }
        // music player is not playing, start music service for the first time
        else {
            musicServiceIntent.putExtra("musicListInitPaused", mainActivityMessenger);
            startService(musicServiceIntent);

            initMusicUI();

            // inform the music service about the seekbar's position from the metadata
            mainFragmentSecondary.updateSeekbarProgress(seekPosition);
        }

        // set the current theme and generate theme values
        setTheme(themeResourceId);
        ThemeColors.generateThemeValues(this, themeResourceId);

        // after generating theme values, update the main ui
        updateTheme(themeResourceId);
        SongListTab.setScrollSelection(songtab_scrollindex, songtab_scrolloffset);

        // metadata has finished loading and the UI is now ready to be consumed
        isViewModelReady = true;
    }

    /**
     * Updates the main activity with the changes made by the database repository
     * @param object an object associated with the update operation (e.g. a playlist, or playlists)
     * @param messenger the messenger to notify after successfully adding the playlist
     * @param operation the operation being performed with the playlist
     */
    public void updateMainActivity(Object object, final Messenger messenger, final int operation){
        try {
            final MainActivity mainActivity = this;
            switch (operation) {
                case DatabaseRepository.ASYNC_INIT_ALL_PLAYLISTS:
                    // remove any songs that were not able to be found in the device
                    playlistList = (ArrayList<Playlist>) object;
                    cleanPlaylistDatabase();
                    SongListAdapter songListadapter = new SongListAdapter(this, R.layout.adapter_song_layout, fullSongList, this);
                    PlaylistAdapter playlistAdapter = new PlaylistAdapter(this, R.layout.adapter_playlist_layout, playlistList, this);
                    mainFragmentPrimary.setAdapters(songListadapter, playlistAdapter);

                    // initialize current playlist from database, if possible
                    databaseRepository.asyncGetCurrentPlaylist();
                    break;
                case DatabaseRepository.ASYNC_GET_CURRENT_PLAYLIST:
                    current_playlist = (Playlist) object;

                    // if a playlist wasn't retrieved from the database
                    if (current_playlist == null) {
                        current_playlist = fullPlaylist;
                        if (fullSongList.size() > 0) {
                            current_song = fullSongList.get(0);
                        } else {
                            break;
                        }
                    }

                    // set up app components using the metadata already retrieved
                    setupMetadata();
                    break;
                case DatabaseRepository.ASYNC_INSERT_PLAYLIST:
                    mainFragmentPrimary.updateMainFragment(object, DatabaseRepository.ASYNC_INSERT_PLAYLIST);
                    break;
                case DatabaseRepository.ASYNC_MODIFY_PLAYLIST:
                    mainFragmentPrimary.updateMainFragment(object, DatabaseRepository.ASYNC_MODIFY_PLAYLIST);
                    break;
                case DatabaseRepository.ASYNC_DELETE_PLAYLISTS_BY_ID:
                    mainFragmentPrimary.updateMainFragment(object, DatabaseRepository.ASYNC_DELETE_PLAYLISTS_BY_ID);
                    break;
                case DatabaseRepository.ASYNC_GET_METADATA:
                    // metadata object guaranteed not null
                    metadata = (Metadata) object;
                    break;
                case DatabaseRepository.ASYNC_GET_ALL_SONGMETADATA:
                    ArrayList<SongMetadata> database_songs = (ArrayList<SongMetadata>) object;

                    if (database_songs != null) {
                        // create hashmap of every song id to song metadata
                        fullSongMetadataHashMap = new HashMap<>();
                        for (SongMetadata songMetadata : database_songs) {
                            fullSongMetadataHashMap.put(songMetadata.getId(), songMetadata);
                        }
                    }
                    break;
            }

            if (messenger != null) {
                // send message to end the addplaylistactivity, as the views are now updated
                Message msg = Message.obtain();
                Bundle bundle = new Bundle();
                bundle.putInt("msg", AddPlaylistFragment.FINISH);
                msg.setData(bundle);
                messenger.send(msg);
            }
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    public View getMainActivityLayout(){
        return mainActivityLayout;
    }
    public DatabaseRepository getDatabaseRepository(){
        return databaseRepository;
    }
    public HashMap<Integer, SongMetadata> getFullSongMetadataHashMap(){
        return fullSongMetadataHashMap;
    }
    public boolean getIsAlbumArtCircular(){
        return isAlbumArtCircular;
    }
    public boolean getIsInfoDisplaying(){
        return isInfoDisplaying;
    }
    public static int getRandom_seed(){
        return random_seed;
    }
    public static boolean getIsShuffled(){
        return isShuffled;
    }
    public static int getRepeat_status(){
        return repeat_status;
    }
    public static Song getCurrent_song(){
        return current_song;
    }
    public static Playlist getCurrent_playlist(){
        return current_playlist;
    }
    public static ArrayList<Playlist> getPlaylists(){
        return playlistList;
    }
    public static Playlist getFullPlaylist(){
        return fullPlaylist;
    }
    public static Notification getNotification(){
        return notification;
    }

    public void setIsAlbumArtCircular(boolean isAlbumArtCircular){
        this.isAlbumArtCircular = isAlbumArtCircular;
    }
    public void setIsInfoDisplaying(boolean isInfoDisplaying){
        this.isInfoDisplaying = isInfoDisplaying;
    }
    public void setSlidingUpPanelTouchEnabled(boolean status){
        mainActivityLayout.setTouchEnabled(status);
    }
    public static void setRandom_seed(int seed){
        random_seed = seed;
    }
    public static void setIsShuffled(boolean shuffled){
        isShuffled = shuffled;
    }
    public static void setRepeat_status(int status){
        repeat_status = status;
    }
    public static void setCurrent_song(Song song){
        current_song = song;
    }
    public static void setCurrent_playlist(Playlist playlist){
        if (!isShuffled) {
            current_playlist = playlist;
        }
        else{
            current_playlist = playlist.shufflePlaylist(random_seed);
        }
    }
    public static void setCurrent_transientPlaylist(Playlist transient_playlist){
        Playlist current_playlist = new Playlist(transient_playlist.getName(), transient_playlist.getSongList());
        setCurrent_playlist(current_playlist);
    }

    /**
     * Class is used to handle messages (mainly about updates) sent from other activities
     * in order to keep the main activity most updated
     */
    public class MessageHandler extends Handler
    {

        public MessageHandler(Looper looper) {
            super(looper);
        }

        @SuppressLint("RestrictedApi")
        @Override
        @TargetApi(26)
        public void handleMessage(Message msg) {
            try {
                Bundle bundle = msg.getData();
                int updateOperation = (int) bundle.get("update");
                switch (updateOperation) {
                    case MusicPlayerService.UPDATE_PLAY:
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                try {
//                                    if (!isDestroyed) {
//                                        mainFragmentSecondary.setPausePlayBtns(R.drawable.ic_play28dp, R.drawable.ic_play24dp);
//                                    }
//                                } catch (Exception e) {
//                                    Logger.logException(e);
//                                }
//                            }
//                        });
//                        notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_play24dp, "play", PendingIntent.getService(getApplicationContext(), 1, notificationPauseplayIntent, PendingIntent.FLAG_IMMUTABLE)));
//                        notification = notificationBuilder.build();
//                        notificationManager.notify(1, notification);
//
//                        // update the isPlaying and seekPosition values in the metadata
//                        if ((boolean) bundle.get("updateDatabase")) {
//                            databaseRepository.updateMetadataIsPlaying(false);
//
//                            // use the seek position given by the music player service
//                            if ((int) bundle.get("updateSeek") >= 0) {
//                                databaseRepository.updateMetadataSeek((int) bundle.get("updateSeek"));
//                            }
//                            // use the seek position from the seekbar view
//                            else {
//                                databaseRepository.updateMetadataSeek(mainFragmentSecondary.getSeekbarProgress());
//                            }
//                        }
                        break;
                    case MusicPlayerService.UPDATE_PAUSE:
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                try {
//                                    if (!isDestroyed) {
//                                        mainFragmentSecondary.setPausePlayBtns(R.drawable.ic_pause28dp, R.drawable.ic_pause24dp);
//                                    }
//                                } catch (Exception e) {
//                                    Logger.logException(e);
//                                }
//                            }
//                        });
//                        notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_pause24dp, "pause", PendingIntent.getService(getApplicationContext(), 1, notificationPauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
//                        notification = notificationBuilder.build();
//                        notificationManager.notify(1, notification);
//
//                        // update the isPlaying status in the metadata
//                        if ((boolean) bundle.get("updateDatabase")) {
//                            databaseRepository.updateMetadataIsPlaying(true);
//                        }
                        break;
                    case MusicPlayerService.UPDATE_SEEKBAR_DURATION:
                        // init the seekbar & textview max duration and begin thread to track progress
                        final int musicMaxDuration = (int) bundle.get("time");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mainFragmentSecondary.setSeekbarMaxDuration(musicMaxDuration);
                                } catch (Exception e) {
                                    Logger.logException(e);
                                }
                            }
                        });

                        // spawn a thread to update seekbar progress each 100 milliseconds
                        final Messenger seekMessenger = new Messenger(seekbarHandler);
                        seekBar_progressIntent.putExtra("seekbarProgress", seekMessenger);
                        Thread seekbarUpdateThread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    while (!isDestroyed) {
                                        if (!mainFragmentSecondary.getSeekbar_isTracking()) {
                                            startService(seekBar_progressIntent);
                                        }
                                        Thread.sleep(100);
                                    }
                                } catch (Exception e) {
                                    Logger.logException(e);
                                }
                            }
                        });
                        seekbarUpdateThread.start();
                        break;
                    case MusicPlayerService.UPDATE_SEEKBAR_PROGRESS:
                        int musicCurrentPosition = (int) bundle.get("time");
                        mainFragmentSecondary.setSeekbarProgress(musicCurrentPosition);
                        break;
                    case MusicPlayerService.UPDATE_SONG:
                        // update main activity with the selected song from music list
                        current_song = (Song) bundle.get("song");

                        // grab song album art and duration
                        String albumID = current_song.getAlbumID();
                        long albumID_long = Long.parseLong(albumID);
                        Uri albumArtURI = ContentUris.withAppendedId(MusicPlayerService.artURI, albumID_long);
                        ContentResolver res = getContentResolver();
                        try {
                            InputStream in = res.openInputStream(albumArtURI);
                            current_albumImage = BitmapFactory.decodeStream(in);
                            if (in != null) {
                                in.close();
                            }
                        } catch (Exception e) {
                            current_albumImage = BitmapFactory.decodeResource(getResources(), R.drawable.default_albumart);
                            e.printStackTrace();
                        }


                        // view changes must be done on the main ui thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mainFragmentSecondary.updateMainSongDetails(current_song, current_playlist, current_albumImage);
                                } catch (Exception e) {
                                    Logger.logException(e);
                                }
                            }
                        });

                        // update notification details
                        notificationBuilder
                                .setContentTitle(current_song.getTitle())
                                .setContentText(current_song.getArtist())
                                .setLargeIcon(current_albumImage);
                        notification = notificationBuilder.build();
                        notificationManager.notify(1, notification);

                        // update palette swatch colors for the animated gradients
                        ThemeColors.generatePaletteColors(current_albumImage);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    if (!isDestroyed) {
                                        mainFragmentSecondary.updateFragmentColors();
                                    }
                                } catch (Exception e) {
                                    Logger.logException(e);
                                }
                            }
                        });

                        // update the index of the current song in database
                        // song can be updated within or outside of the app
                        databaseRepository.updateMetadataSongIndex(current_playlist.getSongList().indexOf(current_song));
                        break;
                    case ChooseThemeFragment.THEME_SELECTED:
                        final int theme_resid = ThemeColors.getThemeResourceId();

                        // change theme colors and button image to match the current theme
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    updateTheme(theme_resid);
                                    mainFragmentPrimary.updateFragmentColors();
                                } catch (Exception e) {
                                    Logger.logException(e);
                                }
                            }
                        });
                        break;
                    case ChooseThemeFragment.THEME_DONE:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    mainFragmentPrimary.rotateThemeButton();
                                } catch (Exception e) {
                                    Logger.logException(e);
                                }
                            }
                        });
                        break;
                    case AddPlaylistFragment.ADD_PLAYLIST:
                        databaseRepository.asyncInsertPlaylist((Playlist) bundle.get("playlist"), (Messenger) bundle.get("messenger"));
                        break;
                    case AddPlaylistFragment.MODIFY_PLAYLIST:
                        databaseRepository.asyncModifyPlaylist((Playlist) bundle.get("playlist"), (Messenger) bundle.get("messenger"));
                        break;
                    case PlaylistTab.REMOVE_PLAYLISTS:
                        databaseRepository.asyncRemovePlaylistByIds((int[]) bundle.get("ids"));
                        break;
                    case MusicPlayerService.UPDATE_SONG_PLAYED:
                        // increment played counter for the song metadata in memory and in database
                        SongMetadata played_songMetadata = fullSongMetadataHashMap.get(((Song) bundle.get("song")).getId());
                        played_songMetadata.setPlayed(played_songMetadata.getPlayed() + 1);
                        databaseRepository.updateSongMetadataPlayed(played_songMetadata);
                        break;
                    case MusicPlayerService.UPDATE_SONG_LISTENED:
                        // update listened data for the song metadata in memory and in database
                        String data_listened = Long.toString(System.currentTimeMillis() / 1000);
                        SongMetadata listened_songMetadata = fullSongMetadataHashMap.get(((Song) bundle.get("song")).getId());
                        listened_songMetadata.setListened(listened_songMetadata.getListened() + 1);
                        listened_songMetadata.setDateListened(data_listened);
                        databaseRepository.updateSongMetadataListened(listened_songMetadata, data_listened);
                        break;
                }
            }catch (Exception e){
                Logger.logException(e);
            }
        }
    }
}

