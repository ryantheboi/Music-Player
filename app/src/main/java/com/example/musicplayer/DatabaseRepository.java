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
    public static final int ASYNC_INSERT_PLAYLIST = 2;
    public static final int ASYNC_MODIFY_PLAYLIST = 3;
    public static final int ASYNC_DELETE_PLAYLISTS_BY_ID = 4;
    public static final int INSERT_PLAYLIST = 5;

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
                                case INSERT_PLAYLIST:
                                    playlistDao.insert((Playlist) query.object);
                                    break;
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
     * Generates a playlist id by adding 1 to the current highest playlist id
     * @return the next highest playlist id that is not in use
     */
    public synchronized static int generatePlaylistId(){
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
}
