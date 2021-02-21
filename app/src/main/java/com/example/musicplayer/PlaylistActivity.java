package com.example.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

public class PlaylistActivity extends Activity {

    private Playlist playlist;
    private ListView listView;
    private ImageButton back_btn;
    private TextView playlist_name_tv;
    private TextView playlist_size_tv;
    private SongListAdapter songListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_playlist);

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


        // initialize listview and adapter using the songs in the playlist
        listView = findViewById(R.id.listview_playlist_songs);
        songListAdapter = new SongListAdapter(this, R.layout.adapter_view_layout, playlist.getSongList(), this);
        listView.setAdapter(songListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected song object
                Song song = (Song) listView.getItemAtPosition(position);

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
    }
}
