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

    /**
     * Update only the isPlaying metadata, by id
     */
    @Query("UPDATE Metadata SET isPlaying = :isPlaying WHERE id =:id")
    void updateIsPlaying(int id, boolean isPlaying);

    /**
     * Update only the seekPosition metadata, by id
     */
    @Query("UPDATE Metadata SET seekPosition = :seekPosition WHERE id =:id")
    void updateSeekPosition(int id, int seekPosition);

    /**
     * Update only the values associated with Songtab listview position (scroll index and scroll offset), by id
     */
    @Query("UPDATE Metadata SET songtab_scrollindex = :songtab_scrollindex, songtab_scrolloffset = :songtab_scrolloffset WHERE id =:id")
    void updateSongTab(int id, int songtab_scrollindex, int songtab_scrolloffset);

    /**
     * Update only the theme resource id metadata, by id
     */
    @Query("UPDATE Metadata SET themeResourceId = :themeResourceId WHERE id =:id")
    void updateTheme(int id, int themeResourceId);

    /**
     * Update only the isLargeAlbumArt metadata, by id
     */
    @Query("UPDATE Metadata SET isLargeAlbumArt = :isLargeAlbumArt WHERE id =:id")
    void updateIsLargeAlbumArt(int id, boolean isLargeAlbumArt);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Metadata metadata);

    @Delete
    void delete(Metadata metadata);
}
