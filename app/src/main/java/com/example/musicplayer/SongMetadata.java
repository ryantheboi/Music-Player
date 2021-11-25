package com.example.musicplayer;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "SongMetadata")
public class SongMetadata implements Parcelable {
    // main fields for identifying a song
    @PrimaryKey
    private int id;
    private String title;
    private String artist;

    // metadata fields
    private int played;
    private int listened;
    private String dateListened;

    /**
     * Constructor only used by database to create a Song for every row
     * All fields other than the main three are null (or 0 for ints)
     */
    public SongMetadata(int id, String title, String artist, int played, int listened, String dateListened) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.played = played;
        this.listened = listened;
        this.dateListened = dateListened;
    }

    /**
     * Constructor used to create a new SongMetadata given a Song
     * @param song the song that needs metadata to be stored by this class
     */
    public SongMetadata(Song song){
        this.id = song.getId();
        this.title = song.getTitle();
        this.artist = song.getArtist();
        this.played = 0;
        this.listened = 0;
        this.dateListened = null;
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

    public void setPlayed(int played){
        this.played = played;
    }

    public void setListened(int listened){
        this.listened = listened;
    }

    public void setDateListened(String dateListened){
        this.dateListened = dateListened;
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
    }

    public SongMetadata(Parcel in) {
        this.id = in.readInt();
        this.title = in.readString();
        this.artist = in.readString();
        this.played = in.readInt();
        this.listened = in.readInt();
        this.dateListened = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static Creator<SongMetadata> CREATOR = new Creator<SongMetadata>() {
        public SongMetadata createFromParcel(Parcel source) {
            return new SongMetadata(source);
        }

        public SongMetadata[] newArray(int size) {
            return new SongMetadata[size];
        }
    };
}
