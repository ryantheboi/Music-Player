package com.example.musicplayer;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Playlist playlist);

    @Delete
    void delete(Playlist playlist);
}
