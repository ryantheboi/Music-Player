package com.example.musicplayer;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

// Playlist is a class annotated with @Entity.
@Database(version = 1, entities = {SongMetadata.class, Playlist.class, Metadata.class})
@TypeConverters({PlaylistConverter.class})
abstract class MusicPlayerDatabase extends RoomDatabase {

    // SongDao is a class annotated with @Dao.
    abstract public SongMetadataDao getSongMetadataDao();

    // PlaylistDao is a class annotated with @Dao.
    abstract public PlaylistDao getPlaylistDao();

    // MetadataDao is a class annotated with @Dao.
    abstract public MetadataDao getMetadataDao();
}