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

    // values to determine how to init main ui components
    private int themeResourceId;
    private boolean isLargeAlbumArt;

    // values to determine the exact songtab position where the user last was
    private int songtab_scrollindex;
    private int songtab_scrolloffset;

    @Ignore
    public static Metadata DEFAULT_METADATA = new Metadata(0, false, 0, R.style.ThemeOverlay_AppCompat_MusicLight, 0, 0, false);

    /**
     * Constructor used by database to create a metadata object
     */
    public Metadata(int id, boolean isPlaying, int seekPosition, int themeResourceId, int songtab_scrollindex, int songtab_scrolloffset, boolean isLargeAlbumArt) {
        this.id = id;
        this.isPlaying = isPlaying;
        this.seekPosition = seekPosition;
        this.themeResourceId = themeResourceId;
        this.songtab_scrollindex = songtab_scrollindex;
        this.songtab_scrolloffset = songtab_scrolloffset;
        this.isLargeAlbumArt = isLargeAlbumArt;
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

    public int getThemeResourceId() {
        return themeResourceId;
    }

    public int getSongtab_scrollindex() {
        return songtab_scrollindex;
    }

    public int getSongtab_scrolloffset() {
        return songtab_scrolloffset;
    }

    public boolean getIsLargeAlbumArt() {
        return isLargeAlbumArt;
    }
}
