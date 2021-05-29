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

    // the theme last selected by the user
    private int themeResourceId;

    // the values to determine the exact songtab position where the user last was
    private int songtab_scrollindex;
    private int songtab_scrolloffset;

    /**
     * Constructor used by database to create a metadata object
     */
    public Metadata(int id, int themeResourceId, int songtab_scrollindex, int songtab_scrolloffset) {
        this.id = id;
        this.themeResourceId = themeResourceId;
        this.songtab_scrollindex = songtab_scrollindex;
        this.songtab_scrolloffset = songtab_scrolloffset;
    }

    public int getId() {
        return id;
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
