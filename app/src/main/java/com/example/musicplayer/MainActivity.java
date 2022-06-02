package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
    public static boolean isActionMode = false;
    public static ActionMode actionMode = null;
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

            isPermissionGranted = ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            // check and request for read permissions
            if (isPermissionGranted) {
                setVolumeControlStream(AudioManager.STREAM_MUSIC);

                musicServiceIntent = new Intent(this, MusicPlayerService.class);

                mediaBrowser.connect();
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
            if (isPermissionGranted && isViewModelReady) {
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

                // save current seekbar position to database
                databaseRepository.updateMetadataSeek(mainFragmentSecondary.getSeekbarProgress());
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
            // disconnect this client from MusicPlayerService
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
                            // get the token for the MediaSession

                            // create and save the controller to this activity
                            MediaSessionCompat.Token token = mediaBrowser.getSessionToken();
                            MediaControllerCompat mediaController = new MediaControllerCompat(MainActivity.this, token);
                            MediaControllerCompat.setMediaController(MainActivity.this, mediaController);

                            // register a callback to stay in sync
                            // MusicPlayerControllerCallback() has methods that handle callbacks for metadata and playback state changes
                            mediaControllerCallback = new MusicPlayerControllerCallback();

                            mediaController.registerCallback(mediaControllerCallback);

                            // finish initializing the UI
                            if (!isViewModelReady) {
                                // retrieve metadata values from database (blocks until db is available)
                                databaseRepository.asyncGetMetadata();

                                // init listview functionality and playlist
                                initMusicList();
                            }
                            mainFragmentSecondary.initMainFragmentSecondaryUI();

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
        boolean isPlaying = metadata.getIsPlaying();
        int songIndex = metadata.getSongIndex();
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

        // set shuffle button transparency using the isShuffled metadata
        mainFragmentSecondary.setShuffleBtnAlpha(isShuffled ? 255 : 40);

        // set repeat button appearance using repeatStatus metadata
        mainFragmentSecondary.toggleRepeatButton(repeat_status);

        // retrieve all playlists that exist in mediastore if this hasn't been done before
        if (!isMediaStorePlaylistsImported){
            getMusicPlaylistsAsync();
        }

        // update main ui scroll index
        SongListTab.setScrollSelection(songtab_scrollindex, songtab_scrolloffset);
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

                    // set up app components using the metadata already retrieved and the current playlist
                    setupMetadata();

                    // send main messenger to musicplayer service and start handshake process for the first time
                    musicServiceIntent.putExtra("handshake", mainActivityMessenger);
                    startService(musicServiceIntent);
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
                    case MusicPlayerService.UPDATE_HANDSHAKE:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // update main display button depending on current playback state
                                    MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MainActivity.this);
                                    if (mediaController.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
                                        mainFragmentSecondary.setPausePlayBtns(R.drawable.ic_pause28dp, R.drawable.ic_pause24dp);
                                    } else {
                                        mainFragmentSecondary.setPausePlayBtns(R.drawable.ic_play28dp, R.drawable.ic_play24dp);
                                        mediaController.getTransportControls().seekTo(metadata.getSeekPosition());
                                    }

                                    // generate theme values and update main ui colors for the first time
                                    int themeResourceId = metadata.getThemeResourceId();
                                    setTheme(themeResourceId);
                                    ThemeColors.generateThemeValues(MainActivity.this, themeResourceId);
                                    mainFragmentPrimary.updateFragmentColors();
                                    mainFragmentSecondary.updateFragmentColors();

                                    // handshake complete, the UI is now ready to be consumed
                                    isViewModelReady = true;
                                } catch (Exception e) {
                                    Logger.logException(e);
                                }
                            }
                        });
                        break;
                    case MusicPlayerService.UPDATE_SEEKBAR_PROGRESS:
                        int musicCurrentPosition = (int) bundle.get("time");
                        mainFragmentSecondary.setSeekbarProgress(musicCurrentPosition);
                        databaseRepository.updateMetadataSeek(musicCurrentPosition);
                        break;
                    case ChooseThemeFragment.THEME_SELECTED:
                        final int theme_resid = ThemeColors.getThemeResourceId();

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

    private class MusicPlayerControllerCallback extends MediaControllerCompat.Callback{
        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            // update main display details including album art palette colors
            mainFragmentSecondary.updateMainSongDetails(MediaControllerCompat.getMediaController(MainActivity.this).getMetadata(), current_playlist);

            // this will be called twice on app launch for the handshake
            // update palette swatch colors for the animated gradients
            mainFragmentSecondary.updateFragmentColors();


            // update the index of the current song in database
            // song can be updated within or outside of the app
//                        databaseRepository.updateMetadataSongIndex(current_playlist.getSongList().indexOf(current_song));

        }

        @Override
        public void onSessionDestroyed() {
            mediaBrowser.disconnect();
            // maybe schedule a reconnection using a new MediaBrowser instance
        }

        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            try {
                // update the seekbar position in main UI
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

