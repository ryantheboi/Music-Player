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
    private PlaylistDatabase playlistDatabase;
    private PlaylistDao playlistDao;
    private MainActivity mainActivity;
    private boolean isModifying = false;
    private LinkedBlockingQueue<Query> messageQueue;

    // playlist objects
    private ArrayList<Playlist> allPlaylists;
    private Playlist currentPlaylist;
    private static int playlist_maxid = 0;

    public static final int GET_ALL_PLAYLISTS = 0;
    public static final int GET_CURRENT_PLAYLIST = 1;
    public static final int GET_MAX_PLAYLIST_ID = 2;
    public static final int INSERT_PLAYLIST = 3;
    public static final int ASYNC_INSERT_PLAYLIST = 4;
    public static final int ASYNC_MODIFY_PLAYLIST = 5;
    public static final int ASYNC_DELETE_PLAYLISTS_BY_ID = 6;

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

        // begin queries that are expected to take time and store the results in memory
        queryGetAllPlaylists();
        queryGetMaxPlaylistId();
        queryGetCurrentPlaylist();
    }

    public void initDatabase(Context context){
        playlistDatabase = Room.databaseBuilder(context,
                PlaylistDatabase.class, "playlist-database").build();

        playlistDao = playlistDatabase.getPlaylistDao();

        String currentDBPath = context.getDatabasePath("playlist-database").getAbsolutePath();
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
                                case GET_ALL_PLAYLISTS:
                                    allPlaylists = new ArrayList<>(playlistDao.getAll());
                                    break;
                                case GET_CURRENT_PLAYLIST:
                                    // the current playlist always has an id of 0
                                    currentPlaylist = playlistDao.findById(0);
                                    break;
                                case GET_MAX_PLAYLIST_ID:
                                    playlist_maxid = playlistDao.getMaxId();
                                    break;
                                case INSERT_PLAYLIST:
                                    playlistDao.insert((Playlist) query.object);
                                    break;
                                case ASYNC_INSERT_PLAYLIST:
                                    playlistDao.insert((Playlist) query.object);

                                    // operation complete, update viewpager in mainactivity
                                    mainActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mainActivity.updateViewPager((Playlist) query.object, (Messenger) query.extra, ASYNC_INSERT_PLAYLIST);
                                        }
                                    });
                                    break;
                                case ASYNC_MODIFY_PLAYLIST:
                                    playlistDao.insert((Playlist) query.object);

                                    // operation complete, update viewpager in mainactivity
                                    mainActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mainActivity.updateViewPager((Playlist) query.object, (Messenger) query.extra, ASYNC_MODIFY_PLAYLIST);
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
                                            mainActivity.updateViewPager(selectPlaylists, null, ASYNC_DELETE_PLAYLISTS_BY_ID);
                                        }
                                    });
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
     * Only returns all playlists when the message queue is finished querying for them
     * @return the full ArrayList containing all playlists in the database
     */
    public synchronized ArrayList<Playlist> getAllPlaylists(){
        while (allPlaylists == null) {
        }
        return allPlaylists;
    }

    /**
     * Only returns the current playlists when the message queue is finished querying for it
     * @return the current playlist, with the id of 0, from the database
     */
    public synchronized Playlist getCurrentPlaylist(){
        while (currentPlaylist == null) {
        }
        return currentPlaylist;
    }

    /**
     * Generates a playlist id by adding 1 to the current highest playlist id
     * @return the next highest playlist id that is not in use
     */
    public synchronized static int generatePlaylistId(){
        while (playlist_maxid == -1) {
        }
        playlist_maxid += 1;
        return playlist_maxid;
    }

    /**
     * Queues message to insert new playlist into database
     * @param playlist the new playlist to insert into database
     */
    public synchronized void insertPlaylist(Playlist playlist){
        messageQueue.offer(new Query(INSERT_PLAYLIST, playlist));
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
     * Queues query message to the blocking queue to get all playlists
     * This method should not be used often, as it can take some time
     */
    private synchronized void queryGetAllPlaylists(){
        allPlaylists = null;
        messageQueue.offer(new Query(GET_ALL_PLAYLISTS, null));
    }

    /**
     * Queues query message to the blocking queue to get the current playlist
     */
    private synchronized void queryGetCurrentPlaylist(){
        currentPlaylist = null;
        messageQueue.offer(new Query(GET_CURRENT_PLAYLIST, null));
    }

    /**
     * Queues query message to the blocking queue to get the next (highest) available playlist id
     * This method should not be used often, as it can take some time
     */
    private synchronized void queryGetMaxPlaylistId(){
        playlist_maxid = -1;
        messageQueue.offer(new Query(GET_MAX_PLAYLIST_ID, null));
    }
}
