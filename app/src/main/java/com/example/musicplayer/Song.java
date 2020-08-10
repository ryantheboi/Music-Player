package com.example.musicplayer;

import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable {
    private String title;
    private String artist;
    private String album;
    private int albumID;

    public Song(String title, String artist, String album, int albumID) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.albumID = albumID;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public int getAlbumID() {
        return albumID;
    }

    public void setAlbumID(int albumID) {
        this.albumID = albumID;
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
        dest.writeString(this.title);
        dest.writeString(this.artist);
        dest.writeString(this.album);
        dest.writeInt(this.albumID);
    }

    public Song(Parcel in) {
        this.title = in.readString();
        this.artist = in.readString();
        this.album = in.readString();
        this.albumID = in.readInt();
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