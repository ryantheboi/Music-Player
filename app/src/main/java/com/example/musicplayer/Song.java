package com.example.musicplayer;

public class Song {
    private String title;
    private String artist;
    private String album;
    private int albumID;

    public Song(String title, String artist, String album, int albumID){
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
}