package com.example.musicplayer;

import android.os.Messenger;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {
    int numTabs;
    SongListAdapter adapter;
    Messenger mainActivityMessenger;
    MainActivity mainActivity;

    public PagerAdapter(FragmentManager fm, int numTabs, SongListAdapter adapter, Messenger mainActivityMessenger, MainActivity mainActivity){
        super(fm);
        this.numTabs = numTabs;
        this.adapter = adapter;
        this.mainActivityMessenger = mainActivityMessenger;
        this.mainActivity = mainActivity;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position){
            case 0:
                return SongListTab.newInstance("Tab1", adapter, mainActivityMessenger, mainActivity);
            case 1:
                return new PlaylistTab();
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return numTabs;
    }
}
