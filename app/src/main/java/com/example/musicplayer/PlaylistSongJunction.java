package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

import java.util.ArrayList;
import java.util.List;

@Entity(primaryKeys = {"pId", "sId"})
public class PlaylistSongJunction {
    @NonNull
    public int pId;
    @NonNull
    @ColumnInfo(index = true)
    public int sId;

    public PlaylistSongJunction(int pId, int sId){
        this.pId = pId;
        this.sId = sId;
    }

    public static List<PlaylistSongJunction> createPlaylistSongsJointList(Playlist playlist, List<SongMetadata> songs){
        List<PlaylistSongJunction> playlistSongsJointList = new ArrayList<>();
        int pId = playlist.getPlaylistId();
        for (SongMetadata s: songs){
            playlistSongsJointList.add(new PlaylistSongJunction(pId, s.getSongId()));
        }

        return playlistSongsJointList;
    }
}
