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
import java.util.HashMap;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;
import static android.os.Build.VERSION_CODES.Q;

public class MusicListActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_REQUEST = 1;
    private ImageButton mainActivity;
    private ImageButton nightModeButton;
    public static boolean nightMode = false;
    private ListView listView;
    private RelativeLayout relativeLayout;
    private ArrayList<Song> songList;
    public static HashMap<Song, SongNode> playlist;
    private SongListAdapter adapter;
    private Messenger mainActivityMessenger;
    private Intent musicServiceIntent;
    private Intent nightModeServiceIntent;



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
                        musicServiceIntent = new Intent(this, MusicPlayerService.class);
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
            ActivityCompat.requestPermissions(MusicListActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
        } else {
            initMusicList();
            initNightMode();
        }
    }

    public void initMusicList() {
        listView = findViewById(R.id.listView);
        songList = new ArrayList<>();
        playlist = new HashMap<>();
        getMusic();
        createPlaylist();
        adapter = new SongListAdapter(this, R.layout.adapter_view_layout, songList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected song object
                Song song = (Song) listView.getItemAtPosition(position);

                // visually highlight the song in the list view
                adapter.highlightItem(position);

                // notify music player service with the main activity messenger and the selected song
                Bundle bundle = new Bundle();
                bundle.putParcelable("mainActivityMessenger", mainActivityMessenger);
                bundle.putParcelable("song", song);
                musicServiceIntent.putExtra("musicListActivity", bundle);
                startService(musicServiceIntent);
            }
        });
    }

    public void initNightMode(){
        nightMode = false;
        nightModeButton = findViewById(R.id.btn_nightmode);
        nightModeServiceIntent = new Intent(this, MusicPlayerService.class);
        nightModeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                toggleNightMode();
            }
        });
    }

    public void toggleNightMode(){
        if (!nightMode){
            listView.setBackgroundColor(getResources().getColor(R.color.nightPrimaryDark));
            relativeLayout.setBackgroundColor(getResources().getColor(R.color.nightPrimaryDark));
            adapter.setItemsTitleTextColor(getResources().getColor(R.color.lightPrimaryWhite));
            nightModeButton.setImageResource(R.drawable.night);
            nightMode = true;

            // notify music player service with the main activity messenger
            Bundle bundle = new Bundle();
            bundle.putParcelable("mainActivityMessenger", mainActivityMessenger);
            bundle.putBoolean("nightmode", true);
            nightModeServiceIntent.putExtra("musicListNightToggle", bundle);
            startService(nightModeServiceIntent);
        }
        else{
            listView.setBackgroundColor(getResources().getColor(R.color.lightPrimaryWhite));
            relativeLayout.setBackgroundColor(getResources().getColor(R.color.lightPrimaryWhite));
            adapter.setItemsTitleTextColor(getResources().getColor(R.color.colorTextDark));
            nightModeButton.setImageResource(R.drawable.light);
            nightMode = false;

            // notify music player service with the main activity messenger
            Bundle bundle = new Bundle();
            bundle.putParcelable("mainActivityMessenger", mainActivityMessenger);
            bundle.putBoolean("nightmode", false);
            nightModeServiceIntent.putExtra("musicListNightToggle", bundle);
            startService(nightModeServiceIntent);
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

            int bucketID = songCursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_ID);
            int bucketDisplayName = songCursor.getColumnIndex(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME);
            int dataPath = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
            int dateAdded = songCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED);
            int dateModified = songCursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED);
            int displayName = songCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
            int documentID = songCursor.getColumnIndex(MediaStore.Audio.Media.DOCUMENT_ID);
            int instanceID = songCursor.getColumnIndex(MediaStore.Audio.Media.INSTANCE_ID);
            int mimeType = songCursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE);
            int originalDocumentID = songCursor.getColumnIndex(MediaStore.Audio.Media.ORIGINAL_DOCUMENT_ID);
            int relativePath = songCursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH);
            int size = songCursor.getColumnIndex(MediaStore.Audio.Media.SIZE);


            do {
                int currentID = songCursor.getInt(songID);
                String currentTitle = songCursor.getString(songTitle);
                String currentArtist = songCursor.getString(songArtist);
                String currentAlbum = songCursor.getString(songAlbum);
                String currentAlbumID = songCursor.getString(songAlbumID);
                int currentSongDuration = songCursor.getInt(songDuration);

                String currentBucketID = songCursor.getString(bucketID);
                String currentBucketDisplayName = songCursor.getString(bucketDisplayName);
                String currentDataPath = songCursor.getString(dataPath);
                String currentDateAdded = songCursor.getString(dateAdded);
                String currentDateModified = songCursor.getString(dateModified);
                String currentDisplayName = songCursor.getString(displayName);
                String currentDocumentID = songCursor.getString(documentID);
                String currentInstanceID = songCursor.getString(instanceID);
                String currentMimeType = songCursor.getString(mimeType);
                String currentOriginalDocumentID = songCursor.getString(originalDocumentID);
                String currentRelativePath = songCursor.getString(relativePath);
                String currentSize = songCursor.getString(size);

                Song song = new Song(currentID,
                        currentTitle,
                        currentArtist,
                        currentAlbum,
                        currentAlbumID,
                        currentSongDuration,
                        currentBucketID,
                        currentBucketDisplayName,
                        currentDataPath,
                        currentDateAdded,
                        currentDateModified,
                        currentDisplayName,
                        currentDocumentID,
                        currentInstanceID,
                        currentMimeType,
                        currentOriginalDocumentID,
                        currentRelativePath,
                        currentSize
                );
                songList.add(song);
            } while (songCursor.moveToNext());

            songCursor.close();
        }
    }

    public void createPlaylist(){
        int size = songList.size();
        if (size != 0) {
            Song head = songList.get(0);
            Song tail = songList.get(size - 1);
            if (size == 1) {
                SongNode songNode = new SongNode(tail, head, tail);
                playlist.put(head, songNode);
            } else {
                for (int i = 0; i < size; i++) {
                    Song song = songList.get(i);
                    SongNode songNode;
                    if (song.equals(tail)) {
                        Song prevSong = songList.get(i - 1);
                        songNode = new SongNode(prevSong, song, head);
                    } else if (song.equals(head)) {
                        Song nextSong = songList.get(i + 1);
                        songNode = new SongNode(tail, song, nextSong);
                    } else {
                        Song prevSong = songList.get(i - 1);
                        Song nextSong = songList.get(i + 1);
                        songNode = new SongNode(prevSong, song, nextSong);
                    }
                    playlist.put(song, songNode);
                }
            }
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
                        initMusicList();
                        initNightMode();
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
