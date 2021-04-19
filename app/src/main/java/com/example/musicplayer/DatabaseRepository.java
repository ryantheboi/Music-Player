package com.example.musicplayer;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import androidx.room.Room;

import java.util.ArrayList;

/**
 * The single source of truth for all database retrievals and updates
 */
public class DatabaseRepository {
    private PlaylistDatabase playlistDatabase;
    private PlaylistDao playlistDao;
    private HandlerThread databaseHandlerThread;
    private DatabaseHandler databaseHandler;

    // playlist objects
    private ArrayList<Playlist> playlistArrayList;
    private Playlist temp_playlist;
    private static int playlist_maxid = -1;

    private final int GET_ALL_PLAYLISTS = 0;
    private final int GET_MAX_PLAYLIST_ID = 1;
    private final int INSERT_PLAYLIST = 2;
    private final int DELETE_PLAYLIST = 3;

    public DatabaseRepository(Context context){
        // init database and corresponding DAOs
        initDatabase(context);

        // database actions should be done on a separate thread to avoid blocking the main UI
        databaseHandlerThread = new HandlerThread("DatabaseHandler");
        databaseHandlerThread.start();
        databaseHandler = new DatabaseHandler(databaseHandlerThread.getLooper());

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
     * Only returns all playlists when the databaseHandler is finished querying for them
     * @return the full ArrayList containing all playlists in the database
     */
    public ArrayList<Playlist> getAllPlaylists(){
        while (playlistArrayList == null) {
        }
        return playlistArrayList;
    }

    /**
     * Generates a playlist id by adding 1 to the current highest playlist id
     * @return the next highest playlist id that is not in use
     */
    public static int generatePlaylistId(){
        while (playlist_maxid == -1) {
        }
        playlist_maxid += 1;
        return playlist_maxid;
    }

    public void insertPlaylist(Playlist playlist){
        // insert playlist into db
        this.temp_playlist = playlist;

        databaseHandler.removeMessages(INSERT_PLAYLIST);
        databaseHandler.obtainMessage(INSERT_PLAYLIST).sendToTarget();

        System.out.println("fake insert");
    }

    public void deletePlaylist(Playlist playlist){
        // delete playlist from db
        this.temp_playlist = playlist;
        databaseHandler.removeMessages(DELETE_PLAYLIST);
        databaseHandler.obtainMessage(DELETE_PLAYLIST).sendToTarget();
    }

    /**
     * Queues query message on the handler thread to get all playlists
     * This method should not be used often, as it can take some time and may cause read conflicts
     */
    private void queryGetAllPlaylists(){
        playlistArrayList = null;
        databaseHandler.removeMessages(GET_ALL_PLAYLISTS);
        databaseHandler.obtainMessage(GET_ALL_PLAYLISTS).sendToTarget();
    }

    /**
     * Queues query message on the handler thread to get the next (highest) available playlist id
     * This method should not be used often, as it may cause read conflicts
     */
    private void queryGetMaxPlaylistId(){
        playlist_maxid = -1;
        databaseHandler.removeMessages(GET_MAX_PLAYLIST_ID);
        databaseHandler.obtainMessage(GET_MAX_PLAYLIST_ID).sendToTarget();
    }

    private final class DatabaseHandler extends Handler {

        public DatabaseHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case GET_ALL_PLAYLISTS:
                    playlistArrayList = new ArrayList<>(playlistDao.getAll());
                    break;
                case GET_MAX_PLAYLIST_ID:
                    playlist_maxid = playlistDao.getMaxId();
                    break;
                case INSERT_PLAYLIST:
                    playlistDao.insert(temp_playlist);
                    break;
                case DELETE_PLAYLIST:
                    playlistDao.delete(temp_playlist);
                    break;
            }
        }
    }
}
