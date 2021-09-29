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
     * Update only the song index metadata, by id
     */
    @Query("UPDATE Metadata SET songIndex = :songIndex WHERE id =:id")
    void updateSongIndex(int id, int songIndex);

    /**
     * Update only the isShuffled metadata, by id
     */
    @Query("UPDATE Metadata SET isShuffled = :isShuffled WHERE id =:id")
    void updateIsShuffled(int id, boolean isShuffled);

    /**
     * Update only the repeatStatus metadata, by id
     */
    @Query("UPDATE Metadata SET repeatStatus = :repeatStatus WHERE id =:id")
    void updateRepeatStatus(int id, int repeatStatus);

    /**
     * Update only the isMediaStorePlaylistsImported metadata, by id
     */
    @Query("UPDATE Metadata SET isMediaStorePlaylistsImported = :isMediaStorePlaylistsImported WHERE id =:id")
    void updateIsMediaStorePlaylistsImported(int id, boolean isMediaStorePlaylistsImported);

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
     * Update only the isAlbumArtCircular metadata, by id
     */
    @Query("UPDATE Metadata SET isAlbumArtCircular = :isAlbumArtCircular WHERE id =:id")
    void updateIsAlbumArtCircular(int id, boolean isAlbumArtCircular);

    /**
     * Update only the random seed metadata, by id
     */
    @Query("UPDATE Metadata SET random_seed = :random_seed WHERE id =:id")
    void updateRandomSeed(int id, int random_seed);

    /**
     * Update only the isAvailable metadata, by id
     */
    @Query("UPDATE Metadata SET isAvailable = :isAvailable WHERE id =:id")
    void updateIsAvailable(int id, boolean isAvailable);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Metadata metadata);

    @Delete
    void delete(Metadata metadata);
}
