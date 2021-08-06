package com.example.musicplayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class used to handle all operations related to .m3u files
 * operations include importing and exporting playlists
 * based on the standard m3u format specification
 */
public class M3U {

    /**
     * Creates and returns a playlist object given the path to a .m3u file
     * The playlist will only contain songs that exist in the device at the time of the app launch
     * @param filepath string path to the .m3u file
     * @return Playlist containing songs specified in the .m3u file
     */
    public static Playlist parseM3U(String filepath){
        File file = new File(filepath);

        // populate arraylist with song filepaths
        ArrayList<String> songPaths = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            for (String line = br.readLine(); line != null; line = br.readLine()) {
                // ignore M3U comments
                if (!line.startsWith("#")) {
                    songPaths.add(line);
                }
            }
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        // construct mapping of filepath to song using the current full list of songs
        ArrayList<Song> fullSongsList = MainActivity.getFullPlaylist().getSongList();
        HashMap<String, Song> pathSongMap = new HashMap<>();
        for (Song s : fullSongsList){
            pathSongMap.put(s.getDataPath(), s);
        }

        // populate new playlist with songs based on the paths specified in the m3u file
        ArrayList<Song> m3uSongs = new ArrayList<>();
        try{
            for (String path : songPaths){
                Song song = pathSongMap.get(path);
                if (song != null) {
                    m3uSongs.add(song);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        String[] temp_arraySplit = filepath.split("/");
        String playlistName = temp_arraySplit[temp_arraySplit.length - 1].replaceAll(".m3u", "");
        return new Playlist(DatabaseRepository.generatePlaylistId(), playlistName, m3uSongs);
    }

    /**
     * Writes a playlist to external storage in standard .m3u format
     * @param playlist The playlist to export to a .m3u file
     * @return true if write succeeded, false otherwise
     */
    public static boolean exportM3U(Playlist playlist){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("/storage/emulated/0/Playlists/" + playlist.getName() +".m3u", true));
            writer.append("#EXTM3U\n");

            ArrayList<Song> songsList = playlist.getSongList();
            for (Song song : songsList){
                writer.append("#EXTINF:" + (song.getDuration() / 1000) + "," + song.getArtist() + " - " + song.getTitle() + "\n");
                writer.append(song.getDataPath() + "\n");
            }
            writer.close();
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
}
