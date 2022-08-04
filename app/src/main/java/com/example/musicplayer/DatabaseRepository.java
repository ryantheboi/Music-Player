package com.example.musicplayer;

import android.content.Context;

import java.util.ArrayList;

import androidx.room.Room;

import java.util.List;
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
    private LinkedBlockingQueue<Query> messageQueue;

    // playlist objects
    private static int playlist_maxid = 0;

    public static final int ASYNC_GET_ALL_PLAYLISTSWITHSONGMETADATA = 0;
    public static final int ASYNC_GET_METADATA = 1;
    public static final int ASYNC_GET_ALL_SONGMETADATA = 2;
    public static final int ASYNC_INSERT_PLAYLIST = 3;
    public static final int ASYNC_MODIFY_PLAYLIST = 4;
    public static final int ASYNC_DELETE_PLAYLISTS_BY_ID = 5;
    public static final int DELETE_PLAYLISTSONG_JUNCTION_BY_ID = 6;
    public static final int DELETE_PLAYLISTSONG_JUNCTIONS = 7;
    public static final int INSERT_SONGMETADATA = 8;
    public static final int INSERT_PLAYLIST = 9;
    public static final int INSERT_PLAYLISTSONG_JUNCTIONS = 10;
    public static final int INSERT_METADATA = 11;
    public static final int UPDATE_METADATA_THEME = 12;
    public static final int UPDATE_METADATA_SONGTAB = 13;
    public static final int UPDATE_METADATA_SONGINDEX = 14;
    public static final int UPDATE_METADATA_SHUFFLEMODE = 15;
    public static final int UPDATE_METADATA_REPEATMODE = 16;
    public static final int UPDATE_METADATA_ISMEDIASTOREPLAYLISTSIMPORTED = 17;
    public static final int UPDATE_METADATA_SEEK = 18;
    public static final int UPDATE_METADATA_ISALBUMARTCIRCULAR = 19;
    public static final int UPDATE_METADATA_RANDOMSEED = 20;
    public static final int UPDATE_SONGMETADATA_PLAYED = 21;
    public static final int UPDATE_SONGMETADATA_LISTENED = 22;

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

        try {
            // init database and corresponding DAOs
            initDatabase(context);

            // init message queue and begin thread to pull from the queue
            startMessageQueueThread();
        }catch (Exception e){
            Logger.logException(e, "DatabaseRepository");
        }
    }

    /**
     * Initializes the database and database access objects (DAOs)
     * Database connection does not need to be manually closed
     * because it will be cleaned up when the process ends
     * @param context the context that will interact with the database (e.g. MainActivity)
     */
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
                while (true) {
                    try {
                        final Query query = messageQueue.take();

                        // update metadata with the current size of the query queue
                        // current size includes current query, unless it is to get the metadata
                        int message = query.message;
                        switch (message) {
                            case ASYNC_GET_ALL_PLAYLISTSWITHSONGMETADATA:
                                final ArrayList<PlaylistWithSongMetadata> allPlaylistsWithSongs = new ArrayList<>(playlistDao.getAllPlaylistsWithSongMetadata());

                                // store the current highest id value in memory
                                playlist_maxid = playlistDao.getMaxId();

                                // operation complete, update viewpager in mainactivity
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mainActivity.updateMainActivity(allPlaylistsWithSongs, ASYNC_GET_ALL_PLAYLISTSWITHSONGMETADATA);
                                    }
                                });
                                break;
                            case ASYNC_INSERT_PLAYLIST:
                                playlistDao.insert((Playlist) query.object);
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mainActivity.updateMainActivity((Playlist) query.object, ASYNC_INSERT_PLAYLIST);
                                    }
                                });
                                break;
                            case ASYNC_MODIFY_PLAYLIST:
                                int operation = (int) query.extra;
                                switch (operation) {
                                    case Playlist.INSERT_SONGS:
                                        playlistDao.insertAll((List<PlaylistSongJunction>) query.object);
                                        mainActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mainActivity.updateMainActivity((List<PlaylistSongJunction>) query.object, ASYNC_MODIFY_PLAYLIST);
                                            }
                                        });
                                        break;
                                    case Playlist.DELETE_SONGS:
                                        playlistDao.deleteAll((List<PlaylistSongJunction>) query.object);
                                        mainActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mainActivity.updateMainActivity((List<PlaylistSongJunction>) query.object, ASYNC_MODIFY_PLAYLIST);
                                            }
                                        });
                                        break;
                                    case Playlist.RENAME:
                                        playlistDao.insert((Playlist) query.object);
                                        mainActivity.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                mainActivity.updateMainActivity((Playlist) query.object, ASYNC_MODIFY_PLAYLIST);
                                            }
                                        });
                                        break;
                                }
                                break;
                            case ASYNC_DELETE_PLAYLISTS_BY_ID:
                                final ArrayList<Playlist> selectPlaylists = new ArrayList<>(playlistDao.getAllByIds((int[]) query.object));
                                for (Playlist playlist : selectPlaylists) {
                                    playlistDao.delete(playlist);
                                }
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mainActivity.updateMainActivity(selectPlaylists, ASYNC_DELETE_PLAYLISTS_BY_ID);
                                    }
                                });
                                break;
                            case DELETE_PLAYLISTSONG_JUNCTION_BY_ID:
                                final int[] playlist_ids = (int[]) query.object;
                                for (int pId : playlist_ids) {
                                    playlistDao.deleteByPlaylistId(pId);
                                }
                                break;
                            case DELETE_PLAYLISTSONG_JUNCTIONS:
                                final List<PlaylistSongJunction> junctions = (List<PlaylistSongJunction>) query.object;
                                playlistDao.deleteAll(junctions);
                                break;
                            case ASYNC_GET_METADATA:
                                // there is only one row of metadata for now, with id 0
                                Metadata temp_metadata = metadataDao.findById(0);
                                final Metadata availableMetadata;

                                // insert default metadata if none exists yet
                                if (temp_metadata == null) {
                                    availableMetadata = Metadata.DEFAULT_METADATA;
                                    insertMetadata(availableMetadata);
                                }

                                else {
                                    availableMetadata = temp_metadata;
                                }

                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mainActivity.updateMainActivity(availableMetadata, ASYNC_GET_METADATA);
                                    }
                                });
                                break;
                            case ASYNC_GET_ALL_SONGMETADATA:
                                final ArrayList<SongMetadata> database_songs = (ArrayList<SongMetadata>) songMetadataDao.getAll();
                                mainActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mainActivity.updateMainActivity(database_songs, ASYNC_GET_ALL_SONGMETADATA);
                                    }
                                });
                                break;
                            case INSERT_SONGMETADATA:
                                SongMetadata sm = new SongMetadata((Song) query.object);
                                if (songMetadataDao.findById(sm.getSongId()) == null) {
                                    songMetadataDao.insert(sm);
                                }
                                break;
                            case INSERT_PLAYLIST:
                                Playlist p = (Playlist) query.object;
                                if (p != null) {
                                    playlistDao.insert(p);
                                }
                                break;
                            case INSERT_PLAYLISTSONG_JUNCTIONS:
                                List<PlaylistSongJunction> playlistSongJunctionList = (List<PlaylistSongJunction>) query.object;
                                if (playlistSongJunctionList.size() > 0) {
                                    playlistDao.insertAll(playlistSongJunctionList);
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
                            case UPDATE_METADATA_SHUFFLEMODE:
                                metadataDao.updateShuffleMode(0, (int) query.object);
                                break;
                            case UPDATE_METADATA_REPEATMODE:
                                metadataDao.updateRepeatMode(0, (int) query.object);
                                break;
                            case UPDATE_METADATA_ISMEDIASTOREPLAYLISTSIMPORTED:
                                metadataDao.updateIsMediaStorePlaylistsImported(0, (boolean) query.object);
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
                                int played_id = ((SongMetadata) query.object).getSongId();
                                int played = songMetadataDao.findPlayedById(played_id);
                                songMetadataDao.updatePlayed(played_id, played + 1);
                                break;
                            case UPDATE_SONGMETADATA_LISTENED:
                                int listened_id = ((SongMetadata) query.object).getSongId();
                                int listened = songMetadataDao.findListenedById(listened_id);
                                songMetadataDao.updateListened(listened_id, listened + 1, (String) query.extra);
                                break;
                        }
                    } catch (InterruptedException e) {
                        System.out.println("Taking from db query queue was interrupted");
                        Logger.logException(e, "DatabaseRepository");
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
     * Queues message to get all playlists with their corresponding songs, and the max playlist id
     * stored in the database, then updates main activity with all playlists upon completion
     */
    public synchronized void asyncGetAllPlaylistsWithSongs(){
        messageQueue.offer(new Query(ASYNC_GET_ALL_PLAYLISTSWITHSONGMETADATA, null));
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
     * Queues message to insert new playlist into database, then updates main ui
     * @param playlist the new playlist to insert into database
     */
    public synchronized void asyncInsertPlaylist(Playlist playlist){
        messageQueue.offer(new Query(ASYNC_INSERT_PLAYLIST, playlist, null));
    }

    /**
     * Queues message to add or remove songs from a playlist via the playlist-songs junction table
     * then updates main ui
     * @param object the playlist-song junctions to add or remove from db if operation is
     *               INSERT or DELETE, or a playlist to rename in db if operation is RENAME
     * @param operation the operation being performed to this playlist
     *                  (e.g. insert songs, delete songs, rename playlist)
     */
    public synchronized void asyncModifyPlaylist(Object object, int operation){
        if (operation == Playlist.RENAME) {
            messageQueue.offer(new Query(ASYNC_MODIFY_PLAYLIST, (Playlist) object, operation));
        }
        else {
            messageQueue.offer(new Query(ASYNC_MODIFY_PLAYLIST, (List<PlaylistSongJunction>) object, operation));
        }
    }

    /**
     * Queues message to removes playlists from database and updates main ui when complete
     * @param playlistIds array of ids of the playlists to get
     */
    public synchronized void asyncRemovePlaylistByIds(int[] playlistIds){
        messageQueue.offer(new Query(ASYNC_DELETE_PLAYLISTS_BY_ID, playlistIds));
    }

    /**
     * Queues message to removes playlists from junction db table and updates main ui when complete
     * @param playlistIds array of ids of the playlists to get
     */
    public synchronized void asyncRemovePlaylistSongJunctionByIds(int[] playlistIds){
        messageQueue.offer(new Query(DELETE_PLAYLISTSONG_JUNCTION_BY_ID, playlistIds));
    }

    /**
     * Queues message to removes songs from playlists in junction db table
     * @param junctions array of playlist-song junctions, each containing the
     *                  playlist id and song id to remove
     */
    public synchronized void deletePlaylistSongJunctions(List<PlaylistSongJunction> junctions){
        messageQueue.offer(new Query(DELETE_PLAYLISTSONG_JUNCTIONS, junctions));
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
     * Queues message to insert new list of playlistSongJunction into database
     * @param junctions a list of objects each representing the many-to-many
     *                  relationship between playlists and songs
     */
    public synchronized void insertPlaylistSongJunctions(List<PlaylistSongJunction> junctions){
        messageQueue.offer(new Query(INSERT_PLAYLISTSONG_JUNCTIONS, junctions));
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
     * Queues message to update the shuffle_mode value in the metadata
     * @param shuffleMode 0 for SHUFFLE_MODE_NONE (no shuffle)
     *                    1 for SHUFFLE_MODE_ALL (shuffle)
     */
    public synchronized void updateMetadataShuffleMode(int shuffleMode){
        messageQueue.offer(new Query(UPDATE_METADATA_SHUFFLEMODE, shuffleMode));
    }

    /**
     * Queues message to update the repeat_mode value in the metadata
     * @param repeat_mode 0 for REPEAT_NONE, 1 for REPEAT_ONE, 2 for REPEAT_ALL
     */
    public synchronized void updateMetadataRepeatMode(int repeat_mode){
        messageQueue.offer(new Query(UPDATE_METADATA_REPEATMODE, repeat_mode));
    }

    /**
     * Queues message to update the isMediaStorePlaylistsImported value in the metadata
     * @param isMediaStorePlaylistsImported true if MediaStore playlists have been imported, false otherwise
     */
    public synchronized void updateMetadataIsMediaStorePlaylistsImported(boolean isMediaStorePlaylistsImported){
        messageQueue.offer(new Query(UPDATE_METADATA_ISMEDIASTOREPLAYLISTSIMPORTED, isMediaStorePlaylistsImported));
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

    /**
     * Informs message thread to clear up all remaining queries for the db in the message queue
     * It is not necessary to close the db connection because that will be handled by Android
     */
    public synchronized void finish(){
        // last query being offered, empty the queue
        messageQueue.clear();
    }
}
