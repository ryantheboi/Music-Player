package com.example.musicplayer;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MetadataDao {
    @Query("SELECT * FROM Metadata")
    List<Metadata> getAll();

    @Query("SELECT * FROM Metadata WHERE id = :id")
    Metadata findById(int id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Metadata metadata);

    @Delete
    void delete(Metadata metadata);
}
