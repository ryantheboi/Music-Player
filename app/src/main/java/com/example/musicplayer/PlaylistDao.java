package com.example.musicplayer;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import java.util.List;

@Dao
public interface PlaylistDao {
    @Query("SELECT * FROM Playlists")
    List<Playlist> getAll();

    @Query("SELECT * FROM Playlists WHERE playlistId IN (:playlistIds)")
    List<Playlist> getAllByIds(int[] playlistIds);

    @Query("SELECT playlistId FROM Playlists ORDER BY playlistId DESC LIMIT 1")
    int getMaxId();

    @Query("SELECT * FROM Playlists WHERE playlistId = :id")
    Playlist findById(int id);

    @Query("SELECT * FROM Playlists WHERE name LIKE :name")
    Playlist findByName(String name);

    @Transaction
    @Query("SELECT * FROM Playlists")
    List<PlaylistWithSongMetadata> getAllPlaylistsWithSongMetadata();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Playlist playlist);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<PlaylistSongJunction> playlistSongJunctions);

    @Delete
    void delete(Playlist playlist);

    @Transaction
    @Query("DELETE FROM PlaylistSongJunction WHERE pId = :pId")
    void deleteByPlaylistId(int pId);
}