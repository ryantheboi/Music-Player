package com.example.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AddPlaylistActivity extends Activity {

    private Playlist addPlaylist;
    private ListView listView;
    private RelativeLayout addPlaylist_layout;
    private ImageButton back_btn;
    private Button addPlaylist_button;
    private TextView addTo_tv;
    private TextView addPlaylist_tv;
    private PlaylistAdapter playlistAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_addplaylist);
        addPlaylist_layout = findViewById(R.id.activity_playlist);

        // obtain the intent that started this activity (should contain an extra with a Playlist)
        Intent intent = this.getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {
            for (String key : b.keySet()) {
                if (key.equals("addPlaylist")) {
                    // get the song which needs its details to be displayed
                    addPlaylist = intent.getParcelableExtra("addPlaylist");
                    break;
                }
            }
        }

        // init textviews
        addTo_tv = findViewById(R.id.textview_addTo);
        addPlaylist_tv = findViewById(R.id.textview_addPlaylist);

        // set the window layout for a clean look
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout((int) (width), (int) (height));

        // initialize listview and adapter using the songs in the playlist
        listView = findViewById(R.id.listview_playlists);
        playlistAdapter = new PlaylistAdapter(this, R.layout.adapter_playlist_layout, MainActivity.getPlaylists(), this);
        listView.setAdapter(playlistAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected playlist object
                Playlist playlist = (Playlist) listView.getItemAtPosition(position);
                playlist.extend(addPlaylist);
                finish();
            }
        });

        // initialize button to return to previous activity
        back_btn = findViewById(R.id.ibtn_addPlaylist_back);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // initialize button to add a new playlist
        addPlaylist_button = findViewById(R.id.btn_addPlaylist);
        addPlaylist_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // add new playlist to the playlist tab
                System.out.println("new playlist not added");
            }
        });

        // adjust activity colors for nightmode
        if (MainActivity.nightMode){
            addTo_tv.setTextColor(getResources().getColor(R.color.colorTextPrimaryLight));
            addPlaylist_tv.setTextColor(getResources().getColor(R.color.colorTextPrimaryLight));
            playlistAdapter.setItemsFrameColor(getResources().getColor(R.color.nightPrimaryDark));
            playlistAdapter.setItemsTitleTextColor(getResources().getColorStateList(R.color.itemnightselectorblue));
            addPlaylist_layout.setBackgroundColor(getResources().getColor(R.color.nightPrimaryDark));
        }
        else{
            addTo_tv.setTextColor(getResources().getColor(R.color.colorTextPrimaryDark));
            addPlaylist_tv.setTextColor(getResources().getColor(R.color.colorTextPrimaryDark));
            playlistAdapter.setItemsFrameColor(getResources().getColor(R.color.lightPrimaryWhite));
            playlistAdapter.setItemsTitleTextColor(getResources().getColorStateList(R.color.itemlightselectorblue));
            addPlaylist_layout.setBackgroundColor(getResources().getColor(R.color.lightPrimaryWhite));
        }
    }
}
