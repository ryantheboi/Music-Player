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
    public static final int ASYNC_INSERT_PLAYLIST = 3;
    public static final int ASYNC_MODIFY_PLAYLIST = 4;
    public static final int ASYNC_DELETE_PLAYLISTS_BY_ID = 5;
    public static final int INSERT_PLAYLIST = 6;
    public static final int INSERT_METADATA = 7;
    public static final int UPDATE_METADATA_THEME = 8;
    public static final int UPDATE_METADATA_SONGTAB = 9;
    public static final int UPDATE_METADATA_ISPLAYING = 10;
    public static final int UPDATE_METADATA_SEEK = 11;

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
                                case INSERT_PLAYLIST:
                                    Playlist p = (Playlist) query.object;
                                    if (p != null) {
                                        playlistDao.insert((Playlist) query.object);
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
                                case UPDATE_METADATA_ISPLAYING:
                                    metadataDao.updateIsPlaying(0, (boolean) query.object);
                                    break;
                                case UPDATE_METADATA_SEEK:
                                    metadataDao.updateSeekPosition(0, (int) query.object);
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
}
