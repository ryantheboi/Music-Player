package com.example.musicplayer;

import android.annotation.TargetApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

@Entity(tableName = "Playlists")
public class Playlist implements Parcelable {
    @PrimaryKey
    private int id;

    private String name;
    private ArrayList<Song> songList;
    private int transientId;
    private long dateAdded;

    @Ignore
    private HashMap<Song, SongNode> songHashMap;

    private static final int MAX_TRANSIENTS = 3;

    /**
     * Constructor used by database to create a playlist for every row
     */
    public Playlist(int id, String name, ArrayList<Song> songList, int transientId, long dateAdded) {
        this.id = id;
        this.name = name;
        this.songList = new ArrayList<>(songList);
        this.songHashMap = createHashMap(songList);
        this.transientId = transientId;
        this.dateAdded = dateAdded;
    }

    /**
     * Overloaded Constructor which does not need id or transientId
     * id is generated when the playlist is expected to persist in database storage
     * otherwise, id is 0 for current playlist
     */
    @Ignore
    public Playlist(String name, ArrayList<Song> songList) {
        this.id = 0;
        this.name = name;
        this.songList = new ArrayList<>(songList);
        this.songHashMap = createHashMap(songList);
        this.dateAdded = System.currentTimeMillis();
    }

    /**
     * Overloaded Constructor which does not need dateAdded
     */
    @Ignore
    public Playlist(int id, String name, ArrayList<Song> songList, int transientId) {
        this.id = id;
        this.name = name;
        this.songList = new ArrayList<>(songList);
        this.songHashMap = createHashMap(songList);
        this.transientId = transientId;
        this.dateAdded = System.currentTimeMillis();
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ArrayList<Song> getSongList() {
        return songList;
    }

    public int getTransientId() {
        return transientId;
    }

    public long getDateAdded(){
        return dateAdded;
    }

    public HashMap<Song, SongNode> getSongHashMap(){
        return songHashMap;
    }

    public int getSize(){
        return songList.size();
    }

    public String getSizeString() {
        return Long.toString(songList.size());
    }

    public String getDateAddedString(){
        return MusicDetailsFragment.convertDateTimeString(String.valueOf(dateAdded));
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTransientId(int transientId){
        this.transientId = transientId;
    }

    /**
     * does the current number of transient playlists reach or exceed the max allowed?
     * @return true if number of transient playlists are greater than or equal to the max allowed
     *         false otherwise
     */
    public static boolean isNumTransientsMaxed(){
        // count the current number of temporary (transient) queues
        int num_transients = 0;
        ArrayList<Playlist> allPlaylists = MainActivity.getPlaylists();
        for (Playlist p : allPlaylists) {
            if (p.getTransientId() > 0) {
                num_transients++;
            }
        }
        return num_transients >= MAX_TRANSIENTS;
    }

    /**
     * creates a temporary (transient) playlist which can be replaced if NUM_TRANSIENTS == MAX_TRANSIENTS
     * if the max number of transient playlists already exist, then the oldest's id and name will be used
     * otherwise, create new transient playlist with transient id less than or equal to MAX_TRANSIENTS
     * @param songList the list of songs to put in the transient playlist
     * @return a new playlist that has a transient id > 0
     */
    public static Playlist createTransientPlaylist(ArrayList<Song> songList){
        // count the current number of temporary (transient) queues
        int num_transients = 0;
        int[] transient_ids = new int[Playlist.MAX_TRANSIENTS + 1];
        Playlist transient_playlist = null;
        Playlist oldest_transient_playlist = null;
        ArrayList<Playlist> allPlaylists = MainActivity.getPlaylists();
        for (Playlist p : allPlaylists){
            int p_transientId = p.getTransientId();
            if (p_transientId > 0){
                transient_ids[p_transientId] = 1;
                num_transients++;
                if (oldest_transient_playlist == null){
                    oldest_transient_playlist = p;
                }
                else{
                    if (p.getDateAdded() < oldest_transient_playlist.getDateAdded()){
                        oldest_transient_playlist = p;
                    }
                }
            }

            // maximum transient playlists reached, stop counting
            // replace existing transient playlist songs with songList
            if (num_transients == MAX_TRANSIENTS){
                return new Playlist(oldest_transient_playlist.getId(), oldest_transient_playlist.getName(), songList, oldest_transient_playlist.getTransientId());
            }
        }

        // else, construct new transient playlist songs with songList
        for (int curr_transient_id = 1; curr_transient_id < Playlist.MAX_TRANSIENTS + 1; curr_transient_id++){
            int transient_id_flag = transient_ids[curr_transient_id];
            if (transient_id_flag == 0){
                // transient id currently does not exist and may be used
                transient_playlist = new Playlist(DatabaseRepository.generatePlaylistId(), "TEMP_QUEUE_" + curr_transient_id, songList, curr_transient_id);
                break;
            }
        }

        return transient_playlist;
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
     * creates a random subset playlist from this playlist, given a number of songs desired
     * if the number of songs desired is more than the number of songs in this playlist,
     * then this playlist will be returned as it is
     * @param numSongs the number of songs desired in this playlist's subset
     * @return new playlist containing a subset of songs in this playlist
     */
    public Playlist createRandomSubsetPlaylist(long numSongs){
        int playlist_size = getSize();
        if (numSongs >= playlist_size){
            return this;
        }
        else{
            ArrayList<Song> randomSongsList = new ArrayList<>();
            ArrayList<Integer> randomSongIndexes = new ArrayList<Integer>();
            for (int i = 0; i < playlist_size; i++) {
                randomSongIndexes.add(i);
            }
            Collections.shuffle(randomSongIndexes);
            for (int i = 0; i < numSongs; i++) {
                randomSongsList.add(songList.get(randomSongIndexes.get(i)));
            }
            return new Playlist("Random Playlist Subset", randomSongsList);
        }
    }

    /**
     * creates a new playlist from a random subset of songs in this playlist,
     * maximizing the value of all songs in the random subset and given a time constraint in minutes.
     * uses tabulation (bottom up dynamic programming) approach
     * @param timer_minutes the number of minutes that this playlist should be
     * @return new playlist within the timer constraint
     */
    public Playlist createTimerPlaylist(int timer_minutes)
    {
        int timer_seconds = timer_minutes * 60;
        long numSongs = Math.round(timer_seconds / 1.5);

        Playlist randomPlaylist = createRandomSubsetPlaylist(numSongs);
        int[] times = randomPlaylist.getTimesInSecondsArray();
        int[] values = randomPlaylist.getValuesArray();

        int n = randomPlaylist.getSize();
        int[][] K = new int[n + 1][timer_seconds + 1];

        // build table K[][] in bottom up manner
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= timer_seconds; j++) {
                if (i == 0 || j == 0)
                    K[i][j] = 0;
                else if (times[i - 1] <= j)
                    K[i][j] = Math.max(
                            values[i - 1] + K[i - 1][j - times[i - 1]],
                            K[i - 1][j]);
                else
                    K[i][j] = K[i - 1][j];
            }
        }

        // the maximum value possible with the random playlist of songs, given the timer constraint
        int maxValue = K[n][timer_seconds];

        // use the table to populate timer playlist
        int remaining_value = maxValue;
        int remaining_seconds = timer_seconds;
        ArrayList<Song> randomSongsList = randomPlaylist.getSongList();
        ArrayList<Song> timerSongsList = new ArrayList<>();
        for (int i = n; i > 0 && maxValue > 0; i--) {

            // either the value comes from
            // (K[i-1][j]) or from
            // (values[i-1] + K[i-1][j-times[i-1]])
            // if it comes from the latter, then song is included in timer playlist
            if (remaining_value == K[i - 1][remaining_seconds]) {
            }

            // this song is included
            else {
                Song song = randomSongsList.get(i-1);
                timerSongsList.add(song);

                // deduct this song's value and remaining seconds from total
                remaining_value = remaining_value - values[i - 1];
                remaining_seconds = remaining_seconds - times[i - 1];
            }
        }
        return new Playlist(timer_seconds/60 + " Minutes - Playlist", timerSongsList);
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
     * gets a array containing the duration, in seconds, of every song in this playlist, in songList order
     * @return array of song durations, in seconds
     */
    public int[] getTimesInSecondsArray(){
        int playlist_size = getSize();
        int[] songTimes = new int[playlist_size];
        for (int i = 0; i < playlist_size; i ++){
            songTimes[i] = songList.get(i).getDuration() / 1000;
        }
        return songTimes;
    }

    /**
     * gets a array containing the utility value of every song in this playlist, in songList order
     * utility value of a song can be derived from how often the song is listened to, etc.
     * TODO currently, all songs just have the same utility value of 1
     * @return array of song values
     */
    public int[] getValuesArray(){
        int playlist_size = getSize();
        int[] songValues = new int[playlist_size];
        for (int i = 0; i < playlist_size; i ++){
            songValues[i] = 1;
        }
        return songValues;
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
     * Randomly reorder every song in this playlist, given a seed
     * @return a new playlist that was randomly shuffled based on the original (this playlist)
     */
    public Playlist shufflePlaylist(int seed){
        ArrayList<Song> shuffled_songlist = new ArrayList<>(songList);

        Random r = new Random(seed);
        // start from the last element and swap one by one, excluding first element
        int songlist_size = shuffled_songlist.size();
        for (int i = songlist_size - 1; i > 0; i--){
            int random_idx = r.nextInt(i);
            Song temp = shuffled_songlist.get(i);
            shuffled_songlist.set(i, shuffled_songlist.get(random_idx));
            shuffled_songlist.set(random_idx, temp);
        }

        return new Playlist(name, shuffled_songlist);
    }

    /**
     * Restores the original order of every song in this playlist, given the original seed
     * @return a new playlist representing the original (unshuffled) playlist
     */
    public Playlist unshufflePlaylist(int seed){
        ArrayList<Song> shuffled_songlist = new ArrayList<>(songList);

        Random r = new Random(seed);

        // obtain the order in which the random indexes were generated
        int songlist_size = shuffled_songlist.size();
        int[] random_indexes = new int[songlist_size];
        for (int i = songlist_size - 1; i > 0; i--){
            int random_idx = r.nextInt(i);
            random_indexes[i] = random_idx;
        }

        // unshuffle this playlist
        for (int i = 1; i < songlist_size; i++){
            Song temp = shuffled_songlist.get(i);
            shuffled_songlist.set(i, shuffled_songlist.get(random_indexes[i]));
            shuffled_songlist.set(random_indexes[i], temp);
        }

        return new Playlist(name, shuffled_songlist);
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