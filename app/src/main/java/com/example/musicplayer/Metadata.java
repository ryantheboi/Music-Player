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

    private int themeResourceId;
    private int seekbar_position;
    private int songtab_scrollposition;

    /**
     * Constructor used by database to create a metadata object
     */
    public Metadata(int id, int themeResourceId, int seekbar_position, int songtab_scrollposition) {
        this.id = id;
        this.themeResourceId = themeResourceId;
        this.seekbar_position = seekbar_position;
        this.songtab_scrollposition = songtab_scrollposition;
    }

    public int getId() {
        return id;
    }

    public int getThemeResourceId() {
        return themeResourceId;
    }

    public int getSeekbar_position() {
        return seekbar_position;
    }

    public int getSongtab_scrollposition() {
        return songtab_scrollposition;
    }
}
