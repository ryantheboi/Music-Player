package com.example.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class PlaylistActivity extends Activity {

    private Playlist playlist;
    private ListView listView;
    private RelativeLayout playlist_layout;
    private ImageButton back_btn;
    private TextView playlist_name_tv;
    private TextView playlist_size_tv;
    private SongListAdapter songListAdapter;
    private static ArrayList<Song> userSelection = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_playlist);
        playlist_layout = findViewById(R.id.activity_playlist);

        // obtain the intent that started this activity (should contain an extra with a Playlist)
        Intent intent = this.getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {
            for (String key : b.keySet()) {
                if (key.equals("playlist")) {
                    // get the song which needs its details to be displayed
                    playlist = intent.getParcelableExtra("playlist");
                    break;
                }
            }
        }

        // initialize and set text in textviews
        playlist_size_tv = findViewById(R.id.textview_playlist_size);
        playlist_name_tv = findViewById(R.id.textview_playlist_name);
        String playlist_size = playlist.getSize() + " Songs";
        playlist_size_tv.setText(playlist_size);
        playlist_name_tv.setText(playlist.getName());

        // set the window layout for a clean look
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout((int) (width), (int) (height));

        // init intents
        final Intent musicListSelectIntent = new Intent(this, MusicPlayerService.class);
        final Intent musicListQueueIntent = new Intent(this, MusicPlayerService.class);
        final Intent addPlaylistIntent = new Intent(this, AddPlaylistActivity.class);

        // initialize listview and adapter using the songs in the playlist
        listView = findViewById(R.id.listview_playlist_songs);
        songListAdapter = new SongListAdapter(this, R.layout.adapter_view_layout, playlist.getSongList(), this);
        listView.setAdapter(songListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected song object
                Song song = (Song) listView.getItemAtPosition(position);

                // redirect the current playlist to reference the songs in this playlist
                MainActivity.setCurrent_playlist(new Playlist(playlist.getName(), playlist.getSongList()));

                // change current song
                MainActivity.setCurrent_song(song);

                // notify music player service about the current song change
                musicListSelectIntent.putExtra("musicListSong", "");
                startService(musicListSelectIntent);
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
                        // construct new current playlist, given the user selections
                        MainActivity.setCurrent_playlist(new Playlist("USER_SELECTION", userSelection));
                        MainActivity.setCurrent_song(userSelection.get(0));

                        // notify music player service to start the new song in the new playlist (queue)
                        musicListQueueIntent.putExtra("musicListSong", "");
                        startService(musicListQueueIntent);

                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.createplaylist:
                        // construct named playlist
                        Playlist playlist = new Playlist(getString(R.string.Favorites), userSelection);
                        addPlaylistIntent.putExtra("addPlaylist", playlist);
                        startActivity(addPlaylistIntent);
                        //mainActivity.addPlaylist(playlist);

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

        // initialize button to return to previous activity
        back_btn = findViewById(R.id.ibtn_playlist_back);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // adjust activity colors for nightmode
        if (MainActivity.nightMode){
            playlist_name_tv.setTextColor(getResources().getColor(R.color.colorTextPrimaryLight));
            songListAdapter.setItemsFrameColor(getResources().getColor(R.color.nightPrimaryDark));
            songListAdapter.setItemsTitleTextColor(getResources().getColorStateList(R.color.itemnightselectorblue));
            playlist_layout.setBackgroundColor(getResources().getColor(R.color.nightPrimaryDark));
        }
        else{
            playlist_name_tv.setTextColor(getResources().getColor(R.color.colorTextPrimaryDark));
            songListAdapter.setItemsFrameColor(getResources().getColor(R.color.lightPrimaryWhite));
            songListAdapter.setItemsTitleTextColor(getResources().getColorStateList(R.color.itemlightselectorblue));
            playlist_layout.setBackgroundColor(getResources().getColor(R.color.lightPrimaryWhite));
        }
    }
}
