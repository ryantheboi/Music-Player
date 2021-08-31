package com.example.musicplayer;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "Songs")
public class Song implements Parcelable {

    // main fields for identifying a song
    @PrimaryKey
    private int id;
    private String title;
    private String artist;

    // metadata fields
    private int played;
    private int listened;
    private String dateListened;

    // other fields from mediastore
    @Ignore
    private String album;
    @Ignore
    private String albumID;
    @Ignore
    private int duration;
    @Ignore
    private String bucketID;
    @Ignore
    private String bucketDisplayName;
    @Ignore
    private String dateAdded;
    @Ignore
    private String dateModified;
    @Ignore
    private String dataPath;
    @Ignore
    private String displayName;
    @Ignore
    private String documentID;
    @Ignore
    private String instanceID;
    @Ignore
    private String mimeType;
    @Ignore
    private String originalDocumentID;
    @Ignore
    private String relativePath;
    @Ignore
    private String size;

    public static Song EMPTY_SONG = new Song(0, "0", "0", "0", "0", 0, "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0");

    /**
     * Constructor only used by database to create a Song for every row
     * All fields other than the main three are null (or 0 for ints)
     */
    public Song(int id, String title, String artist, int played, int listened, String dateListened){
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.played = played;
        this.listened = listened;
        this.dateListened = dateListened;
    }

    public Song(int id, String title, String artist, String album, String albumID, int duration,
                String bucketID, String bucketDisplayName, String dataPath, String dateAdded,
                String dateModified, String displayName, String documentID,
                String instanceID, String mimeType, String originalDocumentID,
                String relativePath, String size) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.albumID = albumID;
        this.duration = duration;

        this.bucketID = bucketID;
        this.bucketDisplayName = bucketDisplayName;
        this.dataPath = dataPath;
        this.dateAdded = dateAdded;
        this.dateModified = dateModified;
        this.displayName = displayName;
        this.originalDocumentID = originalDocumentID;
        this.documentID = documentID;
        this.instanceID = instanceID;
        this.mimeType = mimeType;
        this.relativePath = relativePath;
        this.size = size;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public int getPlayed(){
        return played;
    }

    public int getListened(){
        return listened;
    }

    public String getDateListened(){
        return dateListened;
    }

    public String getAlbum() {
        return album;
    }

    public String getAlbumID() {
        return albumID;
    }

    public int getDuration(){
        return duration;
    }

    public String getBucketID() {
        return bucketID;
    }

    public String getBucketDisplayName() {
        return bucketDisplayName;
    }

    public String getDateAdded() {
        return dateAdded;
    }

    public String getDataPath() {
        return dataPath;
    }

    public String getDateModified() {
        return dateModified;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDocumentID() {
        return documentID;
    }

    public String getInstanceID() {
        return instanceID;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getOriginalDocumentID() {
        return originalDocumentID;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getSize() {
        return size;
    }

    public void setPlayed(int played){
        this.played = played;
    }

    public void setListened(int listened){
        this.listened = listened;
    }

    public void setDateListened(String dateListened){
        this.dateListened = dateListened;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * write the object to a parcelable instance that can be passed through Intents
     *
     * @param dest the parcelable object
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.title);
        dest.writeString(this.artist);
        dest.writeInt(this.played);
        dest.writeInt(this.listened);
        dest.writeString(this.dateListened);
        dest.writeString(this.album);
        dest.writeString(this.albumID);
        dest.writeInt(this.duration);
        dest.writeString(this.bucketID);
        dest.writeString(this.bucketDisplayName);
        dest.writeString(this.dataPath);
        dest.writeString(this.dateAdded);
        dest.writeString(this.dateModified);
        dest.writeString(this.displayName);
        dest.writeString(this.documentID);
        dest.writeString(this.instanceID);
        dest.writeString(this.mimeType);
        dest.writeString(this.originalDocumentID);
        dest.writeString(this.relativePath);
        dest.writeString(this.size);
    }

    public Song(Parcel in) {
        this.id = in.readInt();
        this.title = in.readString();
        this.artist = in.readString();
        this.played = in.readInt();
        this.listened = in.readInt();
        this.dateListened = in.readString();
        this.album = in.readString();
        this.albumID = in.readString();
        this.duration = in.readInt();
        this.bucketID = in.readString();
        this.bucketDisplayName = in.readString();
        this.dataPath = in.readString();
        this.dateAdded = in.readString();
        this.dateModified = in.readString();
        this.displayName = in.readString();
        this.documentID = in.readString();
        this.instanceID = in.readString();
        this.mimeType = in.readString();
        this.originalDocumentID = in.readString();
        this.relativePath = in.readString();
        this.size = in.readString();
    }

    @Override
    public int hashCode() {
        try {
            int prime = 31;
            int result = 1;
            result = prime * result + id;
            result = prime * result + albumID.hashCode();
            result = prime * result + title.hashCode();
            result = prime * result + artist.hashCode();
            result = prime * result + album.hashCode();
            return result;
        }catch (Exception e){
            return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        try {
            return this.id == ((Song) obj).id &&
                    this.title.equals(((Song) obj).title) &&
                    this.artist.equals(((Song) obj).artist);
        }catch (Exception e){
            return super.equals(obj);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return title;
    }

    public static Creator<Song> CREATOR = new Creator<Song>() {
        public Song createFromParcel(Parcel source) {
            return new Song(source);
        }

        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}