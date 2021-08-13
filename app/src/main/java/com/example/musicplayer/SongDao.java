package com.example.musicplayer;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SongDao {
    @Query("SELECT * FROM Songs")
    List<Song> getAll();

    @Query("SELECT * FROM Songs WHERE id IN (:songIds)")
    List<Song> getAllByIds(int[] songIds);

    @Query("SELECT * FROM Songs WHERE id = :id")
    Song findById(int id);

    @Query("SELECT * FROM Songs WHERE title LIKE :title AND artist LIKE :artist")
    Song findByTitleArtist(String title, String artist);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Song song);

    @Delete
    void delete(Song song);
}
