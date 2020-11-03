package com.example.musicplayer;

import androidx.annotation.NonNull;

public class SongNode {
    private Song prev;
    private Song value;
    private Song next;

    public SongNode(Song prev, Song value, Song next){
        this.prev = prev;
        this.value = value;
        this.next = next;
    }

    public Song getPrev() {
        return prev;
    }

    public Song getValue() {
        return value;
    }

    public Song getNext() {
        return next;
    }

    @NonNull
    @Override
    public String toString() {
        return "{\"prev\":\"" + prev.toString() +
                "\", \"value\":\"" + value.toString() +
                "\", \"next\":\"" + next.toString() +
                "\"}";
    }
}
