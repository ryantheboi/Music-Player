package com.example.musicplayer;

import android.content.Context;
import android.os.Messenger;
import java.util.ArrayList;

import androidx.room.Room;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The single source of truth for all database retrievals and updates
 */
public class DatabaseRepository {
    private MusicPlayerDatabase musicPlayerDatabase;
    private SongMetadataDao songMetadataDao;
    private PlaylistDao playlistDao;
    private MetadataDao metadataDao;
    private MainActivity mainActivity;
    private boolean isModifying = false;
    private LinkedBlockingQueue<Query> messageQueue;

    // playlist objects
    private static int playlist_maxid = 0;

    public static final int ASYNC_INIT_ALL_PLAYLISTS = 0;
    public static final int ASYNC_GET_CURRENT_PLAYLIST = 1;
    public static final int ASYNC_GET_METADATA = 2;
    public static final int ASYNC_GET_ALL_SONGMETADATA = 3;
    public static final int ASYNC_INSERT_PLAYLIST = 4;
    public static final int ASYNC_MODIFY_PLAYLIST = 5;
    public static final int ASYNC_DELETE_PLAYLISTS_BY_ID = 6;
    public static final int INSERT_SONGMETADATA = 7;
    public static final int INSERT_PLAYLIST = 8;
    public static final int INSERT_METADATA = 9;
    public static final int UPDATE_METADATA_THEME = 10;
    public static final int UPDATE_METADATA_SONGTAB = 11;
    public static final int UPDATE_METADATA_SONGINDEX = 12;
    public static final int UPDATE_METADATA_ISSHUFFLED = 13;
    public static final int UPDATE_METADATA_REPEATSTATUS = 14;
    public static final int UPDATE_METADATA_ISMEDIASTOREPLAYLISTSIMPORTED = 15;
    public static final int UPDATE_METADATA_ISPLAYING = 16;
    public static final int UPDATE_METADATA_SEEK = 17;
    public static final int UPDATE_METADATA_ISALBUMARTCIRCULAR = 18;
    public static final int UPDATE_METADATA_RANDOMSEED = 19;
    public static final int UPDATE_SONGMETADATA_PLAYED = 20;
    public static final int UPDATE_SONGMETADATA_LISTENED = 21;

    /**
     * Holds the query message and the object involved (if exists)
     */
    private class Query {
        private int message;
        private Object object;
        private Object extra;

        private Query(int msg, Object obj){
            this.message = msg;
            this.object = obj;
        }

        private Query(int msg, Object obj, Object extra){
            this.message = msg;
            this.object = obj;
            this.extra = extra;
        }
    }

    public DatabaseRepository(Context context, MainActivity activity){
        this.mainActivity = activity;

        // init database and corresponding DAOs
        initDatabase(context);

        // init message queue and begin thread to pull from the queue
        startMessageQueueThread();
    }

    public void initDatabase(Context context){
        musicPlayerDatabase = Room.databaseBuilder(context,
                MusicPlayerDatabase.class, "musicplayer-database").build();

        songMetadataDao = musicPlayerDatabase.getSongMetadataDao();
        playlistDao = musicPlayerDatabase.getPlaylistDao();
        metadataDao = musicPlayerDatabase.getMetadataDao();

        String currentDBPath = context.getDatabasePath("musicplayer-database").getAbsolutePath();
    }

    /**
     * Initializes the message queue and starts a thread that takes from it (blocks if empty)
     * and interacts with the database depending on the query that is taken
     */
    private synchronized void startMessageQueueThread(){
        messageQueue = new LinkedBlockingQueue<>();
        Thread messageQueueThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    if (!isModifying) {
                        isModifying = true;
                        try {
                            final Query query = messageQueue.take();

                            int message = query.message;
                            switch (message) {
                                case ASYNC_INIT_ALL_PLAYLISTS:
                                    final ArrayList<Playlist> allPlaylists = new ArrayList<>(playlistDao.getAll());

                                    // store the current highest id value in memory
                                    playlist_maxid = playlistDao.getMaxId();

                                    // operation complete, update viewpager in mainactivity
                                    mainActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mainActivity.updateMainActivity(allPlaylists, null, ASYNC_INIT_ALL_PLAYLISTS);
                                        }
                                    });
                                    break;
                                case ASYNC_GET_CURRENT_PLAYLIST:
                                    // the current playlist always has an id of 0
                                    final Playlist currentPlaylist = playlistDao.findById(0);

                                    // operation complete, update viewpager in mainactivity
                                    mainActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mainActivity.updateMainActivity(currentPlaylist, null, ASYNC_GET_CURRENT_PLAYLIST);
                                        }
                                    });
                                    break;
                                case ASYNC_INSERT_PLAYLIST:
                                    playlistDao.insert((Playlist) query.object);

                                    // operation complete, update viewpager in mainactivity
                                    mainActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mainActivity.updateMainActivity((Playlist) query.object, (Messenger) query.extra, ASYNC_INSERT_PLAYLIST);
                                        }
                                    });
                                    break;
                                case ASYNC_MODIFY_PLAYLIST:
                                    playlistDao.insert((Playlist) query.object);

                                    // operation complete, update viewpager in mainactivity
                                    mainActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mainActivity.updateMainActivity((Playlist) query.object, (Messenger) query.extra, ASYNC_MODIFY_PLAYLIST);
                                        }
                                    });
                                    break;
                                case ASYNC_DELETE_PLAYLISTS_BY_ID:
                                    final ArrayList<Playlist> selectPlaylists = new ArrayList<>(playlistDao.getAllByIds((int[]) query.object));
                                    for (Playlist playlist : selectPlaylists){
                                        playlistDao.delete(playlist);
                                    }

                                    // operation complete, update viewpager in mainactivity
                                    mainActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mainActivity.updateMainActivity(selectPlaylists, null, ASYNC_DELETE_PLAYLISTS_BY_ID);
                                        }
                                    });
                                    break;
                                case ASYNC_GET_METADATA:
                                    // there is only one row of metadata for now, with id 0
                                    final Metadata metadata = metadataDao.findById(0);

                                    // operation complete, update viewpager in mainactivity
                                    mainActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mainActivity.updateMainActivity(metadata, null, ASYNC_GET_METADATA);
                                        }
                                    });
                                    break;
                                case ASYNC_GET_ALL_SONGMETADATA:
                                    final ArrayList<SongMetadata> database_songs = (ArrayList<SongMetadata>) songMetadataDao.getAll();
                                    mainActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mainActivity.updateMainActivity(database_songs, null, ASYNC_GET_ALL_SONGMETADATA);
                                        }
                                    });
                                    break;
                                case INSERT_SONGMETADATA:
                                    SongMetadata sm = new SongMetadata((Song) query.object);
                                    if (songMetadataDao.findById(sm.getId()) == null) {
                                        songMetadataDao.insert(sm);
                                    }
                                    break;
                                case INSERT_PLAYLIST:
                                    Playlist p = (Playlist) query.object;
                                    if (p != null) {
                                        playlistDao.insert(p);
                                    }
                                    break;
                                case INSERT_METADATA:
                                    metadataDao.insert((Metadata) query.object);
                                    break;
                                case UPDATE_METADATA_THEME:
                                    metadataDao.updateTheme(0, (int) query.object);
                                    break;
                                case UPDATE_METADATA_SONGTAB:
                                    metadataDao.updateSongTab(0, (int) query.object, (int) query.extra);
                                    break;
                                case UPDATE_METADATA_SONGINDEX:
                                    metadataDao.updateSongIndex(0, (int) query.object);
                                    break;
                                case UPDATE_METADATA_ISSHUFFLED:
                                    metadataDao.updateIsShuffled(0, (boolean) query.object);
                                    break;
                                case UPDATE_METADATA_REPEATSTATUS:
                                    metadataDao.updateRepeatStatus(0, (int) query.object);
                                    break;
                                case UPDATE_METADATA_ISMEDIASTOREPLAYLISTSIMPORTED:
                                    metadataDao.updateIsMediaStorePlaylistsImported(0, (boolean) query.object);
                                    break;
                                case UPDATE_METADATA_ISPLAYING:
                                    metadataDao.updateIsPlaying(0, (boolean) query.object);
                                    break;
                                case UPDATE_METADATA_SEEK:
                                    metadataDao.updateSeekPosition(0, (int) query.object);
                                    break;
                                case UPDATE_METADATA_ISALBUMARTCIRCULAR:
                                    metadataDao.updateIsAlbumArtCircular(0, (boolean) query.object);
                                    break;
                                case UPDATE_METADATA_RANDOMSEED:
                                    metadataDao.updateRandomSeed(0, (int) query.object);
                                    break;
                                case UPDATE_SONGMETADATA_PLAYED:
                                    int played_id = ((SongMetadata) query.object).getId();
                                    int played = songMetadataDao.findPlayedById(played_id);
                                    songMetadataDao.updatePlayed(played_id, played + 1);
                                    break;
                                case UPDATE_SONGMETADATA_LISTENED:
                                    int listened_id = ((SongMetadata) query.object).getId();
                                    int listened = songMetadataDao.findListenedById(listened_id);
                                    songMetadataDao.updateListened(listened_id, listened + 1, (String) query.extra);
                                    break;
                            }
                            isModifying = false;
                        }catch (InterruptedException e){
                            System.out.println("Taking from db query queue was interrupted");
                        }
                    }
                }
            }
        });
        messageQueueThread.start();
    }

    /**
     * Generates a playlist id by adding 1 to the current highest playlist id
     * @return the next highest playlist id that is not in use
     */
    public synchronized static int generatePlaylistId(){
        playlist_maxid += 1;
        return playlist_maxid;
    }

    /**
     * Queues message to get all playlists and the max playlist id stored in the database,
     * then updates main activity with all playlists upon completion
     */
    public synchronized void asyncInitAllPlaylists(){
        messageQueue.offer(new Query(ASYNC_INIT_ALL_PLAYLISTS, null));
    }

    /**
     * Queues message to get the current playlist (id 0) stored in the database,
     * then updates main activity upon completion
     */
    public synchronized void asyncGetCurrentPlaylist(){
        messageQueue.offer(new Query(ASYNC_GET_CURRENT_PLAYLIST, null));
    }

    /**
     * Queues message to get the metadata stored in the database,
     * then updates main activity upon completion
     */
    public synchronized void asyncGetMetadata(){
        messageQueue.offer(new Query(ASYNC_GET_METADATA, null));
    }

    /**
     * Queues message to get all song metadata records stored in the database
     * then updates main activity upon completion
     */
    public synchronized void asyncGetAllSongMetadata(){
        messageQueue.offer(new Query(ASYNC_GET_ALL_SONGMETADATA, null));
    }

    /**
     * Queues message to insert new playlist into database,
     * then updates main ui and notifies a messenger upon completion
     * @param playlist the new playlist to insert into database
     * @param messenger the messenger to notify about the change
     */
    public synchronized void asyncInsertPlaylist(Playlist playlist, Messenger messenger){
        messageQueue.offer(new Query(ASYNC_INSERT_PLAYLIST, playlist, messenger));
    }

    /**
     * Queues message to insert updated version of existing playlist into database,
     * then updates main ui and notifies a messenger upon completion
     * @param playlist the modified playlist to insert into database
     * @param messenger the messenger to notify about the change
     */
    public synchronized void asyncModifyPlaylist(Playlist playlist, Messenger messenger){
        messageQueue.offer(new Query(ASYNC_MODIFY_PLAYLIST, playlist, messenger));
    }

    /**
     * Queues message to removes playlists from database and updates main ui when complete
     * @param playlistIds array of ids of the playlists to get
     */
    public synchronized void asyncRemovePlaylistByIds(int[] playlistIds){
        messageQueue.offer(new Query(ASYNC_DELETE_PLAYLISTS_BY_ID, playlistIds));
    }

    /**
     * Queues message to insert a song's metadata into database, if it doesn't already exist
     * @param song the song to try to insert into database to store its metadata
     */
    public synchronized void insertSongMetadataIfNotExist(Song song){
        messageQueue.offer(new Query(INSERT_SONGMETADATA, song));
    }

    /**
     * Queues message to insert new playlist into database
     * @param playlist the new playlist to insert into database
     */
    public synchronized void insertPlaylist(Playlist playlist){
        messageQueue.offer(new Query(INSERT_PLAYLIST, playlist));
    }

    /**
     * Queues message to insert metadata into database
     * @param metadata the metadata to insert into database
     */
    public synchronized void insertMetadata(Metadata metadata){
        messageQueue.offer(new Query(INSERT_METADATA, metadata));
    }

    /**
     * Queues message to update the theme value in the metadata
     * @param themeResourceId the resource id corresponding to a theme
     */
    public synchronized void updateMetadataTheme(int themeResourceId){
        messageQueue.offer(new Query(UPDATE_METADATA_THEME, themeResourceId));
    }

    /**
     * Queues message to update the songtab listview position values in the metadata
     * @param scrollindex the position within the adapter's data set for the first item displayed on screen
     * @param scrolloffset the relative offset from the top of the ListView, if there is a top, otherwise 0
     */
    public synchronized void updateMetadataSongtab(int scrollindex, int scrolloffset){
        messageQueue.offer(new Query(UPDATE_METADATA_SONGTAB, scrollindex, scrolloffset));
    }

    /**
     * Queues message to update the song index value in the metadata
     * @param songIndex int representing the index of the current song in playlist 0
     */
    public synchronized void updateMetadataSongIndex(int songIndex){
        messageQueue.offer(new Query(UPDATE_METADATA_SONGINDEX, songIndex));
    }

    /**
     * Queues message to update the isShuffled value in the metadata
     * @param isShuffled true if playlist shuffling is enabled, false otherwise
     */
    public synchronized void updateMetadataIsShuffled(boolean isShuffled){
        messageQueue.offer(new Query(UPDATE_METADATA_ISSHUFFLED, isShuffled));
    }

    /**
     * Queues message to update the repeatStatus value in the metadata
     * @param repeatStatus 0 for disable, 1 for repeat playlist, 2 for repeat one song
     */
    public synchronized void updateMetadataRepeatStatus(int repeatStatus){
        messageQueue.offer(new Query(UPDATE_METADATA_REPEATSTATUS, repeatStatus));
    }

    /**
     * Queues message to update the isMediaStorePlaylistsImported value in the metadata
     * @param isMediaStorePlaylistsImported true if MediaStore playlists have been imported, false otherwise
     */
    public synchronized void updateMetadataIsMediaStorePlaylistsImported(boolean isMediaStorePlaylistsImported){
        messageQueue.offer(new Query(UPDATE_METADATA_ISMEDIASTOREPLAYLISTSIMPORTED, isMediaStorePlaylistsImported));
    }

    /**
     * Queues message to update the isPlaying value in the metadata
     * @param isPlaying true if the mediaplayer is playing, false otherwise
     */
    public synchronized void updateMetadataIsPlaying(boolean isPlaying){
        messageQueue.offer(new Query(UPDATE_METADATA_ISPLAYING, isPlaying));
    }

    /**
     * Queues message to update the seekPosition value in the metadata
     * @param seekPosition the value, in milliseconds, of a seekbar's position
     */
    public synchronized void updateMetadataSeek(int seekPosition){
        messageQueue.offer(new Query(UPDATE_METADATA_SEEK, seekPosition));
    }

    /**
     * Queues message to update the isAlbumArtCircular value in the metadata
     * @param isAlbumArtCircular true if the main display albumart is circular, false otherwise
     */
    public synchronized void updateMetadataIsAlbumArtCircular(boolean isAlbumArtCircular){
        messageQueue.offer(new Query(UPDATE_METADATA_ISALBUMARTCIRCULAR, isAlbumArtCircular));
    }

    /**
     * Queues message to update the random seed value in the metadata
     * @param random_seed int representing the seed used for randomization
     */
    public synchronized void updateMetadataRandomSeed(int random_seed){
        messageQueue.offer(new Query(UPDATE_METADATA_RANDOMSEED, random_seed));
    }

    /**
     * Queues message to increment by 1 the played value in a SongMetadata
     * @param songMetadata metadata corresponding to the song that was played
     */
    public synchronized void updateSongMetadataPlayed(SongMetadata songMetadata){
        messageQueue.offer(new Query(UPDATE_SONGMETADATA_PLAYED, songMetadata));
    }

    /**
     * Queues message to increment the listened value by 1 and update the date listened value in a SongMetadata
     * @param songMetadata metadata corresponding to the song that was listened
     * @param dateListened the last date the song was listened on, represented as number of seconds since 1970-01-01T00:00:00Z
     */
    public synchronized void updateSongMetadataListened(SongMetadata songMetadata, String dateListened){
        messageQueue.offer(new Query(UPDATE_SONGMETADATA_LISTENED, songMetadata, dateListened));
    }
}
