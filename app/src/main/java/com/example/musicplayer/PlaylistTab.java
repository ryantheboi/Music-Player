package com.example.musicplayer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import androidx.fragment.app.Fragment;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SongListTab#newInstance} factory method to
 * create an instance of this fragment.
 */
public class PlaylistTab extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static ListView listView;
    private static PlaylistAdapter playlistAdapter;
    private static MainActivity mainActivity;

    // TODO: Rename and change types of parameters
    private String mParam1;

    public PlaylistTab() {
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
    public static PlaylistTab newInstance(String param1, PlaylistAdapter adapter, MainActivity activity) {
        PlaylistTab fragment = new PlaylistTab();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        playlistAdapter = adapter;
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
        View fragmentView = inflater.inflate(R.layout.fragment_tab_playlists, container, false);
        listView = fragmentView.findViewById(R.id.fragment_listview_playlists);
        listView.setAdapter(playlistAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected playlist object
                Playlist playlist = (Playlist) listView.getItemAtPosition(position);

                Intent playlistIntent = new Intent(mainActivity, PlaylistActivity.class);
                playlistIntent.putExtra("playlist", playlist);
                mainActivity.startActivity(playlistIntent);
            }
        });

        return fragmentView;
    }

    public static void toggleTabColor(){
        listView.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        playlistAdapter.setItemsFrameColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        playlistAdapter.setItemsTitleTextColor(mainActivity.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.ITEM_TEXT_COLOR)));
        playlistAdapter.setItemsSizeTextColor(mainActivity.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
    }
}