package com.example.musicplayer;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Class used to store metadata from the user for the purpose of resuming the app with set values
 */
@Entity(tableName = "Metadata")
public class Metadata {
    @PrimaryKey
    private int id;

    // values to determine the mediaplayer's status
    private boolean isPlaying;
    private int seekPosition;

    // values related to the behavior of playlists
    private int songIndex;
    private int shuffle_mode;
    private int repeat_mode;
    private boolean isMediaStorePlaylistsImported;

    // values to determine how to init main ui components
    private int themeResourceId;
    private boolean isAlbumArtCircular;

    // values to determine the exact songtab position where the user last was
    private int songtab_scrollindex;
    private int songtab_scrolloffset;

    // value to store the last seed used for randomization
    private int random_seed;

    // value to determine if database is allowing read/write access (for other processes)
    private int numQueries;

    @Ignore
    public static Metadata DEFAULT_METADATA = new Metadata(0, false, 0, 0, 0, 0, false, R.style.ThemeOverlay_AppCompat_MusicLight, 0, 0, false, 0, 0);

    /**
     * Constructor used by database to create a metadata object
     */
    public Metadata(int id, boolean isPlaying, int seekPosition, int songIndex, int shuffle_mode, int repeat_mode, boolean isMediaStorePlaylistsImported, int themeResourceId, int songtab_scrollindex, int songtab_scrolloffset, boolean isAlbumArtCircular, int random_seed, int numQueries) {
        this.id = id;
        this.isPlaying = isPlaying;
        this.seekPosition = seekPosition;
        this.songIndex = songIndex;
        this.shuffle_mode = shuffle_mode;
        this.repeat_mode = repeat_mode;
        this.isMediaStorePlaylistsImported = isMediaStorePlaylistsImported;
        this.themeResourceId = themeResourceId;
        this.songtab_scrollindex = songtab_scrollindex;
        this.songtab_scrolloffset = songtab_scrolloffset;
        this.isAlbumArtCircular = isAlbumArtCircular;
        this.random_seed = random_seed;
        this.numQueries = numQueries;
    }

    public int getId() {
        return id;
    }

    public boolean getIsPlaying() {
        return isPlaying;
    }

    public int getSeekPosition(){
        return seekPosition;
    }

    public int getSongIndex(){
        return songIndex;
    }

    public int getShuffle_mode(){
        return shuffle_mode;
    }

    public int getRepeat_mode(){
        return repeat_mode;
    }

    public boolean getIsMediaStorePlaylistsImported(){
        return isMediaStorePlaylistsImported;
    }

    public int getThemeResourceId() {
        return themeResourceId;
    }

    public int getSongtab_scrollindex() {
        return songtab_scrollindex;
    }

    public int getSongtab_scrolloffset() {
        return songtab_scrolloffset;
    }

    public boolean getIsAlbumArtCircular() {
        return isAlbumArtCircular;
    }

    public int getRandom_seed(){
        return random_seed;
    }

    public int getNumQueries(){
        return numQueries;
    }
}
