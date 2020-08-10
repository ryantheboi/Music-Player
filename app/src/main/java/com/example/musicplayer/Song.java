package com.example.musicplayer;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

public class Song implements Parcelable{
    private String title;
    private String artist;
    private String album;
    private Bitmap albumArt;

    public Song(String title, String artist, String album, Bitmap albumArt)
    {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.albumArt = albumArt;
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

    public Bitmap getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(Bitmap albumArt) {
        this.albumArt = albumArt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * write the object to a parcelable instance that can be passed through Intents
     * @param dest the parcelable object
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.title);
        dest.writeString(this.artist);
        dest.writeString(this.album);
        dest.writeValue(this.albumArt);
    }

}