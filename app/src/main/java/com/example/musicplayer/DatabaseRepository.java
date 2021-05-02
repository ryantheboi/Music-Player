package com.example.musicplayer;

import android.content.Context;
import androidx.room.Room;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The single source of truth for all database retrievals and updates
 */
public class DatabaseRepository {
    private PlaylistDatabase playlistDatabase;
    private PlaylistDao playlistDao;
    private boolean isModifying = false;
    private LinkedBlockingQueue<Query> messageQueue;

    // playlist objects
    private ArrayList<Playlist> playlistArrayList;
    private static int playlist_maxid = -1;

    private final int GET_ALL_PLAYLISTS = 0;
    private final int GET_MAX_PLAYLIST_ID = 1;
    private final int INSERT_PLAYLIST = 2;
    private final int DELETE_PLAYLIST = 3;

    /**
     * Holds the query message and the object involved (if exists)
     */
    private class Query {
        private int message;
        private Object object;

        private Query(int msg, Object obj){
            this.message = msg;
            this.object = obj;
        }
    }

    public DatabaseRepository(Context context){
        // init database and corresponding DAOs
        initDatabase(context);

        // init message queue and begin thread to pull from the queue
        startMessageQueueThread();

        // begin queries that are expected to take time and store the results in memory
        queryGetAllPlaylists();
        queryGetMaxPlaylistId();
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
                            Query query = messageQueue.take();

                            int message = query.message;
                            switch (message) {
                                case GET_ALL_PLAYLISTS:
                                    playlistArrayList = new ArrayList<>(playlistDao.getAll());
                                    break;
                                case GET_MAX_PLAYLIST_ID:
                                    playlist_maxid = playlistDao.getMaxId();
                                    break;
                                case INSERT_PLAYLIST:
                                    playlistDao.insert((Playlist) query.object);
                                    break;
                                case DELETE_PLAYLIST:
                                    playlistDao.delete((Playlist) query.object);
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
     * Only returns all playlists when the databaseHandler is finished querying for them
     * @return the full ArrayList containing all playlists in the database
     */
    public synchronized ArrayList<Playlist> getAllPlaylists(){
        while (playlistArrayList == null) {
        }
        return playlistArrayList;
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

    public synchronized void insertPlaylist(Playlist playlist){
        // insert playlist into db
        messageQueue.offer(new Query(INSERT_PLAYLIST, playlist));

    }

    public synchronized void deletePlaylist(Playlist playlist){
        // delete playlist from db
        messageQueue.offer(new Query(DELETE_PLAYLIST, playlist));

    }

    /**
     * Queues query message to the blocking queue to get all playlists
     * This method should not be used often, as it can take some time
     */
    private synchronized void queryGetAllPlaylists(){
        playlistArrayList = null;
        messageQueue.offer(new Query(GET_ALL_PLAYLISTS, null));
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
