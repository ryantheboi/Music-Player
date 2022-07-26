package com.example.musicplayer;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.ArrayList;
import java.util.List;

public class PlaylistWithSongMetadata {
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

    public static ArrayList<Playlist> extractPlaylists(List<PlaylistWithSongMetadata> playlistWithSongMetadataList) {
        ArrayList<Playlist> playlists = new ArrayList<>();
        for (PlaylistWithSongMetadata pws : playlistWithSongMetadataList) {
            playlists.add(pws.playlist);
        }
        return playlists;
    }
}
