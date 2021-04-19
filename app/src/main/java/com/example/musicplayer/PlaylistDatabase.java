package com.example.musicplayer;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

// Playlist is a class annotated with @Entity.
@Database(version = 1, entities = {Playlist.class})
@TypeConverters({PlaylistConverter.class})
abstract class PlaylistDatabase extends RoomDatabase {

    // PlaylistDao is a class annotated with @Dao.
    abstract public PlaylistDao getPlaylistDao();
}