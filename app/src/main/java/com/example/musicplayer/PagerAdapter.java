package com.example.musicplayer;

import android.os.Messenger;
import android.os.Parcelable;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {
    public static final int SONGS_TAB = 0;
    public static final int PLAYLISTS_TAB = 1;

    int numTabs;
    SongListAdapter songListAdapter;
    PlaylistAdapter playlistAdapter;
    Messenger mainActivityMessenger;
    MainActivity mainActivity;
    SongListTab songListTab;
    PlaylistTab playlistTab;

    public PagerAdapter(FragmentManager fm, int numTabs, SongListAdapter songListAdapter, PlaylistAdapter playlistAdapter, Messenger mainActivityMessenger, MainActivity mainActivity){
        super(fm);
        this.numTabs = numTabs;
        this.songListAdapter = songListAdapter;
        this.playlistAdapter = playlistAdapter;
        this.mainActivityMessenger = mainActivityMessenger;
        this.mainActivity = mainActivity;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case SONGS_TAB:
                songListTab = SongListTab.newInstance("Tab1", songListAdapter, mainActivityMessenger, mainActivity);
                return songListTab;
            case PLAYLISTS_TAB:
                playlistTab = PlaylistTab.newInstance("Tab2", playlistAdapter, mainActivityMessenger, mainActivity);
                return playlistTab;
            default:
                return null;
        }
    }

    @Override
    public Parcelable saveState() {
        // fragments may be destroyed without state being saved, so adapter doesn't auto create
        return null;
    }

    @Override
    public int getCount() {
        return numTabs;
    }
}
