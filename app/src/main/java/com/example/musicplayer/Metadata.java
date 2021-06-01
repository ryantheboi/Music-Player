package com.example.musicplayer;

import androidx.room.Entity;
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

    // theme last selected by the user
    private int themeResourceId;

    // values to determine the exact songtab position where the user last was
    private int songtab_scrollindex;
    private int songtab_scrolloffset;

    /**
     * Constructor used by database to create a metadata object
     */
    public Metadata(int id, boolean isPlaying, int seekPosition, int themeResourceId, int songtab_scrollindex, int songtab_scrolloffset) {
        this.id = id;
        this.isPlaying = isPlaying;
        this.seekPosition = seekPosition;
        this.themeResourceId = themeResourceId;
        this.songtab_scrollindex = songtab_scrollindex;
        this.songtab_scrolloffset = songtab_scrolloffset;
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
}
