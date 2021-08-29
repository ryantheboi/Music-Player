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

    @Query("SELECT played FROM Songs WHERE id = :id")
    int findPlayedById(int id);

    @Query("SELECT listened FROM Songs WHERE id = :id")
    int findListenedById(int id);

    @Query("SELECT dateListened FROM Songs WHERE id = :id")
    String findDateListenedById(int id);

    @Query("SELECT * FROM Songs WHERE id = :id")
    Song findById(int id);

    @Query("SELECT * FROM Songs WHERE title LIKE :title AND artist LIKE :artist")
    Song findByTitleArtist(String title, String artist);

    @Query("UPDATE Songs SET played = :played WHERE id =:id")
    void updatePlayed(int id, int played);

    @Query("UPDATE Songs SET listened = :listened, dateListened = :dateListened WHERE id =:id")
    void updateListened(int id, int listened, String dateListened);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Song song);

    @Delete
    void delete(Song song);
}
