package com.example.musicplayer;

import android.os.Environment;
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
    public static final String EXPORT_DIRECTORY = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_MUSIC;
    private static final char COMMA_SEPARATOR = ',';
    private static final String DASH_SEPARATOR = " - ";
    private static final String M3U_EXTENSION = ".m3u";
    private static final String M3U_HEADER = "#EXTM3U";
    private static final String M3U_TRACK_INFO_DIRECTIVE = "#EXTINF:";

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
            Logger.logException(e, "M3U");
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
            Logger.logException(e, "M3U");
        }

        String[] temp_arraySplit = filepath.split("/");
        String playlistName = temp_arraySplit[temp_arraySplit.length - 1].replaceAll(M3U_EXTENSION, "");
        return new Playlist(DatabaseRepository.generatePlaylistId(), playlistName, m3uSongs, 0);
    }

    /**
     * Writes a playlist to external storage in standard .m3u format
     * @param playlist The playlist to export to a .m3u file
     * @return true if write succeeded, false otherwise
     */
    public static boolean exportM3U(Playlist playlist){
        try {
            File directory = new File(EXPORT_DIRECTORY);
            if (!directory.exists()) {
                directory.mkdir();
            }
            String filename = EXPORT_DIRECTORY + '/' + playlist.getName() + M3U_EXTENSION;
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename, false));
            writer.append(M3U_HEADER);
            writer.append('\n');

            ArrayList<Song> songsList = playlist.getSongList();
            for (Song song : songsList){
                writer.append(M3U_TRACK_INFO_DIRECTIVE);
                writer.append(Integer.toString(song.getDuration() / 1000));
                writer.append(COMMA_SEPARATOR);
                writer.append(song.getArtist());
                writer.append(DASH_SEPARATOR);
                writer.append(song.getTitle());
                writer.append('\n');
                writer.append(song.getDataPath());
                writer.append('\n');
            }
            writer.close();
            return true;
        }catch (Exception e){
            Logger.logException(e, "M3U");
            return false;
        }
    }

    /**
     * Recursively searches a directory to find all .m3u files
     * @param directory the directory to begin recursively searching for .m3u files
     * @return an arraylist of strings representing the path to a .m3u file
     */
    public static ArrayList<String> findM3U(String directory){
        ArrayList<String> m3uFilePaths = new ArrayList<>();
        File root_directory = new File(directory);
        File[] children = root_directory.listFiles();
        if (children != null) {
            int num_children = children.length;
            for (int i = 0; i < num_children; i++) {
                File child = children[i];
                if (child.toString().endsWith(M3U_EXTENSION)) {
                    m3uFilePaths.add(child.getAbsolutePath());
                } else {
                    m3uFilePaths.addAll(findM3U(child.getAbsolutePath()));
                }
            }
        }

        return m3uFilePaths;
    }
}
