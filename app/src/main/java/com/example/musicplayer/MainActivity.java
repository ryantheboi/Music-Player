package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_REQUEST = 1;
    private boolean isPermissionGranted = false;
    private boolean isInfoDisplaying;
    private static Song current_song = Song.EMPTY_SONG;
    private ArrayList<Song> fullSongList;
    private HashMap<Integer, SongMetadata> fullSongMetadataHashMap;
    private static ArrayList<Playlist> playlistList;
    private static Playlist current_playlist;
    private static Playlist fullPlaylist;
    private Messenger mainActivityMessenger;
    private Intent musicServiceIntent;
    private boolean isViewModelReady = false;
    private boolean isSongListChanged = false;
    private boolean isMusicPlayerServiceReady = false;
    private boolean isMediaBrowserConnected = false;
    public static boolean isActionMode = false;
    public static ActionMode actionMode = null;
    private static Metadata metadata;
    private static int random_seed;
    private static int shuffle_mode;
    private boolean isAlbumArtCircular;
    private static int repeat_mode;
    private MessageHandler messageHandler;
    private SlidingUpPanelLayout mainActivityLayout;

    // database
    private static DatabaseRepository databaseRepository;

    // fragments
    private MainFragmentPrimary mainFragmentPrimary;
    private MainFragmentSecondary mainFragmentSecondary;

    // service
    private MediaBrowserCompat mediaBrowser;
    private MediaBrowserCompat.ConnectionCallback mediaBrowserConnectionCallback;
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

            // check and request for necessary permissions
            isPermissionGranted = ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

            if (isPermissionGranted) {
                setVolumeControlStream(AudioManager.STREAM_MUSIC);

                musicServiceIntent = new Intent(this, MusicPlayerService.class);

                mediaBrowser.connect();
            }

            // necessary permissions have not been granted yet, request for them
            else {
                String[] permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.BLUETOOTH_CONNECT} :
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.BLUETOOTH};

                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // TODO present rationale to user and then request for permissions
                }

                // request for permissions
                ActivityCompat.requestPermissions(MainActivity.this, permissions, MY_PERMISSION_REQUEST);
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
            super.onPause();
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    @Override
    protected void onStop() {
        try {
            System.out.println("stopped");

            // save metadata values to database
            metadata.setSongtab_values(SongListTab.getScrollIndex(), SongListTab.getScrollOffset());
            databaseRepository.insertMetadata(metadata);

            // unregister mediacontroller callback and disconnect this client from MusicPlayerService
            isMediaBrowserConnected = false;
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
                        try {
                            // send main messenger to musicplayer service and start handshake process for the first time
                            musicServiceIntent.putExtra("handshake", mainActivityMessenger);
                            startService(musicServiceIntent);

                            isMediaBrowserConnected = true;

                            // get the token for the MediaSession
                            MediaSessionCompat.Token token = mediaBrowser.getSessionToken();

                            // create and save the controller to this activity
                            MediaControllerCompat mediaController = new MediaControllerCompat(MainActivity.this, token);
                            MediaControllerCompat.setMediaController(MainActivity.this, mediaController);

                            // register a callback to stay in sync
                            // MusicPlayerControllerCallback() has methods that handle callbacks for metadata and playback state changes
                            mediaControllerCallback = new MusicPlayerControllerCallback();

                            mediaController.registerCallback(mediaControllerCallback);

                            // initialize the UI with songs and playlists
                            initMusicList();

                            // no need to update main ui again if the viewmodel has already been established
                            // or if no songs have been added/removed outside of app
                            if (!isViewModelReady || isSongListChanged) {
                                // retrieve metadata values from database
                                databaseRepository.asyncGetMetadata();

                                // asynchronously gets all songs and playlists from database, then updates main activity
                                databaseRepository.asyncGetAllSongMetadata();
                                databaseRepository.asyncInitAllPlaylists();

                                // init listview functionality and playlist
                                mainFragmentSecondary.initMainFragmentSecondaryUI();
                            }
                            else{
                                // update pauseplay button depending on current playback state
                                if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
                                    mainFragmentSecondary.setPausePlayBtns(R.drawable.ic_pause28dp, R.drawable.ic_pause24dp);
                                } else {
                                    mainFragmentSecondary.setPausePlayBtns(R.drawable.ic_play28dp, R.drawable.ic_play24dp);
                                    mediaController.getTransportControls().seekTo(metadata.getSeekPosition());
                                }
                            }
                        }catch (Exception e){
                            Logger.logException(e);
                        }
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
        // get all songs from device and sort them
        ArrayList<Song> mediastoreSongList = getMusicSongList();
        mediastoreSongList.sort(new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                String o1_title = o1.getTitle().toUpperCase();
                String o2_title = o2.getTitle().toUpperCase();
                return o1_title.compareTo(o2_title);
            }
        });

        // there has been a change between the existing song list and the current song list
        // this can happen when re-entering the app without destroying it
        isSongListChanged = fullSongList != null && !fullSongList.equals(mediastoreSongList);

        fullSongList = mediastoreSongList;

        fullPlaylist = new Playlist("FULL_PLAYLIST", fullSongList);
    }

    public ArrayList<Song> getMusicSongList() {
        ArrayList<Song> mediastoreSongList = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri, null, null, null, null);

        if (songCursor != null && songCursor.moveToFirst()) {
            SongHelper.setSongCursorIndexes(songCursor);

            do {
                Song song = SongHelper.createSong(songCursor);
                databaseRepository.insertSongMetadataIfNotExist(song);
                mediastoreSongList.add(song);
            } while (songCursor.moveToNext());

            songCursor.close();
        }
        return mediastoreSongList;
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

                        // update metadata to notify that mediastore playlists (if any) have been imported
                        metadata.setMediaStorePlaylistsImported(true);
                    }
                });
            }
        });
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
        int songIndex = metadata.getSongIndex();
        int songtab_scrollindex = metadata.getSongtab_scrollindex();
        int songtab_scrolloffset = metadata.getSongtab_scrolloffset();
        int seekPosition = metadata.getSeekPosition();
        int themeResourceId = metadata.getThemeResourceId();
        boolean isMediaStorePlaylistsImported = metadata.getIsMediaStorePlaylistsImported();
        shuffle_mode = metadata.getShuffle_mode();
        repeat_mode = metadata.getRepeat_mode();
        isAlbumArtCircular = metadata.getIsAlbumArtCircular();
        random_seed = metadata.getRandom_seed();

        // set current song using the song index metadata
        if (current_playlist.getSize() > 0) {
            current_song = current_playlist.getSongList().get(songIndex);
            if (current_playlist.getSize() > songIndex) {
                current_song = current_playlist.getSongList().get(songIndex);
            }

            // TODO this does not fix song mismatching if songindex is within current_playlist bounds
            // if the number of songs in the device was updated and the
            // songindex no longer works with the current playlist
            // reset current playlist and song to default
            else{
                current_playlist = fullPlaylist;
                current_song = fullPlaylist.getSize() > 0 ? current_playlist.getSongList().get(0) : Song.EMPTY_SONG;
            }
        }

        // retrieve all playlists that exist in mediastore if this hasn't been done before
        if (!isMediaStorePlaylistsImported){
            getMusicPlaylistsAsync();
        }

        // set album art roundness
        mainFragmentSecondary.setAlbumArtCircular(isAlbumArtCircular);

        // set shuffle button transparency using the shuffle mode metadata
        mainFragmentSecondary.setShuffleBtnAlpha(shuffle_mode == PlaybackStateCompat.SHUFFLE_MODE_ALL ? 255 : 40);

        // set repeat button appearance using repeat_mode metadata
        mainFragmentSecondary.setRepeatButton(repeat_mode);

        // update main ui scroll index
        SongListTab.setScrollSelection(songtab_scrollindex, songtab_scrolloffset);

        // update main display button depending on current playback state
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MainActivity.this);
        mainFragmentSecondary.updateMainSongDetails(current_song, current_playlist);
        if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
            mainFragmentSecondary.setPausePlayBtns(R.drawable.ic_pause28dp, R.drawable.ic_pause24dp);
        } else {
            mainFragmentSecondary.setPausePlayBtns(R.drawable.ic_play28dp, R.drawable.ic_play24dp);
            mainFragmentSecondary.setSeekbarProgress(seekPosition);
            mediaController.getTransportControls().seekTo(seekPosition);
        }

        // generate theme values and update main ui colors for the first time
        setTheme(themeResourceId);
        ThemeColors.generateThemeValues(MainActivity.this, themeResourceId);
        mainFragmentPrimary.updateFragmentColors();
        mainFragmentSecondary.updateFragmentColors();

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
            switch (operation) {
                case DatabaseRepository.ASYNC_INIT_ALL_PLAYLISTS:
                    // remove any songs that were not able to be found in the device
                    playlistList = (ArrayList<Playlist>) object;
                    cleanPlaylistDatabase();

                    // update adapters for ui
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

                    // set up app components using the metadata already retrieved and the current playlist
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
    public static int getShuffleMode(){
        return shuffle_mode;
    }
    public static int getRepeat_mode(){
        return repeat_mode;
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
    public void setIsAlbumArtCircular(boolean isAlbumArtCircular){
        metadata.setAlbumArtCircular(isAlbumArtCircular);
        this.isAlbumArtCircular = isAlbumArtCircular;
    }
    public void setIsInfoDisplaying(boolean isInfoDisplaying){
        this.isInfoDisplaying = isInfoDisplaying;
    }
    public void setSlidingUpPanelTouchEnabled(boolean status){
        mainActivityLayout.setTouchEnabled(status);
    }
    public static void setRandom_seed(int seed){
        metadata.setRandom_seed(seed);
        random_seed = seed;
    }
    public static void setShuffleMode(int mode){
        metadata.setShuffle_mode(mode);
        shuffle_mode = mode;
    }
    public static void setRepeat_mode(int mode){
        metadata.setRepeat_mode(mode);
        repeat_mode = mode;
    }
    public static void setCurrent_song(Song song){
        current_song = song;
    }
    public static void setCurrent_playlist(Playlist playlist){
        current_playlist = playlist;

        // TODO might be better to place this database write somewhere else
        databaseRepository.insertPlaylist(current_playlist);
    }
    public static void setCurrent_playlist_shufflemode(Playlist playlist){
        if (shuffle_mode == PlaybackStateCompat.SHUFFLE_MODE_ALL) {
            setRandom_seed(Math.abs(new Random().nextInt()));
            setCurrent_playlist(playlist.shufflePlaylist(random_seed));
        } else {
            setCurrent_playlist(new Playlist(playlist.getName(), playlist.getSongList()));
        }
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
                    case MusicPlayerService.UPDATE_HANDSHAKE:
                        // handshake complete; mp service can now communicate with main activity
                        isMusicPlayerServiceReady = true;
                        break;
                    case MusicPlayerService.UPDATE_SEEKBAR_PROGRESS:
                        int musicCurrentPosition = (int) bundle.get("time");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mainFragmentSecondary.setSeekbarProgress(musicCurrentPosition);
                            }
                        });

                        // update seek position outside of app
                        if (getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
                            databaseRepository.updateMetadataSeek(musicCurrentPosition);
                        }

                        // update seek position in memory while app is still active
                        metadata.setSeekPosition(musicCurrentPosition);
                        break;
                    case ChooseThemeFragment.THEME_SELECTED:
                        final int theme_resid = ThemeColors.getThemeResourceId();
                        metadata.setThemeResourceId(theme_resid);

                        // change theme colors and button image to match the current theme
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    updateTheme(theme_resid);
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
                    case MusicPlayerService.UPDATE_SONG_INDEX:
                        // update index for current song
                        int songIndex = current_playlist.getSongList().indexOf(current_song);

                        // update song index outside of app
                        if (getLifecycle().getCurrentState() == Lifecycle.State.DESTROYED) {
                            databaseRepository.updateMetadataSongIndex(songIndex);
                        }

                        // update song index in memory while app is still active
                        metadata.setSongIndex(songIndex);

                        // update fragment with song details since media controller callbacks are now unregistered
                        if (!isMediaBrowserConnected && getLifecycle().getCurrentState() != Lifecycle.State.DESTROYED){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mainFragmentSecondary.updateMainSongDetails(current_song, current_playlist);
                                    mainFragmentSecondary.updateFragmentColors();
                                }
                            });
                        }
                        break;
                    case MusicPlayerService.UPDATE_SONG_PLAYED:
                        // increment played counter for the song metadata in memory and in database
                        SongMetadata played_songMetadata = fullSongMetadataHashMap.get(current_song.getId());
                        played_songMetadata.setPlayed(played_songMetadata.getPlayed() + 1);
                        databaseRepository.updateSongMetadataPlayed(played_songMetadata);
                        break;
                    case MusicPlayerService.UPDATE_SONG_LISTENED:
                        // update listened data for the song metadata in memory and in database
                        String date_listened = Long.toString(System.currentTimeMillis() / 1000);
                        SongMetadata listened_songMetadata = fullSongMetadataHashMap.get(current_song.getId());
                        listened_songMetadata.setListened(listened_songMetadata.getListened() + 1);
                        listened_songMetadata.setDateListened(date_listened);
                        databaseRepository.updateSongMetadataListened(listened_songMetadata, date_listened);
                        break;
                    case MusicPlayerService.UPDATE_BLUETOOTH_CONNECTED:
                        String bluetooth_deviceName = (String) bundle.get("name");
                        if (getLifecycle().getCurrentState() != Lifecycle.State.DESTROYED) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mainFragmentSecondary.setBluetoothDeviceName(bluetooth_deviceName);
                                    mainFragmentSecondary.setBluetoothTransparent(false);
                                }
                            });
                        }
                        break;
                    case MusicPlayerService.UPDATE_BLUETOOTH_DISCONNECTED:
                        if (getLifecycle().getCurrentState() != Lifecycle.State.DESTROYED) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mainFragmentSecondary.setBluetoothTransparent(true);
                                }
                            });
                        }
                        break;
                }
            }catch (Exception e){
                Logger.logException(e);
            }
        }
    }

    private class MusicPlayerControllerCallback extends MediaControllerCompat.Callback{
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            // TODO nothing should be done if current song is unchanged
            // update main display details including album art palette colors
            mainFragmentSecondary.updateMainSongDetails(MediaControllerCompat.getMediaController(MainActivity.this).getMetadata(), current_playlist);

            // update palette swatch colors for the animated gradients
            mainFragmentSecondary.updateFragmentColors();
        }

        @Override
        public void onSessionDestroyed() {
            mediaBrowser.disconnect();
            // maybe schedule a reconnection using a new MediaBrowser instance
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            try {
                // update the seekbar position in main UI and metadata
                int seekPosition = (int) state.getPosition();
                metadata.setSeekPosition(seekPosition);
                mainFragmentSecondary.setSeekbarProgress((int) state.getPosition());

                switch (state.getState()) {
                    case PlaybackStateCompat.STATE_PLAYING:
                        mainFragmentSecondary.setPausePlayBtns(R.drawable.ic_pause28dp, R.drawable.ic_pause24dp);
                        break;
                    case PlaybackStateCompat.STATE_PAUSED:
                        mainFragmentSecondary.setPausePlayBtns(R.drawable.ic_play28dp, R.drawable.ic_play24dp);
                        break;
                    case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                    case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                        break;
                }
            } catch (Exception e) {
                Logger.logException(e);
            }
        }
    }
}

