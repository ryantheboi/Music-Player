package com.example.musicplayer;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SongMetadataDao {
    @Query("SELECT * FROM SongMetadata")
    List<SongMetadata> getAll();

    @Query("SELECT * FROM SongMetadata WHERE songId IN (:songIds)")
    List<SongMetadata> getAllByIds(int[] songIds);

    @Query("SELECT played FROM SongMetadata WHERE songId = :id")
    int findPlayedById(int id);

    @Query("SELECT listened FROM SongMetadata WHERE songId = :id")
    int findListenedById(int id);

    @Query("SELECT dateListened FROM SongMetadata WHERE songId = :id")
    String findDateListenedById(int id);

    @Query("SELECT * FROM SongMetadata WHERE songId = :id")
    SongMetadata findById(int id);

    @Query("SELECT * FROM SongMetadata WHERE title LIKE :title AND artist LIKE :artist")
    SongMetadata findByTitleArtist(String title, String artist);

    @Query("UPDATE SongMetadata SET played = :played WHERE songId =:id")
    void updatePlayed(int id, int played);

    @Query("UPDATE SongMetadata SET listened = :listened, dateListened = :dateListened WHERE songId =:id")
    void updateListened(int id, int listened, String dateListened);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SongMetadata song);

    @Delete
    void delete(SongMetadata song);
}
