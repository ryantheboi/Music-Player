package com.example.musicplayer;

import androidx.room.Embedded;
import androidx.room.Junction;
import androidx.room.Relation;

import java.util.ArrayList;
import java.util.HashMap;
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

    /**
     * Extract all playlists from a list of PlaylistWithSongMetadata objects and populate
     * their songs list with the included list of song metadata
     * @param playlistWithSongMetadataList list of playlists with their respective songs
     * @param fullSongHashmap hashmap containing all song IDs mapped to their respective songs
     * @return all playlists contained in playlistWithSongMetadataList
     */
    public static ArrayList<Playlist> extractPlaylists(List<PlaylistWithSongMetadata> playlistWithSongMetadataList, HashMap<Integer, Song> fullSongHashmap) {
        ArrayList<Playlist> playlists = new ArrayList<>();
        for (PlaylistWithSongMetadata pws : playlistWithSongMetadataList) {
            ArrayList<Song> songs = new ArrayList<>();
            for (SongMetadata s : pws.songs) {
                Song song = fullSongHashmap.get(s.getSongId());
                songs.add(song);
            }
            pws.playlist.extend(new Playlist("temp", songs));
            playlists.add(pws.playlist);
        }
        return playlists;
    }
}
