package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Messenger;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import java.util.ArrayList;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;
import static android.os.Build.VERSION_CODES.Q;

public class MusicListActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_REQUEST = 1;
    private ImageButton mainActivity;
    private ImageButton nightModeButton;
    private boolean nightMode;
    private ListView listView;
    private RelativeLayout relativeLayout;
    private ArrayList<Song> songList;
    private SongListAdapter adapter;
    private Messenger mainActivityMessenger;
    private Intent musicListIntent;


    @Override
    @TargetApi(16)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musiclist);
        relativeLayout = findViewById(R.id.activity_musiclist);

        // obtain the intent that started this activity (should be the Main Activity)
        Intent intent = this.getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {
            for (String key : b.keySet()) {
                switch (key) {
                    case "mainActivity":
                        // get the mainActivity's messenger and forward it to MusicPlayerService
                        mainActivityMessenger = intent.getParcelableExtra("mainActivity");
                        musicListIntent = new Intent(this, MusicPlayerService.class);
                        break;
                }
            }
        }

        // init button for returning to main activity
        mainActivity = findViewById(R.id.btn_mainactivity);
        mainActivity.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                openMainActivity();
            }
        });

        // check and request for read permissions
        if (ContextCompat.checkSelfPermission(MusicListActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MusicListActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(MusicListActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            } else { // temp else branch
                ActivityCompat.requestPermissions(MusicListActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            }
        } else {
            createMusicList();
            initNightMode();
        }
    }

    public void createMusicList() {
        listView = findViewById(R.id.listView);
        songList = new ArrayList<>();
        getMusic();
        adapter = new SongListAdapter(this, R.layout.adapter_view_layout, songList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected song object
                Song song = (Song) listView.getItemAtPosition(position);

                // notify music player service with the main activity messenger and the selected song
                Bundle bundle = new Bundle();
                bundle.putParcelable("mainActivityMessenger", mainActivityMessenger);
                bundle.putParcelable("song", song);
                musicListIntent.putExtra("musicListActivity", bundle);
                startService(musicListIntent);
            }
        });
    }

    public void initNightMode(){
        nightMode = false;
        nightModeButton = findViewById(R.id.btn_nightmode);
        nightModeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                toggleNightMode();
            }
        });
    }

    public void toggleNightMode(){
        if (!nightMode){
            listView.setBackgroundColor(Color.parseColor("#232123"));
            relativeLayout.setBackgroundColor(Color.parseColor("#232123"));
            adapter.setItemTitleTextColor("#F5F5F5");
            nightModeButton.setImageResource(R.drawable.night);
            nightMode = true;
        }
        else{
            listView.setBackgroundColor(Color.parseColor("#F4F4F4"));
            relativeLayout.setBackgroundColor(Color.parseColor("#F4F4F4"));
            adapter.setItemTitleTextColor("#030303");
            nightModeButton.setImageResource(R.drawable.light);
            nightMode = false;
        }
    }

    @TargetApi(Q)
    public void getMusic() {
        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri, null, null, null, null);
        if (songCursor != null && songCursor.moveToFirst()) {
            int songID = songCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int songTitle = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int songArtist = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int songAlbum = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            int songAlbumID = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            int songDuration = songCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);

            do {
                int currentID = songCursor.getInt(songID);
                String currentTitle = songCursor.getString(songTitle);
                String currentArtist = songCursor.getString(songArtist);
                String currentAlbum = songCursor.getString(songAlbum);
                int currentAlbumID = songCursor.getInt(songAlbumID);
                int currentSongDuration = songCursor.getInt(songDuration);

                Song song = new Song(currentID,
                                    currentTitle,
                                    currentArtist,
                                    currentAlbum,
                                    currentAlbumID,
                                    currentSongDuration
                                    );
                songList.add(song);
            } while (songCursor.moveToNext());

            songCursor.close();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        switch (requestCode){
            case MY_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    if (ContextCompat.checkSelfPermission(MusicListActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                        createMusicList();
                    }
                }
                else{
                    Toast.makeText(this, "Permission denied..", Toast.LENGTH_SHORT).show();
                }
        }
    }

    public void openMainActivity(){
        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        mainActivityIntent.addFlags(FLAG_ACTIVITY_REORDER_TO_FRONT );
        startActivity(mainActivityIntent);
    }
}
