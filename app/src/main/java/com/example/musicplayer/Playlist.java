package com.example.musicplayer;

import android.annotation.TargetApi;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

@Entity(tableName = "Playlists")
public class Playlist implements Parcelable {
    @PrimaryKey
    private int id;

    private String name;
    private ArrayList<Song> songList;

    @Ignore
    private HashMap<Song, SongNode> songHashMap;

    /**
     * Constructor used by database to create a playlist for every row
     */
    public Playlist(int id, String name, ArrayList<Song> songList) {
        this.id = id;
        this.name = name;
        this.songList = new ArrayList<>(songList);
        this.songHashMap = createHashMap(songList);
    }

    /**
     * Overloaded Constructor which does not need id field parameter
     * id is manually generated when the playlist is expected to persist in database storage
     * otherwise, id is 0 for temporary playlists (e.g. queues)
     */
    @Ignore
    public Playlist(String name, ArrayList<Song> songList) {
        this.id = 0;
        this.name = name;
        this.songList = new ArrayList<>(songList);
        this.songHashMap = createHashMap(songList);
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<Song> getSongList() {
        return songList;
    }

    public HashMap<Song, SongNode> getSongHashMap(){
        return songHashMap;
    }

    public String getSizeString() {
        return Long.toString(songList.size());
    }
    public int getSize(){
        return songList.size();
    }

    /**
     * creates a hashmap given an arraylist of songs
     * the hashmap is a mapping of Song to SongNode
     * the SongNodes are created to form a circular doubly linked list
     *
     * @param songList arraylist containing the songs to populate the playlist
     * @return playlist hashmap that contains every Song in songList, each mapping to a SongNode
     */
    public static HashMap<Song, SongNode> createHashMap(ArrayList<Song> songList) {
        HashMap<Song, SongNode> playlist = new HashMap<>();

        int size = songList.size();
        if (size != 0) {
            Song head = songList.get(0);
            Song tail = songList.get(size - 1);
            if (size == 1) {
                SongNode songNode = new SongNode(tail, head, tail);
                playlist.put(head, songNode);
            } else {
                for (int i = 0; i < size; i++) {
                    Song song = songList.get(i);
                    SongNode songNode;
                    if (song.equals(tail)) {
                        Song prevSong = songList.get(i - 1);
                        songNode = new SongNode(prevSong, song, head);
                    } else if (song.equals(head)) {
                        Song nextSong = songList.get(i + 1);
                        songNode = new SongNode(tail, song, nextSong);
                    } else {
                        Song prevSong = songList.get(i - 1);
                        Song nextSong = songList.get(i + 1);
                        songNode = new SongNode(prevSong, song, nextSong);
                    }
                    playlist.put(song, songNode);
                }
            }
        }
        return playlist;
    }

    /**
     * gets the song before the current song being played in this playlist
     * @return the previous song
     */
    public Song getPrevSong(){
        Song current_song = MainActivity.getCurrent_song();
        SongNode songNode = songHashMap.get(current_song);
        return songNode.getPrev();
    }

    /**
     * gets the song after the current song being played in this playlist
     * @return the next song
     */
    public Song getNextSong(){
        Song current_song = MainActivity.getCurrent_song();
        SongNode songNode = songHashMap.get(current_song);
        return songNode.getNext();
    }

    /**
     * Adds every new song in the given playlist to this playlist's collection of songs
     * Reconstructs the underlying hashmap with the updated collection of songs
     * @param playlist the playlist to append to this playlist
     * @return true if extend succeeded, false otherwise
     */
    public boolean extend(Playlist playlist){
        // create songHashMap if it doesn't already exist (because it's not included in parcel)
        if (songHashMap == null) {
            songHashMap = createHashMap(songList);
        }

        // loop through list of songs to check if a song exists before adding
        ArrayList<Song> playlist_songs = playlist.getSongList();
        int playlist_songs_size = playlist_songs.size();
        int dupe_counter = 0;
        for (Song song : playlist_songs){
            if (!songHashMap.containsKey(song)){
                songList.add(song);
            }
            else{
                dupe_counter += 1;
            }
        }

        // all songs that were to be added are duplicates, extend unnecessary
        if (dupe_counter == playlist_songs_size) {
            return false;
        }

        // reconstruct hashmap with the updated song list
        songHashMap = createHashMap(songList);
        return true;
    }

    /**
     * Removes the collection of songs from this playlist
     * @param songs the songs to remove from this playlist
     */
    public void removeAll(Collection<Song> songs){
        songList.removeAll(songs);

        // reconstruct hashmap with the updated song list
        songHashMap = createHashMap(songList);
    }

    /**
     * Replaces this songlist with the songlist from another playlist
     * @param playlist the playlist whose songlist shall be adopted
     */
    public void adoptSongList(Playlist playlist){
        this.songList = new ArrayList<>();
        this.songList.addAll(playlist.getSongList());

        // reconstruct hashmap with the updated song list
        songHashMap = createHashMap(songList);
    }

    /**
     * Rearranges the playlist such that the playlist begins from the provided song
     * @param song the song to begin the playlist
     */
    public void rearrangePlaylist(Song song){
        // get index of current song to split the current list of songs
        int splitIdx = songList.indexOf(song);

        List<Song> firstHalf = songList.subList(0, splitIdx);
        List<Song> secondHalf = songList.subList(splitIdx, songList.size());

        // reconstruct the new songslist and hash map
        songList = new ArrayList<>();
        songList.addAll(secondHalf);
        songList.addAll(firstHalf);
        songHashMap = createHashMap(songList);
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
    @TargetApi(29)
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.name);
        dest.writeParcelableList(songList, flags);
    }

    @TargetApi(29)
    public Playlist(Parcel in) {
        this.id = in.readInt();
        this.name = in.readString();
        this.songList = new ArrayList<>();
        in.readParcelableList(this.songList, Song.class.getClassLoader());
    }

    public static Creator<Playlist> CREATOR = new Creator<Playlist>() {
        public Playlist createFromParcel(Parcel source) {
            return new Playlist(source);
        }

        public Playlist[] newArray(int size) {
            return new Playlist[size];
        }
    };

    @Override
    public int hashCode() {
        try {
            int prime = 31;
            int result = 1;
            result = prime * result + id;
            result = prime * result + name.hashCode();
            return result;
        }catch (Exception e){
            return super.hashCode();
        }
    }

    @Override
    public boolean equals(Object obj) {
        try {
            return this.id == ((Playlist) obj).id;
        }catch (Exception e){
            return super.equals(obj);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}