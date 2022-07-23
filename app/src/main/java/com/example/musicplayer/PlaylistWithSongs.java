package com.example.musicplayer;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.List;

public class PlaylistWithSongs {
    @Embedded
    public Playlist playlist;
    @Relation(
            parentColumn = "playlistId",
            entity = SongMetadata.class,
            entityColumn = "songId",
            associateBy = @Junction(
                    value = PlaylistSongJunction.class,
                    parentColumn = "pId",
                    entityColumn = "sId")
    )
    public List<SongMetadata> songs;
}
