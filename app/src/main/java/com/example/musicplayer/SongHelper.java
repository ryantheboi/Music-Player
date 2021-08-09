package com.example.musicplayer;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.provider.MediaStore;

import static android.os.Build.VERSION_CODES.Q;

/**
 * Helper class containing all operations related to Song objects
 */
public class SongHelper {

    private static int cursorIndex_songID;
    private static int cursorIndex_songTitle;
    private static int cursorIndex_songArtist;
    private static int cursorIndex_songAlbum;
    private static int cursorIndex_songAlbumID;
    private static int cursorIndex_songDuration;
    private static int cursorIndex_bucketID;
    private static int cursorIndex_bucketDisplayName;
    private static int cursorIndex_dataPath;
    private static int cursorIndex_dateAdded;
    private static int cursorIndex_dateModified;
    private static int cursorIndex_displayName;
    private static int cursorIndex_documentID;
    private static int cursorIndex_instanceID;
    private static int cursorIndex_mimeType;
    private static int cursorIndex_originalDocumentID;
    private static int cursorIndex_relativePath;
    private static int cursorIndex_size;

    public static Song createSong(Cursor songCursor){
        int currentID = songCursor.getInt(cursorIndex_songID);
        String currentTitle = songCursor.getString(cursorIndex_songTitle);
        String currentArtist = songCursor.getString(cursorIndex_songArtist);
        String currentAlbum = songCursor.getString(cursorIndex_songAlbum);
        String currentAlbumID = songCursor.getString(cursorIndex_songAlbumID);
        int currentSongDuration = songCursor.getInt(cursorIndex_songDuration);

        String currentBucketID = songCursor.getString(cursorIndex_bucketID);
        String currentBucketDisplayName = songCursor.getString(cursorIndex_bucketDisplayName);
        String currentDataPath = songCursor.getString(cursorIndex_dataPath);
        String currentDateAdded = songCursor.getString(cursorIndex_dateAdded);
        String currentDateModified = songCursor.getString(cursorIndex_dateModified);
        String currentDisplayName = songCursor.getString(cursorIndex_displayName);
        String currentDocumentID = songCursor.getString(cursorIndex_documentID);
        String currentInstanceID = songCursor.getString(cursorIndex_instanceID);
        String currentMimeType = songCursor.getString(cursorIndex_mimeType);
        String currentOriginalDocumentID = songCursor.getString(cursorIndex_originalDocumentID);
        String currentRelativePath = songCursor.getString(cursorIndex_relativePath);
        String currentSize = songCursor.getString(cursorIndex_size);

        return new Song(currentID,
                currentTitle,
                currentArtist,
                currentAlbum,
                currentAlbumID,
                currentSongDuration,
                currentBucketID,
                currentBucketDisplayName,
                currentDataPath,
                currentDateAdded,
                currentDateModified,
                currentDisplayName,
                currentDocumentID,
                currentInstanceID,
                currentMimeType,
                currentOriginalDocumentID,
                currentRelativePath,
                currentSize
        );
    }

    /**
     * Helper function to convert time in milliseconds to HH:MM:SS format
     */
    public static String convertTime(int timeInMS){
        int timeInSeconds = timeInMS / 1000;

        int seconds = timeInSeconds % 3600 % 60;
        int minutes = timeInSeconds % 3600 / 60;
        int hours = timeInSeconds / 3600;

        String HH, MM, SS;
        if (hours == 0){
            MM = ((minutes  < 10) ? "" : "") + minutes;
            SS = ((seconds  < 10) ? "0" : "") + seconds;
            return MM + ":" + SS;
        }
        else {
            HH = ((hours    < 10) ? "0" : "") + hours;
            MM = ((minutes  < 10) ? "0" : "") + minutes;
            SS = ((seconds  < 10) ? "0" : "") + seconds;
        }

        return HH + ":" + MM + ":" + SS;
    }

    /**
     * Helper function to set all song cursor column indexes given the song cursor
     * @param songCursor the cursor which contains all column indexes from MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
     */
    @TargetApi(Q)
    public static void setSongCursorIndexes(Cursor songCursor){
        cursorIndex_songID = songCursor.getColumnIndex(MediaStore.Audio.Media._ID);
        cursorIndex_songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        cursorIndex_songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        cursorIndex_songAlbum = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        cursorIndex_songAlbumID = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        cursorIndex_songDuration = songCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

        cursorIndex_bucketID = songCursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_ID);
        cursorIndex_bucketDisplayName = songCursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME);
        cursorIndex_dataPath = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        cursorIndex_dateAdded = songCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
        cursorIndex_dateModified = songCursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED);
        cursorIndex_displayName = songCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
        cursorIndex_documentID = songCursor.getColumnIndex(MediaStore.Audio.Media.DOCUMENT_ID);
        cursorIndex_instanceID = songCursor.getColumnIndex(MediaStore.Audio.Media.INSTANCE_ID);
        cursorIndex_mimeType = songCursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE);
        cursorIndex_originalDocumentID = songCursor.getColumnIndex(MediaStore.Audio.Media.ORIGINAL_DOCUMENT_ID);
        cursorIndex_relativePath = songCursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH);
        cursorIndex_size = songCursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
    }
}
