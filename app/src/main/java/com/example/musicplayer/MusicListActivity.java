package com.example.musicplayer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.palette.graphics.Palette;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;
import static android.os.Build.VERSION_CODES.Q;
import static com.example.musicplayer.Notifications.CHANNEL_ID_1;

public class MusicListActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_REQUEST = 1;
    private ImageButton mainActivity;
    private ImageButton nightModeButton;
    public static boolean nightMode = false;
    private ListView listView;
    private RelativeLayout musicListRelativeLayout;
    private ArrayList<Song> songList;
    public static HashMap<Song, SongNode> playlist;
    public static HashMap<Song, SongNode> fullPlaylist;
    private SongListAdapter adapter;
    private Messenger mainActivityMessenger;
    private Intent musicServiceIntent;
    private Intent nightModeServiceIntent;
    public static boolean isActionMode = false;
    private ActionMode actionMode = null;
    private ArrayList<Song> userSelection;

    // sliding up panel
    private boolean largeAlbumArt;
    private ImageView albumArt;
    private ImageView pauseplay_background;
    private ImageView nextbtn_background;
    private ImageView prevbtn_background;
    private Animation pauseplayAnim;
    private Animation nextbtnAnim;
    private Animation prevbtnAnim;
    private Animation pauseplayBackgroundAnim;
    private Animation nextbtnBackgroundAnim;
    private Animation prevbtnBackgroundAnim;
    private Button albumArt_btn;
    private ImageButton info_btn;
    private ImageButton pauseplay;
    private ImageButton next_btn;
    private ImageButton prev_btn;
    private AnimationDrawable mainAnimation;
    private GradientDrawable gradient1;
    private GradientDrawable gradient2;
    private GradientDrawable pauseplay_background_gradient;
    private GradientDrawable nextbtn_background_gradient;
    private GradientDrawable prevbtn_background_gradient;
    private SeekBar seekBar;
    private TextView musicPosition;
    private TextView musicDuration;
    private TextView songName;
    private TextView artistName;
    private MediaSessionCompat mediaSession;
    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private Notification notificationChannel1;
    private Intent mainPausePlayIntent;
    private Intent mainPrevIntent;
    private Intent mainNextIntent;
    private Intent infoIntent;
    private Intent notificationPauseplayIntent;
    private Intent notificationPrevIntent;
    private Intent notificationNextIntent;
    private Intent seekBarProgressIntent;
    private Intent seekBarSeekIntent;
    private static Song current_song = Song.EMPTY_SONG;
    private Palette.Swatch vibrantSwatch;
    private Palette.Swatch darkVibrantSwatch;
    private Palette.Swatch dominantSwatch;
    private RelativeLayout mainActivityRelativeLayout;
    private LinearLayout slidingUpMenuLayout;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private ImageView slidingUp_albumArt;
    private TextView slidingUp_songName;
    private TextView slidingUp_artistName;
    private Intent slidingUp_pauseplayIntent;
    private Intent slidingUp_prevIntent;
    private Intent slidingUp_nextIntent;
    private ImageButton slidingUp_prev_btn;
    private ImageButton slidingUp_pauseplay_btn;
    private ImageButton slidingUp_next_btn;
    private Animation slidingUp_pauseplaybtnAnim;
    private Animation slidingUp_nextbtnAnim;
    private Animation slidingUp_prevbtnAnim;


    @Override
    @TargetApi(16)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_musiclist);
        musicListRelativeLayout = findViewById(R.id.activity_musiclist);
        initMusicList();

        // initialize main sliding up panel
        initMainAnimation();

        initAlbumArt();

        initMainButtons();

        initSeekbar();

        initInfoButton();

        initSlidingUpPanel();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaSession = new MediaSessionCompat(this, "media");
        notificationManager = NotificationManagerCompat.from(this);
        showNotification();

//        // obtain the intent that started this activity (should be the Main Activity)
//        Intent intent = this.getIntent();
//        Bundle b = intent.getExtras();
//        if (b != null) {
//            for (String key : b.keySet()) {
//                switch (key) {
//                    case "mainActivity":
//                        // get mainActivity's messenger and prepare intent for MusicPlayerService
//                        mainActivityMessenger = intent.getParcelableExtra("mainActivity");
//                        musicServiceIntent = new Intent(this, MusicPlayerService.class);
//
//                        break;
//                }
//            }
//        }
        mainActivityMessenger = new Messenger(new MessageHandler());
        musicServiceIntent = new Intent(this, MusicPlayerService.class);

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
        userSelection = new ArrayList<>();
        songList = new ArrayList<>();
        fullPlaylist = new HashMap<>();
        playlist = new HashMap<>();
        getMusic(); // populates songList
        fullPlaylist = createPlaylist(songList);
        adapter = new SongListAdapter(this, R.layout.adapter_view_layout, songList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected song object
                Song song = (Song) listView.getItemAtPosition(position);

                // redirect the current playlist to reference the full (original) playlist
                playlist = fullPlaylist;

                // visually highlight the song in the list view
                adapter.highlightItem(song);

                // notify music player service with the main activity messenger and the selected song
                Bundle bundle = new Bundle();
                bundle.putParcelable("mainActivityMessenger", mainActivityMessenger);
                bundle.putParcelable("song", song);
                musicServiceIntent.putExtra("musicListActivity", bundle);
                startService(musicServiceIntent);
            }
        });

        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
                // obtain the selected song object and add to user selection arraylist
                Song song = (Song) listView.getItemAtPosition(position);

                if (!checked){
                    userSelection.remove(song);
                }
                else{
                    userSelection.add(song);
                }

                if (userSelection.size() == 1){
                    mode.setTitle(userSelection.get(0).getTitle());
                }
                else{
                    mode.setTitle(userSelection.size() + " songs selected");
                }
            }

            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
                mode.getMenuInflater().inflate(R.menu.example_menu, menu);
                isActionMode = true;
                actionMode = mode;
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
                switch (item.getItemId()){
                    case R.id.createqueue:
                        Toast.makeText(MusicListActivity.this, "Creating Queue of " + userSelection.size() + " songs", Toast.LENGTH_SHORT).show();
                        // construct new current playlist, given the user selections
                        playlist = createPlaylist(userSelection);

                        // notify music player service to start the new song in the new playlist (queue)
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("mainActivityMessenger", mainActivityMessenger);
                        bundle.putParcelable("song", userSelection.get(0));
                        musicServiceIntent.putExtra("musicListActivity", bundle);
                        startService(musicServiceIntent);

                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.createplaylist:
                        Toast.makeText(MusicListActivity.this, "Creating Playlist of " + userSelection.size() + " songs", Toast.LENGTH_SHORT).show();
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                isActionMode = false;
                actionMode = null;
                userSelection.clear();
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
            musicListRelativeLayout.setBackgroundColor(getResources().getColor(R.color.nightPrimaryDark));
            slidingUpPanelLayout.setBackgroundColor(getResources().getColor(R.color.nightPrimaryDark));
            slidingUp_songName.setTextColor(getResources().getColor(R.color.lightPrimaryWhite));
            adapter.setItemsTitleTextColor(getResources().getColorStateList(R.color.itemnightselectorblue));
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
            musicListRelativeLayout.setBackgroundColor(getResources().getColor(R.color.lightPrimaryWhite));
            slidingUpPanelLayout.setBackgroundColor(getResources().getColor(R.color.lightPrimaryWhite));
            slidingUp_songName.setTextColor(getResources().getColor(R.color.colorTextDark));
            adapter.setItemsTitleTextColor(getResources().getColorStateList(R.color.itemlightselectorblue));
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

    /**
     * creates a playlist given an arraylist of songs
     * the playlist is a hashmap of Song to SongNode
     * the SongNodes are created to form a circular doubly linked list
     *
     * @param songList arraylist containing the songs to populate the playlist
     * @return playlist hashmap that contains every Song in songList, each mapping to a SongNode
     */
    public HashMap<Song, SongNode> createPlaylist(ArrayList<Song> songList){
        HashMap<Song, SongNode> playlist = new HashMap<>();

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
        return playlist;
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

    /**
     * Initialize the sliding up panel for controlling the visibility of the main activity
     */
    public void initSlidingUpPanel(){
        // init image, text, and buttons on sliding menu
        slidingUpMenuLayout = findViewById(R.id.sliding_menu);
        slidingUp_albumArt = findViewById(R.id.sliding_albumart);
        slidingUp_songName = findViewById(R.id.sliding_title);
        slidingUp_artistName = findViewById(R.id.sliding_artist);
        initSlidingUpPanelButtons();


        // init slide and click controls for slide panel layout
        slidingUpPanelLayout = findViewById(R.id.slidingPanel);
        slidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                slidingUpMenuLayout.setAlpha(1 - slideOffset);
                mainActivityRelativeLayout.setAlpha(slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
            }
        });

        // enables expand clicking only for the menu (and disables it for the entire relativelayout)
        slidingUpPanelLayout.getChildAt(1).setOnClickListener(null);
        slidingUpMenuLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
                }
            }
        });
    }

    /**
     * Initializes the three main buttons in sliding up panel:
     * Pauseplay, previous, and next
     * Each one will trigger a unique event from MusicPlayerService
     */
    @SuppressLint("ClickableViewAccessibility")
    public void initSlidingUpPanelButtons(){
        Messenger slidingUpPanelMessenger = new Messenger(new MessageHandler());

        // init pauseplay button with touch and click, and appropriate animations
        slidingUp_pauseplay_btn = findViewById(R.id.sliding_btn_play);
        slidingUp_pauseplaybtnAnim = AnimationUtils.loadAnimation(this, R.anim.blink_animation);

        slidingUp_pauseplayIntent = new Intent(this, MusicPlayerService.class);
        slidingUp_pauseplayIntent.putExtra("pauseplay", slidingUpPanelMessenger);

        slidingUp_pauseplay_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                slidingUp_pauseplay_btn.startAnimation(slidingUp_pauseplaybtnAnim);
                startService(slidingUp_pauseplayIntent);
            }
        });
        slidingUp_pauseplay_btn.setOnTouchListener(new View.OnTouchListener() {
            private Rect viewBoundary;
            private boolean ignore; // true to ignore all touches, false otherwise
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ignore = false;
                        viewBoundary = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        slidingUp_pauseplay_btn.animate().scaleX((float) 0.8).scaleY((float) 0.8);
                        break;
                    case MotionEvent.ACTION_UP:
                        slidingUp_pauseplay_btn.animate().scaleX(1).scaleY(1);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // if movement is greater than 60 pixels from the original press point
                        if (!ignore) {
                            if (!viewBoundary.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                                slidingUp_pauseplay_btn.animate().scaleX(1).scaleY(1);
                                ignore = true;
                            }
                        }
                        break;
                }
                return ignore;
            }
        });

        // init next button with touch and click, and appropriate animations
        slidingUp_next_btn = findViewById(R.id.sliding_btn_next);
        slidingUp_nextbtnAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_inout_animation);

        slidingUp_nextIntent = new Intent(this, MusicPlayerService.class);
        slidingUp_nextIntent.putExtra("next", slidingUpPanelMessenger);

        slidingUp_next_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                slidingUp_next_btn.startAnimation(slidingUp_nextbtnAnim);
                startService(slidingUp_nextIntent);
            }
        });

        slidingUp_next_btn.setOnTouchListener(new View.OnTouchListener() {
            private Rect viewBoundary;
            private boolean ignore; // true to ignore all touches, false otherwise
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ignore = false;
                        viewBoundary = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        slidingUp_next_btn.animate().scaleX((float) 0.8).scaleY((float) 0.8);
                        break;
                    case MotionEvent.ACTION_UP:
                        slidingUp_next_btn.animate().scaleX(1).scaleY(1);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // if movement is greater than 60 pixels from the original press point
                        if (!ignore) {
                            if (!viewBoundary.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                                slidingUp_next_btn.animate().scaleX(1).scaleY(1);
                                ignore = true;
                            }
                        }
                        break;
                }
                return ignore;
            }
        });

        // init prev button with touch and click, and appropriate animations
        slidingUp_prev_btn = findViewById(R.id.sliding_btn_prev);
        slidingUp_prevbtnAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_inout_animation);

        slidingUp_prevIntent = new Intent(this, MusicPlayerService.class);
        slidingUp_prevIntent.putExtra("prev", slidingUpPanelMessenger);

        slidingUp_prev_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                slidingUp_prev_btn.startAnimation(slidingUp_prevbtnAnim);
                startService(slidingUp_prevIntent);
            }
        });

        slidingUp_prev_btn.setOnTouchListener(new View.OnTouchListener() {
            private Rect viewBoundary;
            private boolean ignore; // true to ignore all touches, false otherwise
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ignore = false;
                        viewBoundary = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        slidingUp_prev_btn.animate().scaleX((float) 0.8).scaleY((float) 0.8);
                        break;
                    case MotionEvent.ACTION_UP:
                        slidingUp_prev_btn.animate().scaleX(1).scaleY(1);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // if movement is greater than 60 pixels from the original press point
                        if (!ignore) {
                            if (!viewBoundary.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                                slidingUp_prev_btn.animate().scaleX(1).scaleY(1);
                                ignore = true;
                            }
                        }
                        break;
                }
                return ignore;
            }
        });
    }

    /**
     * Initialize the animated gradient based on night mode and album art
     * Gradients are set up to be mutated, here
     */
    @TargetApi(16)
    public void initMainAnimation(){
        // set up gradients that can be mutated
        gradient1 = (GradientDrawable) ResourcesCompat.getDrawable(this.getResources(), R.drawable.gradient_default1, null);
        gradient2 = (GradientDrawable) ResourcesCompat.getDrawable(this.getResources(), R.drawable.gradient_default2, null);
        gradient1.mutate();
        gradient2.mutate();

        // 6 second animated gradients with 3 second transitions
        mainActivityRelativeLayout = findViewById(R.id.mainlayout);
        mainAnimation = new AnimationDrawable();
        mainAnimation.addFrame(gradient1, 6000);
        mainAnimation.addFrame(gradient2, 6000);
        mainAnimation.setEnterFadeDuration(3000);
        mainAnimation.setExitFadeDuration(3000);
        mainAnimation.setOneShot(false);
        mainActivityRelativeLayout.setBackground(mainAnimation);
        mainAnimation.start();
    }

    /**
     * Initialize the mutable album art view and the transparent button
     * On button click, scales the view and button with an animation
     */
    public void initAlbumArt(){
        // init album art button and textviews for song details
        albumArt = findViewById(R.id.circularImageView);
        albumArt_btn = findViewById(R.id.toggle_largeAlbumArt);
        songName = findViewById(R.id.song_name);
        artistName = findViewById(R.id.artist_name);
        largeAlbumArt = true;
        albumArt_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLargeAlbumArt();
            }
        });
    }

    /**
     * Initializes the three main buttons in main activity:
     * Pauseplay, previous, and next
     * Each one will trigger a unique event from MusicPlayerService
     */
    @SuppressLint("ClickableViewAccessibility")
    public void initMainButtons(){
        Messenger mainMessenger = new Messenger(new MessageHandler());

        // init pauseplay button with touch and click, and appropriate animations
        pauseplay = findViewById((R.id.btn_play));
        pauseplay_background = findViewById((R.id.round_play_background));
        pauseplayAnim = AnimationUtils.loadAnimation(this, R.anim.blink_animation);
        pauseplayBackgroundAnim = AnimationUtils.loadAnimation(this, R.anim.blink_animation_background);
        pauseplay_background_gradient = (GradientDrawable) pauseplay_background.getBackground().getCurrent();

        mainPausePlayIntent = new Intent(this, MusicPlayerService.class);
        mainPausePlayIntent.putExtra("pauseplay", mainMessenger);

        pauseplay.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                pauseplay.startAnimation(pauseplayAnim);
                pauseplay_background.setVisibility(View.VISIBLE);
                pauseplay_background.startAnimation(pauseplayBackgroundAnim);
                pauseplay_background.setVisibility(View.INVISIBLE);
                startService(mainPausePlayIntent);
            }
        });
        pauseplay.setOnTouchListener(new View.OnTouchListener() {
            private Rect viewBoundary;
            private boolean ignore; // true to ignore all touches, false otherwise
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ignore = false;
                        viewBoundary = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        pauseplay_background.clearAnimation();
                        pauseplay_background.setVisibility(View.VISIBLE);
                        pauseplay_background.animate().alpha((float) 0.3).scaleX((float) 0.8).scaleY((float) 0.8);
                        pauseplay.animate().scaleX((float) 0.8).scaleY((float) 0.8);
                        break;
                    case MotionEvent.ACTION_UP:
                        pauseplay_background.animate().scaleX(1).scaleY(1);
                        pauseplay.animate().scaleX(1).scaleY(1);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // if movement is greater than 60 pixels from the original press point
                        if (!ignore) {
                            if (!viewBoundary.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                                pauseplay_background.setVisibility(View.VISIBLE);
                                pauseplay_background.startAnimation(pauseplayBackgroundAnim);
                                pauseplay_background.setVisibility(View.INVISIBLE);
                                pauseplay_background.animate().scaleX(1).scaleY(1);
                                pauseplay.animate().scaleX(1).scaleY(1);
                                ignore = true;
                            }
                        }
                        break;
                }
                return ignore;
            }
        });

        // init next button with touch and click, and appropriate animations
        next_btn = findViewById((R.id.btn_next));
        nextbtn_background = findViewById((R.id.round_next_background));
        nextbtnAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_inout_animation);
        nextbtnBackgroundAnim = AnimationUtils.loadAnimation(this, R.anim.blink_animation_background);
        nextbtn_background_gradient = (GradientDrawable) nextbtn_background.getBackground().getCurrent();

        mainNextIntent = new Intent(this, MusicPlayerService.class);
        mainNextIntent.putExtra("next", mainMessenger);

        next_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                nextbtn_background.setVisibility(View.VISIBLE);
                next_btn.startAnimation(nextbtnAnim);
                nextbtn_background.startAnimation(nextbtnBackgroundAnim);
                nextbtn_background.setVisibility(View.INVISIBLE);
                startService(mainNextIntent);
            }
        });

        next_btn.setOnTouchListener(new View.OnTouchListener() {
            private Rect viewBoundary;
            private boolean ignore; // true to ignore all touches, false otherwise
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ignore = false;
                        viewBoundary = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        nextbtn_background.clearAnimation();
                        nextbtn_background.setVisibility(View.VISIBLE);
                        nextbtn_background.animate().alpha((float) 0.3).scaleX((float) 0.8).scaleY((float) 0.8);
                        next_btn.animate().scaleX((float) 0.8).scaleY((float) 0.8);
                        break;
                    case MotionEvent.ACTION_UP:
                        nextbtn_background.animate().scaleX(1).scaleY(1);
                        next_btn.animate().scaleX(1).scaleY(1);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // if movement is greater than 60 pixels from the original press point
                        if (!ignore) {
                            if (!viewBoundary.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                                nextbtn_background.setVisibility(View.VISIBLE);
                                nextbtn_background.startAnimation(nextbtnBackgroundAnim);
                                nextbtn_background.setVisibility(View.INVISIBLE);
                                nextbtn_background.animate().scaleX(1).scaleY(1);
                                next_btn.animate().scaleX(1).scaleY(1);
                                ignore = true;
                            }
                        }
                        break;
                }
                return ignore;
            }
        });

        // init prev button with touch and click, and appropriate animations
        prev_btn = findViewById((R.id.btn_prev));
        prevbtn_background = findViewById((R.id.round_prev_background));
        prevbtnAnim = AnimationUtils.loadAnimation(this, R.anim.zoom_inout_animation);
        prevbtnBackgroundAnim = AnimationUtils.loadAnimation(this, R.anim.blink_animation_background);
        prevbtn_background_gradient = (GradientDrawable) prevbtn_background.getBackground().getCurrent();

        mainPrevIntent = new Intent(this, MusicPlayerService.class);
        mainPrevIntent.putExtra("prev", mainMessenger);

        prev_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                prevbtn_background.setVisibility(View.VISIBLE);
                prev_btn.startAnimation(prevbtnAnim);
                prevbtn_background.startAnimation(prevbtnBackgroundAnim);
                prevbtn_background.setVisibility(View.INVISIBLE);
                startService(mainPrevIntent);
            }
        });

        prev_btn.setOnTouchListener(new View.OnTouchListener() {
            private Rect viewBoundary;
            private boolean ignore; // true to ignore all touches, false otherwise
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        ignore = false;
                        viewBoundary = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                        prevbtn_background.clearAnimation();
                        prevbtn_background.setVisibility(View.VISIBLE);
                        prevbtn_background.animate().alpha((float) 0.3).scaleX((float) 0.8).scaleY((float) 0.8);
                        prev_btn.animate().scaleX((float) 0.8).scaleY((float) 0.8);
                        break;
                    case MotionEvent.ACTION_UP:
                        prevbtn_background.animate().scaleX(1).scaleY(1);
                        prev_btn.animate().scaleX(1).scaleY(1);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // if movement is greater than 60 pixels from the original press point
                        if (!ignore) {
                            if (!viewBoundary.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                                prevbtn_background.setVisibility(View.VISIBLE);
                                prevbtn_background.startAnimation(prevbtnBackgroundAnim);
                                prevbtn_background.setVisibility(View.INVISIBLE);
                                prevbtn_background.animate().scaleX(1).scaleY(1);
                                prev_btn.animate().scaleX(1).scaleY(1);
                                ignore = true;
                            }
                        }
                        break;
                }
                return ignore;
            }
        });
    }

    /**
     * Initializes the seekbar and the textviews for current position and max duration
     * Time is converted from milliseconds to HH:MM:SS format
     * Textview for current position is updated upon user changing the seekbar
     */
    public void initSeekbar(){
        // init seekbar and textviews
        seekBar = findViewById(R.id.seekBar);
        musicPosition = findViewById(R.id.music_position);
        musicDuration = findViewById(R.id.music_duration);

        // set the seekbar & textview duration and sync with mediaplayer
        Intent seekBarDurationIntent = new Intent(this, MusicPlayerService.class);
        seekBarProgressIntent = new Intent(this, MusicPlayerService.class);
        seekBarSeekIntent = new Intent(this, MusicPlayerService.class);
        Messenger mainMessenger = new Messenger(new MessageHandler());
        seekBarDurationIntent.putExtra("seekbarDuration", mainMessenger);
        startService(seekBarDurationIntent);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String time = convertTime(progress);
                musicPosition.setText(time);
                if (fromUser) {
                    seekBarSeekIntent.putExtra("seekbarSeek", progress);
                    startService(seekBarSeekIntent);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    /**
     * Initializes the button for displaying details about a song in a scrollview
     * Upon clicking the button, the current song will be sent in the intent
     */
    public void initInfoButton(){
        info_btn = findViewById(R.id.btn_info);
        infoIntent = new Intent(this, MusicDetailsActivity.class);
        info_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                infoIntent.putExtra("currentSong", current_song);
                startActivity(infoIntent);
            }
        });
    }


    @TargetApi(19)
    public void showNotification(){
        Bitmap largeImage = BitmapFactory.decodeResource(getResources(), R.drawable.kaminomanimani);
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

        // create intents for the notification action buttons
        Messenger notificationMessenger = new Messenger(new MessageHandler());
        notificationPrevIntent = new Intent(this, MusicPlayerService.class).putExtra("prev", notificationMessenger);
        notificationPauseplayIntent =  new Intent(this, MusicPlayerService.class).putExtra("pauseplay", notificationMessenger);
        notificationNextIntent = new Intent(this, MusicPlayerService.class).putExtra("next", notificationMessenger);

        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID_1)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_music)
                .setContentTitle("song")
                .setContentText("artist")
                .setLargeIcon(largeImage)
                .addAction(R.drawable.ic_prev24dp, "prev", PendingIntent.getService(this, 0, notificationPrevIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_play24dp, "play", PendingIntent.getService(this, 1, notificationPauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_next24dp, "next", PendingIntent.getService(this, 2, notificationNextIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.getSessionToken()));

        notificationChannel1 = notificationBuilder.build();
        notificationManager.notify(1, notificationChannel1);
    }

    /**
     * helper method used to swap the two main gradients to white and vibrant swatch, if it exists
     * swaps to white and dominant if vibrant swatch doesn't exist
     * swaps to white and yellow if neither swatch exists
     */
    @TargetApi(16)
    public void swapVibrantGradient(){
        songName.setTextColor(getResources().getColor(R.color.colorTextDark));
        if (vibrantSwatch != null){
            gradient1.setColors(new int[]{Color.WHITE, vibrantSwatch.getRgb()});
            gradient1.setOrientation(GradientDrawable.Orientation.TR_BL);
            gradient2.setColors(new int[]{Color.WHITE, vibrantSwatch.getRgb()});
            gradient2.setOrientation(GradientDrawable.Orientation.BL_TR);
        }
        else if (dominantSwatch != null){
            gradient1.setColors(new int[]{Color.WHITE, dominantSwatch.getRgb()});
            gradient1.setOrientation(GradientDrawable.Orientation.TR_BL);
            gradient2.setColors(new int[]{Color.WHITE, dominantSwatch.getRgb()});
            gradient2.setOrientation(GradientDrawable.Orientation.BL_TR);
        }
        else{
            gradient1.setColors(new int[]{Color.WHITE, Color.YELLOW});
            gradient1.setOrientation(GradientDrawable.Orientation.TR_BL);
            gradient2.setColors(new int[]{Color.WHITE, Color.YELLOW});
            gradient2.setOrientation(GradientDrawable.Orientation.BL_TR);
        }

        // change background gradient of buttons to contrast with light theme
        if (darkVibrantSwatch != null){
            pauseplay_background_gradient.setColor(darkVibrantSwatch.getRgb());
            nextbtn_background_gradient.setColor(darkVibrantSwatch.getRgb());
            prevbtn_background_gradient.setColor(darkVibrantSwatch.getRgb());
        }
        else{
            pauseplay_background_gradient.setColor(Color.BLACK);
            nextbtn_background_gradient.setColor(Color.BLACK);
            prevbtn_background_gradient.setColor(Color.BLACK);
        }
    }

    /**
     * helper method used to swap the two main gradients to dark and darkvibrant swatch, if it exists
     * swaps to dark and dominant if vibrant swatch doesn't exist
     * swaps to dark and grey if neither swatch exists
     */
    @TargetApi(16)
    public void swapDarkVibrantGradient(){
        songName.setTextColor(getResources().getColor(R.color.lightPrimaryWhite));
        if (darkVibrantSwatch != null) {
            gradient1.setColors(new int[]{getResources().getColor(R.color.nightPrimaryDark), darkVibrantSwatch.getRgb()});
            gradient1.setOrientation(GradientDrawable.Orientation.TR_BL);
            gradient2.setColors(new int[]{getResources().getColor(R.color.nightPrimaryDark), darkVibrantSwatch.getRgb()});
            gradient2.setOrientation(GradientDrawable.Orientation.BL_TR);
        }
        else if (dominantSwatch != null){
            gradient1.setColors(new int[]{getResources().getColor(R.color.nightPrimaryDark), dominantSwatch.getRgb()});
            gradient1.setOrientation(GradientDrawable.Orientation.TR_BL);
            gradient2.setColors(new int[]{getResources().getColor(R.color.nightPrimaryDark), dominantSwatch.getRgb()});
            gradient2.setOrientation(GradientDrawable.Orientation.BL_TR);
        }
        else {
            gradient1.setColors(new int[]{getResources().getColor(R.color.nightPrimaryDark), getResources().getColor(R.color.nightPrimaryGrey)});
            gradient1.setOrientation(GradientDrawable.Orientation.TR_BL);
            gradient2.setColors(new int[]{getResources().getColor(R.color.nightPrimaryDark), getResources().getColor(R.color.nightPrimaryGrey)});
            gradient2.setOrientation(GradientDrawable.Orientation.BL_TR);
        }

        // change background gradient of buttons to contrast with dark theme
        if (vibrantSwatch != null){
            pauseplay_background_gradient.setColor(vibrantSwatch.getRgb());
            nextbtn_background_gradient.setColor(vibrantSwatch.getRgb());
            prevbtn_background_gradient.setColor(vibrantSwatch.getRgb());
        }
        else{
            pauseplay_background_gradient.setColor(Color.WHITE);
            nextbtn_background_gradient.setColor(Color.WHITE);
            prevbtn_background_gradient.setColor(Color.WHITE);
        }
    }

    /**
     * helper function to convert time in milliseconds to HH:MM:SS format
     */
    public static String convertTime(int timeInMS){
        int timeInSeconds = timeInMS / 1000;

        int seconds = timeInSeconds % 3600 % 60;
        int minutes = timeInSeconds % 3600 / 60;
        int hours = timeInSeconds / 3600;

        String HH, MM, SS;
        if (hours == 0){
            MM = ((minutes  < 10) ? "" : "") + minutes;
            SS = ((seconds  < 10) ? "0" : "") + seconds;
            return MM + ":" + SS;
        }
        else {
            HH = ((hours    < 10) ? "0" : "") + hours;
            MM = ((minutes  < 10) ? "0" : "") + minutes;
            SS = ((seconds  < 10) ? "0" : "") + seconds;
        }

        return HH + ":" + MM + ":" + SS;
    }

    /**
     * helper method to enable scaling of album art and visibility of song name & artist
     * based on largeAlbumArt boolean conditions:
     * false - album art is not large and song name & artist are visible
     * true - album art is large and hides song name & artist
     */
    @TargetApi(16)
    public void toggleLargeAlbumArt(){
        if (largeAlbumArt) {
            largeAlbumArt = false;
            albumArt.animate().scaleX(0.6f).scaleY(0.6f);
            albumArt_btn.setScaleX((float)0.65);
            albumArt_btn.setScaleY((float)0.65);
            songName.setVisibility(View.VISIBLE);
            artistName.setVisibility(View.VISIBLE);
        }
        else{
            largeAlbumArt = true;
            albumArt.animate().scaleX(1f).scaleY(1f);
            albumArt_btn.setScaleX(1);
            albumArt_btn.setScaleY(1);
            songName.setVisibility(View.INVISIBLE);
            artistName.setVisibility(View.INVISIBLE);
        }
    }

    public static Song getCurrent_song(){
        return current_song;
    }

    /**
     * Class is used to handle messages (mainly about updates) sent from other activities
     * in order to keep the main activity most updated
     */
    public class MessageHandler extends Handler
    {
        @SuppressLint("RestrictedApi")
        @Override
        @TargetApi(24)
        public void handleMessage(Message msg) {

            Bundle bundle = msg.getData();
            int updateOperation = (int) bundle.get("update");
            switch (updateOperation) {
                case MusicPlayerService.UPDATE_PLAY:
                    pauseplay.setImageResource(R.drawable.ic_play);
                    slidingUp_pauseplay_btn.setImageResource(R.drawable.ic_play24dp);
                    notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_play24dp, "play", PendingIntent.getService(getApplicationContext(), 1, notificationPauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                    notificationChannel1 = notificationBuilder.build();
                    notificationManager.notify(1, notificationChannel1);
                    break;
                case MusicPlayerService.UPDATE_PAUSE:
                    pauseplay.setImageResource(R.drawable.ic_pause);
                    slidingUp_pauseplay_btn.setImageResource(R.drawable.ic_pause24dp);
                    notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_pause24dp, "pause", PendingIntent.getService(getApplicationContext(), 1, notificationPauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                    notificationChannel1 = notificationBuilder.build();
                    notificationManager.notify(1, notificationChannel1);
                    break;
                case MusicPlayerService.UPDATE_SEEKBAR_DURATION:
                    // init the seekbar & textview max duration and begin thread to track progress
                    int musicMaxDuration = (int) bundle.get("time");
                    seekBar.setMax(musicMaxDuration);
                    String time = convertTime(musicMaxDuration);
                    musicDuration.setText(time);

                    // spawn a thread to update seekbar progress each millisecond
                    final Messenger seekMessenger = new Messenger(new MessageHandler());
                    seekBarProgressIntent.putExtra("seekbarProgress", seekMessenger);
                    Thread seekbarUpdateThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                startService(seekBarProgressIntent);
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                    seekbarUpdateThread.start();
                    break;
                case MusicPlayerService.UPDATE_SEEKBAR_PROGRESS:
                    int musicCurrentPosition = (int) bundle.get("time");
                    seekBar.setProgress(musicCurrentPosition);
                    break;
                case MusicPlayerService.UPDATE_SONG:
                    // update main activitiy with the selected song from music list
                    current_song = (Song) bundle.get("song");

                    // grab song album art and duration
                    String albumID = current_song.getAlbumID();
                    long albumID_long = Long.parseLong(albumID);
                    Bitmap albumImage;
                    Uri albumArtURI = ContentUris.withAppendedId(MusicPlayerService.artURI, albumID_long);
                    ContentResolver res = getContentResolver();
                    try {
                        InputStream in = res.openInputStream(albumArtURI);
                        albumImage = BitmapFactory.decodeStream(in);
                        if (in != null){
                            in.close();
                        }
                    }catch(Exception e){
                        albumImage = BitmapFactory.decodeResource(getResources(), R.drawable.default_image);
                        e.printStackTrace();
                    }
                    int songDuration = current_song.getDuration();

                    // update notification details
                    notificationBuilder
                            .setContentTitle(current_song.getTitle())
                            .setPriority(NotificationManager.IMPORTANCE_LOW)
                            .setContentText(current_song.getArtist())
                            .setLargeIcon(albumImage);
                    notificationChannel1 = notificationBuilder.build();
                    notificationManager.notify(1, notificationChannel1);

                    // update sliding menu details
                    slidingUp_songName.setText(current_song.getTitle());
                    slidingUp_artistName.setText(current_song.getArtist());
                    slidingUp_albumArt.setImageBitmap(albumImage);

                    // update main activity details
                    songName.setText(current_song.getTitle());
                    artistName.setText(current_song.getArtist());
                    albumArt.setImageBitmap(albumImage);
                    seekBar.setMax(songDuration);
                    musicDuration.setText(convertTime(songDuration));

                    // update palette swatch colors for the animated gradients
                    Palette.from(albumImage).maximumColorCount(8).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(@Nullable Palette palette) {
                            vibrantSwatch = palette.getVibrantSwatch();
                            darkVibrantSwatch = palette.getDarkVibrantSwatch();
                            dominantSwatch = palette.getDominantSwatch();
                            if (MusicListActivity.nightMode){
                                swapDarkVibrantGradient();
                            }
                            else{
                                swapVibrantGradient();
                            }
                        }
                    });
                    break;
                case MusicPlayerService.UPDATE_NIGHT:
                    swapDarkVibrantGradient();
                    info_btn.setImageResource(R.drawable.info_light);
                    break;
                case MusicPlayerService.UPDATE_LIGHT:
                    swapVibrantGradient();
                    info_btn.setImageResource(R.drawable.info_night);
                    break;
            }
        }
    }
}

