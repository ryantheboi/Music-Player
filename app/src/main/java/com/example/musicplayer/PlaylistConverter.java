package com.example.musicplayer;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Used to allow db storage of the ArrayList of Songs in a Playlist
 */
public class PlaylistConverter {
    @TypeConverter
    public static ArrayList<Song> fromString(String value) {
        Type listType = new TypeToken<ArrayList<Song>>() {}.getType();
        return new Gson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromArrayList(ArrayList<Song> list) {
        Gson gson = new Gson();
        String json = gson.toJson(list);
        return json;
    }
}