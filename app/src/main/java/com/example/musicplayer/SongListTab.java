package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;

import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SongListTab#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SongListTab extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static ListView listView;
    private static SongListAdapter songListAdapter;
    private static Messenger mainActivityMessenger;
    private static MainActivity mainActivity;
    private static ArrayList<Song> userSelection;

    // TODO: Rename and change types of parameters
    private String mParam1;

    public SongListTab() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment Tab.
     */
    // TODO: Rename and change types and number of parameters
    public static SongListTab newInstance(String param1, SongListAdapter adapter, Messenger messenger, MainActivity activity) {
        SongListTab fragment = new SongListTab();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        userSelection = new ArrayList<>();
        songListAdapter = adapter;
        mainActivityMessenger = messenger;
        mainActivity = activity;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_tab_songs, container, false);
        listView = fragmentView.findViewById(R.id.fragment_listview_songs);
        listView.setAdapter(songListAdapter);

        // init intents
        final Intent musicListSelectIntent = new Intent(mainActivity, MusicPlayerService.class);
        final Intent musicListQueueIntent = new Intent(mainActivity, MusicPlayerService.class);
        final Intent addPlaylistIntent = new Intent(mainActivity, AddPlaylistActivity.class);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected song object
                Song song = (Song) listView.getItemAtPosition(position);

                // redirect the current playlist to reference the full (original) playlist
                MainActivity.setCurrent_playlist(MainActivity.getFullPlaylist());

                // change current song
                MainActivity.setCurrent_song(song);

                // notify music player service about the current song change
                musicListSelectIntent.putExtra("musicListSong", mainActivityMessenger);
                mainActivity.startService(musicListSelectIntent);
            }
        });

        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
                // obtain the selected song object and add to user selection arraylist
                Song song = (Song) listView.getItemAtPosition(position);

                if (!checked) {
                    userSelection.remove(song);
                } else {
                    userSelection.add(song);
                }

                if (userSelection.size() == 1) {
                    mode.setTitle(userSelection.get(0).getTitle());
                } else {
                    mode.setTitle(userSelection.size() + " songs selected");
                }
            }

            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.songs_menu, menu);
                MainActivity.isActionMode = true;
                MainActivity.actionMode = mode;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.createqueue:
                        Toast.makeText(mainActivity, "Creating Queue of " + userSelection.size() + " songs", Toast.LENGTH_SHORT).show();
                        // construct new current playlist, given the user selections
                        MainActivity.setCurrent_playlist(new Playlist("USER_SELECTION", userSelection));
                        MainActivity.setCurrent_song(userSelection.get(0));

                        // notify music player service to start the new song in the new playlist (queue)
                        musicListQueueIntent.putExtra("musicListSong", mainActivityMessenger);
                        mainActivity.startService(musicListQueueIntent);

                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.createplaylist:
                        Toast.makeText(mainActivity, "Creating Playlist of " + userSelection.size() + " songs", Toast.LENGTH_SHORT).show();

                        // construct named playlist and send it to addPlaylist activity
                        Playlist playlist = new Playlist(getString(R.string.Favorites), userSelection);
                        addPlaylistIntent.putExtra("addPlaylist", playlist);
                        startActivity(addPlaylistIntent);

                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                MainActivity.isActionMode = false;
                MainActivity.actionMode = null;
                userSelection.clear();
            }
        });
        return fragmentView;
    }

    public static void toggleTabColor(){
        if (MainActivity.nightMode) {
            listView.setBackgroundColor(mainActivity.getResources().getColor(R.color.nightPrimaryDark));
            songListAdapter.setItemsFrameColor(mainActivity.getResources().getColor(R.color.nightPrimaryDark));
            songListAdapter.setItemsTitleTextColor(mainActivity.getResources().getColorStateList(R.color.itemnightselectorblue));
        }
        else{
            listView.setBackgroundColor(mainActivity.getResources().getColor(R.color.lightPrimaryWhite));
            songListAdapter.setItemsFrameColor(mainActivity.getResources().getColor(R.color.lightPrimaryWhite));
            songListAdapter.setItemsTitleTextColor(mainActivity.getResources().getColorStateList(R.color.itemlightselectorblue));
        }
    }

}