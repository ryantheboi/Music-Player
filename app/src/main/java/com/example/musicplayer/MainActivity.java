package com.example.musicplayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaSessionCompat;
import android.view.ActionMode;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.example.musicplayer.Notifications.CHANNEL_ID_1;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_REQUEST = 1;
    private boolean isPermissionGranted = false;
    private boolean isInfoDisplaying;
    private ArrayList<Song> fullSongList;
    private HashMap<Integer, SongMetadata> fullSongMetadataHashMap;
    private static ArrayList<Playlist> playlistList;
    private static Playlist current_playlist;
    private static Playlist fullPlaylist;
    private Messenger mainActivityMessenger;
    private Intent musicServiceIntent;
    public static boolean isActionMode = false;
    public static ActionMode actionMode = null;
    private boolean isDestroyed = false;
    private Metadata metadata;
    private boolean isMetadataLoaded = false;

    // sliding up panel
    private static int random_seed;
    private boolean isAlbumArtCircular;
    private static boolean isShuffled;
    private static int repeat_status;
    private MessageHandler messageHandler;
    private MessageHandler seekbarHandler;
    private ImageView mainDisplay_albumArt;
    private Button mainDisplay_albumArt_btn;
    private CardView mainDisplay_albumArt_cardView;
    private ObjectAnimator mainDisplay_albumArt_cardView_animator_round;
    private ObjectAnimator mainDisplay_albumArt_cardView_animator_square;
    private GradientDrawable mainDisplay_mainGradient;
    private SeekBar mainDisplay_seekBar;
    private ImageButton mainDisplay_slidedown_btn;
    private ImageButton mainDisplay_info_btn;
    private ImageButton mainDisplay_pauseplay_btn;
    private ImageButton mainDisplay_next_btn;
    private ImageButton mainDisplay_prev_btn;
    private ImageButton mainDisplay_shuffle_btn;
    private ImageButton mainDisplay_repeat_btn;
    private RippleDrawable mainDisplay_slidedown_btn_ripple;
    private RippleDrawable mainDisplay_info_btn_ripple;
    private RippleDrawable mainDisplay_prev_btn_ripple;
    private RippleDrawable mainDisplay_pauseplay_btn_ripple;
    private RippleDrawable mainDisplay_next_btn_ripple;
    private RippleDrawable mainDisplay_shuffle_btn_ripple;
    private RippleDrawable mainDisplay_repeat_btn_ripple;
    private TextView mainDisplay_playlistHeader;
    private TextView mainDisplay_musicPosition;
    private TextView mainDisplay_musicDuration;
    private TextView mainDisplay_songTitle;
    private TextView mainDisplay_songArtist;
    private MediaSessionCompat mediaSession;
    private NotificationManagerCompat notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private static Notification notificationChannel1;
    private Intent mainPausePlayIntent;
    private Intent mainPrevIntent;
    private Intent mainNextIntent;
    private Intent infoIntent;
    private Intent notificationPauseplayIntent;
    private Intent notificationPrevIntent;
    private Intent notificationNextIntent;
    private Intent seekBar_progressIntent;
    private Intent seekBar_seekIntent;
    private boolean seekBar_isTracking;
    private static Song current_song = Song.EMPTY_SONG;
    private Bitmap current_albumImage;
    private RelativeLayout mainActivityRelativeLayout;
    private RelativeLayout slidingUpMenuLayout;
    private SlidingUpPanelLayout slidingUpPanelLayout;
    private ImageView slidingUp_albumArt;
    private TextView slidingUp_songName;
    private TextView slidingUp_artistName;
    private Intent slidingUp_pauseplayIntent;
    private Intent slidingUp_prevIntent;
    private Intent slidingUp_nextIntent;
    private Intent notificationIntent;
    private ImageButton slidingUp_prev_btn;
    private ImageButton slidingUp_pauseplay_btn;
    private ImageButton slidingUp_next_btn;
    private RippleDrawable slidingUp_prev_btn_ripple;
    private RippleDrawable slidingUp_pauseplay_btn_ripple;
    private RippleDrawable slidingUp_next_btn_ripple;

    // database
    private DatabaseRepository databaseRepository;

    //fragments
    private MainFragment mainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setTheme(R.style.ThemeOverlay_AppCompat_MusicLight);
            ThemeColors.generateThemeValues(this, R.style.ThemeOverlay_AppCompat_MusicLight);

            System.out.println("created");

            // initialize database repository to handle all retrievals and transactions
            databaseRepository = new DatabaseRepository(this, this);

            // init thread for message handling
            HandlerThread messageHandlerThread = new HandlerThread("MessageHandler");
            messageHandlerThread.start();
            messageHandler = new MessageHandler(messageHandlerThread.getLooper());
            mainActivityMessenger = new Messenger(messageHandler);

            // when a configuration change occurs and activity is recreated, fragment is auto restored
            if (savedInstanceState == null) {
                mainFragment = MainFragment.getInstance(mainActivityMessenger);
                getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .add(R.id.fragment_playlist, mainFragment)
                        .commit();
            }

            // initialize all views
            setContentView(R.layout.activity_main);
            slidingUpMenuLayout = findViewById(R.id.sliding_menu);
            slidingUp_albumArt = findViewById(R.id.sliding_albumart);
            slidingUp_songName = findViewById(R.id.sliding_title);
            slidingUp_artistName = findViewById(R.id.sliding_artist);
            slidingUpPanelLayout = findViewById(R.id.slidingPanel);
            slidingUp_pauseplay_btn = findViewById(R.id.sliding_btn_play);
            slidingUp_next_btn = findViewById(R.id.sliding_btn_next);
            slidingUp_prev_btn = findViewById(R.id.sliding_btn_prev);
            mainActivityRelativeLayout = findViewById(R.id.mainlayout);
            mainDisplay_albumArt = findViewById(R.id.song_albumart);
            mainDisplay_albumArt_cardView = findViewById(R.id.song_cardview);
            mainDisplay_albumArt_btn = findViewById(R.id.toggle_largeAlbumArt);
            mainDisplay_songTitle = findViewById(R.id.song_title);
            mainDisplay_songArtist = findViewById(R.id.song_artist);
            mainDisplay_seekBar = findViewById(R.id.seekBar);
            mainDisplay_slidedown_btn = findViewById(R.id.slidedown_btn);
            mainDisplay_info_btn = findViewById(R.id.btn_info);
            mainDisplay_pauseplay_btn = findViewById(R.id.btn_play);
            mainDisplay_next_btn = findViewById(R.id.btn_next);
            mainDisplay_prev_btn = findViewById(R.id.btn_prev);
            mainDisplay_shuffle_btn = findViewById(R.id.btn_shuffle);
            mainDisplay_repeat_btn = findViewById(R.id.btn_repeat);
            mainDisplay_musicPosition = findViewById(R.id.music_position);
            mainDisplay_musicDuration = findViewById(R.id.music_duration);
            mainDisplay_playlistHeader = findViewById(R.id.playlist_header);
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    @Override
    @TargetApi(16)
    protected void onStart() {
        try {
            super.onStart();
            System.out.println("started");

            isPermissionGranted = ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            // check and request for read permissions
            if (isPermissionGranted) {
                setVolumeControlStream(AudioManager.STREAM_MUSIC);

                musicServiceIntent = new Intent(this, MusicPlayerService.class);

                // retrieve metadata values from database (blocks until db is available)
                databaseRepository.asyncGetMetadata();

                // init listview functionality and playlist
                initMusicList();

                initMainGradient();
                initMainButtons();
                initInfoButton();
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // present rationale to user and then request for permissions
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            } else {
                // request for permissions
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            }
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    @Override
    protected void onResume() {
        try {
            System.out.println("resumed");
            if (isPermissionGranted) {
                isInfoDisplaying = false;
            }
            super.onResume();
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    @Override
    protected void onPause() {
        try {
            System.out.println("paused");
            if (isPermissionGranted) {
                if (isMetadataLoaded) {
                    // update current metadata values in database
                    int theme_resourceid = ThemeColors.getThemeResourceId();
                    int songtab_scrollindex = SongListTab.getScrollIndex();
                    int songtab_scrolloffset = SongListTab.getScrollOffset();
                    databaseRepository.updateMetadataTheme(theme_resourceid);
                    databaseRepository.updateMetadataSongtab(songtab_scrollindex, songtab_scrolloffset);

                    // save the current random seed
                    databaseRepository.updateMetadataRandomSeed(random_seed);

                    // save the current shuffle and repeat option
                    databaseRepository.updateMetadataIsShuffled(isShuffled);
                    databaseRepository.updateMetadataRepeatStatus(repeat_status);

                    // save current song index and playlist to database
                    databaseRepository.updateMetadataSongIndex(current_playlist.getSongList().indexOf(current_song));
                    databaseRepository.insertPlaylist(current_playlist);
                }
            }
            super.onPause();
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    @Override
    protected void onStop() {
        try {
            System.out.println("stopped");
            if (isPermissionGranted) {
                databaseRepository.finish();
            }
            super.onStop();
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    @Override
    protected void onDestroy() {
        try {
            System.out.println("destroyed");
            isDestroyed = true;
            super.onDestroy();
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    /**
     * Initializes all songs from the device and all playlists from the database
     */
    @TargetApi(24)
    public void initMusicList() {
        // gets all songs from device and sorts them
        fullSongList = new ArrayList<>();
        getMusic(); // populates fullSongList
        fullSongList.sort(new Comparator<Song>() {
            @Override
            public int compare(Song o1, Song o2) {
                String o1_title = o1.getTitle().toUpperCase();
                String o2_title = o2.getTitle().toUpperCase();
                return o1_title.compareTo(o2_title);
            }
        });

        fullPlaylist = new Playlist("FULL_PLAYLIST", fullSongList);

        // asynchronously gets all songs and playlists from database, then updates main activity
        databaseRepository.asyncGetAllSongMetadata();
        databaseRepository.asyncInitAllPlaylists();
    }

    public void getMusic() {
        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri, null, null, null, null);

        if (songCursor != null && songCursor.moveToFirst()) {
            SongHelper.setSongCursorIndexes(songCursor);

            do {
                Song song = SongHelper.createSong(songCursor);
                databaseRepository.insertSongMetadataIfNotExist(song);
                fullSongList.add(song);
            } while (songCursor.moveToNext());

            songCursor.close();
        }
    }

    @TargetApi(24)
    public void getMusicPlaylistsAsync() {
        CompletableFuture<ArrayList<Playlist>> cf;

        // perform asynchronously to return all playlists and their songs from mediastore
        cf = CompletableFuture.supplyAsync(new Supplier<ArrayList<Playlist>>() {
            @Override
            public ArrayList<Playlist> get() {
                // query for cursor to iterate over all playlists in mediastore
                ArrayList<Playlist> mediastorePlaylists = new ArrayList<>();
                ContentResolver contentResolver = getContentResolver();
                Cursor playlistCursor = contentResolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null, null, null, null);
                if (playlistCursor != null && playlistCursor.moveToFirst()) {

                    int playlistId = playlistCursor.getColumnIndex(MediaStore.Audio.Playlists._ID);
                    int playlistName = playlistCursor.getColumnIndex(MediaStore.Audio.Playlists.NAME);

                    do {
                        // query for cursor to iterate over each member in a playlist (using its id)
                        long current_playlistId = playlistCursor.getLong(playlistId);
                        String current_playlistName = playlistCursor.getString(playlistName);

                        Uri playListUri = MediaStore.Audio.Playlists.Members.getContentUri("external", current_playlistId);
                        Cursor playlistMembersCursor = contentResolver.query(playListUri, null, null, null, null);
                        if (playlistMembersCursor != null) {
                            if (playlistMembersCursor.moveToFirst()) {
                                ArrayList<Song> current_playlistSongs = new ArrayList<>();
                                Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                                String selection = MediaStore.Audio.Media._ID + "=?";
                                do {
                                    // query for cursor to identify playlist members that are songs
                                    String track_id = playlistMembersCursor.getString(playlistMembersCursor.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));
                                    String[] selectionArgs = new String[]{track_id};
                                    Cursor songCursor = contentResolver.query(songUri, null, selection, selectionArgs, null);
                                    SongHelper.setSongCursorIndexes(songCursor);

                                    if (songCursor.getCount() >= 0 && songCursor.moveToFirst()) {
                                        Song song = SongHelper.createSong(songCursor);
                                        current_playlistSongs.add(song);
                                    }
                                    songCursor.close();
                                } while (playlistMembersCursor.moveToNext());

                                // add the playlist and its songs to the async return
                                Playlist current_playlist = new Playlist(DatabaseRepository.generatePlaylistId(), current_playlistName, current_playlistSongs, 0);
                                mediastorePlaylists.add(current_playlist);

                                // add the playlist and its songs to the database
                                databaseRepository.insertPlaylist(current_playlist);
                            }
                            playlistMembersCursor.close();
                        }
                    } while (playlistCursor.moveToNext());
                    playlistCursor.close();
                }
                return mediastorePlaylists;
            }
        });

        // perform after the asynchronous operation is complete
        cf.thenAccept(new Consumer<ArrayList<Playlist>>() {
            @Override
            public void accept(final ArrayList<Playlist> arr) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (Playlist mediastorePlaylist : arr){
                            // update the viewpager adapter with the mediastore playlists
                            mainFragment.addPlaylist(mediastorePlaylist);
                        }
                    }
                });
                // update db to notify that mediastore playlists (if any) have been imported
                databaseRepository.updateMetadataIsMediaStorePlaylistsImported(true);
            }
        });
    }

    /**
     * Method used to initialize all UI components that are related to the device's music
     */
    public void initMusicUI(){
        // init main ui
        initNotification();
        initMainDisplay();
        initSeekbar();
        initSlidingUpPanel();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST:
                // if request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                    onStart();
                } else {
                    Toast.makeText(this, "Permission denied..", Toast.LENGTH_SHORT).show();
                    // gracefully degrade app experience and explain what features are unavailable
                }
                break;
        }
    }

    /**
     * Initialize views and buttons in the sliding up panel
     * The sliding up panel can be expanded upon click, but not collapsed
     * Buttons will be invisible and unclickable depending on the panel state
     */
    public void initSlidingUpPanel() {
        // init functionality for buttons on sliding menu
        initSlidingUpPanelButtons();

        // enable sliding menu text marquee left-right scrolling
        slidingUp_songName.setSelected(true);
        slidingUp_artistName.setSelected(true);

        // init state of sliding panel views
        if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            // set layout alphas
            slidingUpMenuLayout.setAlpha(0);
            mainActivityRelativeLayout.setAlpha(1);

            // disable buttons in sliding menu
            slidingUp_prev_btn.setClickable(false);
            slidingUp_pauseplay_btn.setClickable(false);
            slidingUp_next_btn.setClickable(false);

            // enable buttons in main display
            mainDisplay_slidedown_btn.setClickable(true);
        }
        else if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            // set layout alphas
            slidingUpMenuLayout.setAlpha(1);
            mainActivityRelativeLayout.setAlpha(0);

            // enable buttons in sliding menu
            slidingUp_prev_btn.setClickable(true);
            slidingUp_pauseplay_btn.setClickable(true);
            slidingUp_next_btn.setClickable(true);

            // disable buttons in main display
            mainDisplay_slidedown_btn.setClickable(false);
        }

        // init slide listener
        slidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                slidingUpMenuLayout.setAlpha(1 - slideOffset);
                mainActivityRelativeLayout.setAlpha(slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    // disable buttons in sliding menu
                    slidingUp_prev_btn.setClickable(false);
                    slidingUp_pauseplay_btn.setClickable(false);
                    slidingUp_next_btn.setClickable(false);

                    // enable buttons in main display
                    mainDisplay_slidedown_btn.setClickable(true);
                }
                else if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    // enable buttons in sliding menu
                    slidingUp_prev_btn.setClickable(true);
                    slidingUp_pauseplay_btn.setClickable(true);
                    slidingUp_next_btn.setClickable(true);

                    // disable buttons in main display
                    mainDisplay_slidedown_btn.setClickable(false);
                }
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
    @TargetApi(21)
    public void initSlidingUpPanelButtons() {
        Messenger slidingUpPanelMessenger = new Messenger(messageHandler);

        // init pauseplay button and ripple drawable
        slidingUp_pauseplay_btn_ripple = (RippleDrawable) slidingUp_pauseplay_btn.getBackground();

        slidingUp_pauseplayIntent = new Intent(this, MusicPlayerService.class);
        slidingUp_pauseplayIntent.putExtra("pauseplay", slidingUpPanelMessenger);

        slidingUp_pauseplay_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(slidingUp_pauseplayIntent);
            }
        });

        // init next button and ripple drawable
        slidingUp_next_btn_ripple = (RippleDrawable) slidingUp_next_btn.getBackground();

        slidingUp_nextIntent = new Intent(this, MusicPlayerService.class);
        slidingUp_nextIntent.putExtra("next", slidingUpPanelMessenger);

        slidingUp_next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(slidingUp_nextIntent);
            }
        });

        // init prev button and ripple drawable
        slidingUp_prev_btn_ripple = (RippleDrawable) slidingUp_prev_btn.getBackground();

        slidingUp_prevIntent = new Intent(this, MusicPlayerService.class);
        slidingUp_prevIntent.putExtra("prev", slidingUpPanelMessenger);

        slidingUp_prev_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(slidingUp_prevIntent);
            }
        });
    }

    /**
     * Initialize the gradient, which can be mutated based on the on the theme and album art
     */
    @TargetApi(16)
    public void initMainGradient() {
        // set up gradient background which can be mutated
        mainDisplay_mainGradient = (GradientDrawable) ResourcesCompat.getDrawable(this.getResources(), R.drawable.gradient_default, null);
        mainDisplay_mainGradient.mutate();
        mainActivityRelativeLayout.setBackground(mainDisplay_mainGradient);
    }

    /**
     * Initializes the main display details, including the album art, song name, and artist name
     * On button click (overlaps with albumart), scales the view and button with an animation
     */
    public void initMainDisplay() {
        // init album art corner radius animations
        mainDisplay_albumArt_cardView_animator_round = ObjectAnimator
                .ofFloat(mainDisplay_albumArt_cardView, "radius", (float) mainDisplay_albumArt_cardView.getWidth() / 2)
                .setDuration(350);
        mainDisplay_albumArt_cardView_animator_square = ObjectAnimator
                .ofFloat(mainDisplay_albumArt_cardView, "radius", (float) mainDisplay_albumArt_cardView.getWidth() / 10)
                .setDuration(350);

        // if the intended album art height is greater than the actual height, resize based on screen dimensions
        if (mainDisplay_albumArt_cardView.getLayoutParams().height > mainDisplay_albumArt_cardView.getHeight()) {
            // out fields in [x,y] format
            int[] coords1 = new int[2];
            int[] coords2 = new int[2];
            mainDisplay_playlistHeader.getLocationOnScreen(coords1);
            mainDisplay_songTitle.getLocationOnScreen(coords2);

            // resize album art to fit between the playlist header and song title
            float height_position1 = (float) coords1[1] + (float) (mainDisplay_playlistHeader.getHeight() * 2);
            float height_position2 = coords2[1] - (mainDisplay_songTitle.getHeight() * 2);
            float height_difference = Math.abs(height_position1 - height_position2);
            float shrink_factor = height_difference / mainDisplay_albumArt_cardView.getLayoutParams().height;

            mainDisplay_albumArt_cardView.getLayoutParams().height *= shrink_factor;
            mainDisplay_albumArt_cardView.getLayoutParams().width *= shrink_factor;
        }

        // init main display album art circular if different from xml (less rounded corners)
        if (isAlbumArtCircular) {
            mainDisplay_albumArt_cardView.setScaleX(0.95f);
            mainDisplay_albumArt_cardView.setScaleY(0.95f);
            mainDisplay_albumArt_cardView.setRadius((float) mainDisplay_albumArt_cardView.getWidth() / 2);
        }

        // init album art size toggle button
        mainDisplay_albumArt_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLargeAlbumArt();
                databaseRepository.updateMetadataIsAlbumArtCircular(isAlbumArtCircular);
            }
        });

        // enable playlist header, song title, and song artist text with marquee scrolling
        mainDisplay_playlistHeader.setSelected(true);
        mainDisplay_songTitle.setSelected(true);
        mainDisplay_songArtist.setSelected(true);
    }

    /**
     * Initializes the main buttons in main activity:
     * Pauseplay, Previous, Next, Shuffle
     * Pauseplay, Previous, and Next will trigger a unique event from MusicPlayerService
     */
    @TargetApi(23)
    public void initMainButtons() {
        Messenger mainMessenger = new Messenger(messageHandler);

        // init pauseplay button click functionality and its ripple
        mainDisplay_pauseplay_btn_ripple = (RippleDrawable) mainDisplay_pauseplay_btn.getBackground();
        mainDisplay_pauseplay_btn_ripple.setRadius(70);
        mainPausePlayIntent = new Intent(this, MusicPlayerService.class);
        mainPausePlayIntent.putExtra("pauseplay", mainMessenger);

        mainDisplay_pauseplay_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(mainPausePlayIntent);
            }
        });

        // init next button click functionality and its ripple
        mainDisplay_next_btn_ripple = (RippleDrawable) mainDisplay_next_btn.getBackground();
        mainDisplay_next_btn_ripple.setRadius(70);
        mainNextIntent = new Intent(this, MusicPlayerService.class);
        mainNextIntent.putExtra("next", mainMessenger);

        mainDisplay_next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(mainNextIntent);
            }
        });

        // init prev button click functionality and its ripple
        mainDisplay_prev_btn_ripple = (RippleDrawable) mainDisplay_prev_btn.getBackground();
        mainDisplay_prev_btn_ripple.setRadius(70);
        mainPrevIntent = new Intent(this, MusicPlayerService.class);
        mainPrevIntent.putExtra("prev", mainMessenger);

        mainDisplay_prev_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(mainPrevIntent);
            }
        });

        // init slidedown button functionality and its ripple
        mainDisplay_slidedown_btn_ripple = (RippleDrawable) mainDisplay_slidedown_btn.getBackground();
        mainDisplay_slidedown_btn_ripple.setRadius(50);
        mainDisplay_slidedown_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });

        // init shuffle button functionality and its ripple
        mainDisplay_shuffle_btn_ripple = (RippleDrawable) mainDisplay_shuffle_btn.getBackground();
        mainDisplay_shuffle_btn_ripple.setRadius(70);
        mainDisplay_shuffle_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // toggle shuffle button
                if (isShuffled){
                    isShuffled = false;
                    mainDisplay_shuffle_btn.setImageAlpha(40);
                    current_playlist = current_playlist.unshufflePlaylist(random_seed);
                }
                else {
                    isShuffled = true;
                    random_seed = Math.abs(new Random().nextInt());
                    mainDisplay_shuffle_btn.setImageAlpha(255);
                    current_playlist = current_playlist.shufflePlaylist(random_seed);
                }
            }
        });

        // init repeat button functionality and its ripple
        mainDisplay_repeat_btn_ripple = (RippleDrawable) mainDisplay_repeat_btn.getBackground();
        mainDisplay_repeat_btn_ripple.setRadius(70);
        mainDisplay_repeat_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int next_repeat_status = repeat_status + 1 > 2 ? 0 : repeat_status + 1;
                toggleRepeatButton(next_repeat_status);
                repeat_status = next_repeat_status;
            }
        });
    }

    /**
     * Initializes the seekbar and the textviews for current position and max duration
     * Time is converted from milliseconds to HH:MM:SS format
     * Textview for current position is updated upon user changing the seekbar
     */
    public void initSeekbar() {
        // init thread for seekbar updates
        HandlerThread seekbarHandlerThread = new HandlerThread("SeekbarHandler");
        seekbarHandlerThread.start();
        seekbarHandler = new MessageHandler(seekbarHandlerThread.getLooper());

        // set the seekbar & textview duration and sync with mediaplayer
        Intent seekBarDurationIntent = new Intent(this, MusicPlayerService.class);
        seekBar_progressIntent = new Intent(this, MusicPlayerService.class);
        seekBar_seekIntent = new Intent(this, MusicPlayerService.class);
        Messenger mainMessenger = new Messenger(messageHandler);
        seekBarDurationIntent.putExtra("seekbarDuration", mainMessenger);
        startService(seekBarDurationIntent);

        mainDisplay_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String time = SongHelper.convertTime(seekBar.getProgress());
                mainDisplay_musicPosition.setText(time);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBar_isTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar_seekIntent.putExtra("seekbarSeek", seekBar.getProgress());
                startService(seekBar_seekIntent);

                // update the seekPosition value in the local metadata
                databaseRepository.updateMetadataSeek(seekBar.getProgress());

                seekBar_isTracking = false;
            }
        });
    }

    /**
     * Initializes the button for displaying details about a song in a scrollview
     * Upon clicking the button, the current song will be sent in the intent
     */
    public void initInfoButton() {
        mainDisplay_info_btn_ripple = (RippleDrawable) mainDisplay_info_btn.getBackground();
        infoIntent = new Intent(this, MusicDetailsActivity.class);
        mainDisplay_info_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isInfoDisplaying) {
                    isInfoDisplaying = true;
                    infoIntent.putExtra("currentSong", current_song);
                    infoIntent.putExtra("currentSongMetadata", fullSongMetadataHashMap.get(current_song.getId()));
                    startActivity(infoIntent);
                }
            }
        });
    }

    @TargetApi(19)
    public void initNotification() {
        mediaSession = new MediaSessionCompat(this, "media");
        notificationManager = NotificationManagerCompat.from(this);

        Bitmap largeImage = BitmapFactory.decodeResource(getResources(), R.drawable.default_albumart);
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

        // create intents for the notification action buttons
        Messenger notificationMessenger = new Messenger(messageHandler);
        notificationPrevIntent = new Intent(this, MusicPlayerService.class).putExtra("prev", notificationMessenger);
        notificationPauseplayIntent = new Intent(this, MusicPlayerService.class).putExtra("pauseplay", notificationMessenger);
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
                .setOngoing(false)
                .setContentIntent(contentIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mediaSession.getSessionToken()));

        notificationChannel1 = notificationBuilder.build();
        notificationManager.notify(1, notificationChannel1);

        // notify music player service about the notification
        notificationIntent = new Intent(this, MusicPlayerService.class);
        notificationIntent.putExtra("notification", mainActivityMessenger);
        startService(notificationIntent);
    }

    /**
     * Set the theme to the resource id provided and apply its colors to this activity
     * @param theme_resid the resource id of the theme to apply
     */
    public void updateTheme(int theme_resid) {
        setTheme(theme_resid);
        mainFragment.updateFragmentColors();
        updateSeekBarColors();
        updateMainColors();
        updateSlidingMenuColors();
    }

    /**
     * updates the two main gradients and the text colors in the main display
     * gradient colors are derived from the current theme and song
     */
    @TargetApi(21)
    public void updateMainColors() {
        int textSongColor = ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR);
        int textArtistColor = ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR);
        int textSeekbarColor = ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR);
        int primaryColor = ThemeColors.getColor(ThemeColors.COLOR_PRIMARY);
        int secondaryColor= ThemeColors.getGradientColor();

        mainDisplay_songTitle.setTextColor(textSongColor);
        mainDisplay_songArtist.setTextColor(textArtistColor);
        mainDisplay_musicPosition.setTextColor(textSeekbarColor);
        mainDisplay_musicDuration.setTextColor(textSeekbarColor);
        mainDisplay_playlistHeader.setTextColor(textSongColor);
        mainDisplay_mainGradient.setColors(new int[]{primaryColor, secondaryColor});
        mainDisplay_mainGradient.setOrientation(GradientDrawable.Orientation.BL_TR);

        // update drawable vector colors
        Drawable unwrappedDrawableSlideDown = mainDisplay_slidedown_btn.getDrawable();
        Drawable unwrappedDrawableInfo = mainDisplay_info_btn.getDrawable();
        Drawable unwrappedDrawablePauseplay = mainDisplay_pauseplay_btn.getDrawable();
        Drawable unwrappedDrawableNext = mainDisplay_next_btn.getDrawable();
        Drawable unwrappedDrawablePrev = mainDisplay_prev_btn.getDrawable();
        Drawable unwrappedDrawableShuffle = mainDisplay_shuffle_btn.getDrawable();
        Drawable unwrappedDrawableRepeat = mainDisplay_repeat_btn.getDrawable();
        Drawable wrappedDrawableSlideDown = DrawableCompat.wrap(unwrappedDrawableSlideDown);
        Drawable wrappedDrawableInfo = DrawableCompat.wrap(unwrappedDrawableInfo);
        Drawable wrappedDrawablePauseplay = DrawableCompat.wrap(unwrappedDrawablePauseplay);
        Drawable wrappedDrawableNext = DrawableCompat.wrap(unwrappedDrawableNext);
        Drawable wrappedDrawablePrev = DrawableCompat.wrap(unwrappedDrawablePrev);
        Drawable wrappedDrawableShuffle = DrawableCompat.wrap(unwrappedDrawableShuffle);
        Drawable wrappedDrawableRepeat = DrawableCompat.wrap(unwrappedDrawableRepeat);
        DrawableCompat.setTint(wrappedDrawableSlideDown, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
        DrawableCompat.setTint(wrappedDrawableInfo, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
        DrawableCompat.setTint(wrappedDrawablePauseplay, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
        DrawableCompat.setTint(wrappedDrawableNext, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
        DrawableCompat.setTint(wrappedDrawablePrev, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
        DrawableCompat.setTint(wrappedDrawableShuffle, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
        DrawableCompat.setTint(wrappedDrawableRepeat, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));

        // update ripple colors
        mainDisplay_slidedown_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
        mainDisplay_info_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
        mainDisplay_pauseplay_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
        mainDisplay_next_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
        mainDisplay_prev_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
        mainDisplay_shuffle_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
        mainDisplay_repeat_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
    }

    /**
     * updates the background, text, and button colors of the sliding menu
     * text and button colors are decided as the swatch that contrasts the most with the background
     */
    @TargetApi(21)
    public void updateSlidingMenuColors() {
        int base = ThemeColors.getColor(ThemeColors.COLOR_SECONDARY);
        int contrastColor = ThemeColors.getContrastColor(base);

        // change color of sliding menu, its buttons, and text
        slidingUp_songName.setTextColor(contrastColor);
        slidingUp_artistName.setTextColor(contrastColor);
        slidingUpMenuLayout.setBackgroundColor(base);
        Drawable unwrappedDrawablePauseplay = slidingUp_pauseplay_btn.getDrawable();
        Drawable unwrappedDrawableNext = slidingUp_next_btn.getDrawable();
        Drawable unwrappedDrawablePrev = slidingUp_prev_btn.getDrawable();
        Drawable wrappedDrawablePauseplay = DrawableCompat.wrap(unwrappedDrawablePauseplay);
        Drawable wrappedDrawableNext = DrawableCompat.wrap(unwrappedDrawableNext);
        Drawable wrappedDrawablePrev = DrawableCompat.wrap(unwrappedDrawablePrev);
        DrawableCompat.setTint(wrappedDrawablePauseplay, contrastColor);
        DrawableCompat.setTint(wrappedDrawableNext, contrastColor);
        DrawableCompat.setTint(wrappedDrawablePrev, contrastColor);

        // change color of the button ripples
        slidingUp_pauseplay_btn_ripple.setColor(ColorStateList.valueOf(contrastColor));
        slidingUp_next_btn_ripple.setColor(ColorStateList.valueOf(contrastColor));
        slidingUp_prev_btn_ripple.setColor(ColorStateList.valueOf(contrastColor));
    }

    @TargetApi(23)
    private void updateSeekBarColors(){
        mainDisplay_seekBar.getThumb().setTint(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        mainDisplay_seekBar.getProgressDrawable().setTint(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        ((RippleDrawable) mainDisplay_seekBar.getBackground()).setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getRippleDrawableColorId())));
    }

    /**
     * Cleans playlist database of any songs that can no longer be found in the full list of songs
     */
    private void cleanPlaylistDatabase(){
        HashMap<Song, SongNode> fullSongHashMap = new HashMap<>(fullPlaylist.getSongHashMap());
        for (Playlist playlist : playlistList){
            // accumulate list of removed songs for this playlist (if any)
            ArrayList<Song> playlist_songs = playlist.getSongList();
            ArrayList<Song> playlist_removed_songs = new ArrayList<>();
            for (Song song : playlist_songs){
                if (!fullSongHashMap.containsKey(song)){
                    playlist_removed_songs.add(song);
                }
            }

            // recreate the playlist excluding its removed songs (if any) and replace in database
            if (playlist_removed_songs.size() > 0){
                for (Song removed_song : playlist_removed_songs){
                    // removes song from the playlist reference
                    playlist_songs.remove(removed_song);
                }
                Playlist updated_playlist = new Playlist(playlist.getId(), playlist.getName(), playlist_songs, 0);
                databaseRepository.insertPlaylist(updated_playlist);
            }
        }
    }

    /**
     * Method used to initialize various app components using values stored in the metadata
     * E.g. views, random seed, current song, etc.
     */
    public void setupMetadata(){
        boolean isPlaying = metadata.getIsPlaying();
        int seekPosition = metadata.getSeekPosition();
        int songIndex = metadata.getSongIndex();
        int themeResourceId = metadata.getThemeResourceId();
        int songtab_scrollindex = metadata.getSongtab_scrollindex();
        int songtab_scrolloffset = metadata.getSongtab_scrolloffset();
        isShuffled = metadata.getIsShuffled();
        repeat_status = metadata.getRepeatStatus();
        boolean isMediaStorePlaylistsImported = metadata.getIsMediaStorePlaylistsImported();
        isAlbumArtCircular = metadata.getIsAlbumArtCircular();
        random_seed = metadata.getRandom_seed();

        // set current song using the song index metadata
        if (current_playlist.getSize() > 0) {
            current_song = current_playlist.getSongList().get(songIndex);
        }

        // set shuffle button transparency using the isShuffled metadata
        mainDisplay_shuffle_btn.setImageAlpha(isShuffled ? 255 : 40);

        // set repeat button appearance using repeatStatus metadata
        toggleRepeatButton(repeat_status);

        // retrieve all playlists that exist in mediastore if this hasn't been done before
        if (!isMediaStorePlaylistsImported){
            getMusicPlaylistsAsync();
        }

        // music player is playing, start music service but keep playing
        if (isPlaying) {
            musicServiceIntent.putExtra("musicListInitPlaying", mainActivityMessenger);
            startService(musicServiceIntent);

            initMusicUI();
        }
        // music player is not playing, start music service for the first time
        else {
            musicServiceIntent.putExtra("musicListInitPaused", mainActivityMessenger);
            startService(musicServiceIntent);

            initMusicUI();

            // inform the music service about the seekbar's position from the metadata
            seekBar_seekIntent.putExtra("seekbarSeek", seekPosition);
            startService(seekBar_seekIntent);
            mainDisplay_seekBar.setProgress(seekPosition);
        }

        // set the current theme and generate theme values
        setTheme(themeResourceId);
        ThemeColors.generateThemeValues(this, themeResourceId);

        // after generating theme values, update the main ui
        updateTheme(themeResourceId);
        SongListTab.setScrollSelection(songtab_scrollindex, songtab_scrolloffset);
        isMetadataLoaded = true;
    }

    /**
     * Helper method to enable scaling of album art and visibility of song name & artist
     * Based on largeAlbumArt boolean conditions:
     * false - album art is not large and song name & artist are visible
     * true - album art is large and hides song name & artist
     */
    @TargetApi(16)
    public void toggleLargeAlbumArt(){
        if (isAlbumArtCircular) {
            isAlbumArtCircular = false;
            mainDisplay_albumArt_cardView.animate().scaleX(1f).scaleY(1f);
            mainDisplay_albumArt_cardView_animator_square.start();
        }
        else{
            isAlbumArtCircular = true;
            mainDisplay_albumArt_cardView.animate().scaleX(0.95f).scaleY(0.95f);
            mainDisplay_albumArt_cardView_animator_round.start();
        }
    }

    /**
     * Helper method to set the appearance of the repeat button, based on the repeat status
     * @param repeatStatus 0 for disable, 1 for repeat playlist, 2 for repeat one song
     */
    public void toggleRepeatButton(int repeatStatus){
        switch (repeatStatus){
            // disable repeat
            case 0:
                mainDisplay_repeat_btn.setImageResource(R.drawable.ic_repeat28dp);
                mainDisplay_repeat_btn.setImageAlpha(40);
                break;

            // repeat playlist
            case 1:
                mainDisplay_repeat_btn.setImageResource(R.drawable.ic_repeat28dp);
                mainDisplay_repeat_btn.setImageAlpha(255);
                break;

            // repeat one song
            case 2:
                mainDisplay_repeat_btn.setImageResource(R.drawable.ic_repeat_one28dp);
                mainDisplay_repeat_btn.setImageAlpha(255);
                break;
        }

        // set appropriate colors for the button
        Drawable unwrappedDrawableRepeat = mainDisplay_repeat_btn.getDrawable();
        Drawable wrappedDrawableRepeat = DrawableCompat.wrap(unwrappedDrawableRepeat);
        DrawableCompat.setTint(wrappedDrawableRepeat, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
    }

    /**
     * Updates the main activity with the changes made by the database repository
     * @param object an object associated with the update operation (e.g. a playlist, or playlists)
     * @param messenger the messenger to notify after successfully adding the playlist
     * @param operation the operation being performed with the playlist
     */
    public void updateMainActivity(Object object, final Messenger messenger, final int operation){
        try {
            final MainActivity mainActivity = this;
            switch (operation) {
                case DatabaseRepository.ASYNC_INIT_ALL_PLAYLISTS:
                    // remove any songs that were not able to be found in the device
                    playlistList = (ArrayList<Playlist>) object;
                    cleanPlaylistDatabase();
                    SongListAdapter songListadapter = new SongListAdapter(this, R.layout.adapter_song_layout, fullSongList, this);
                    PlaylistAdapter playlistAdapter = new PlaylistAdapter(this, R.layout.adapter_playlist_layout, playlistList, this);
                    mainFragment.setAdapters(songListadapter, playlistAdapter);

                    // initialize current playlist from database, if possible
                    databaseRepository.asyncGetCurrentPlaylist();
                    break;
                case DatabaseRepository.ASYNC_GET_CURRENT_PLAYLIST:
                    current_playlist = (Playlist) object;

                    // if a playlist wasn't retrieved from the database
                    if (current_playlist == null) {
                        current_playlist = fullPlaylist;
                        if (fullSongList.size() > 0) {
                            current_song = fullSongList.get(0);
                        } else {
                            break;
                        }
                    }

                    // set up app components using the metadata already retrieved
                    setupMetadata();
                    break;
                case DatabaseRepository.ASYNC_INSERT_PLAYLIST:
                    mainFragment.updateMainFragment(object, DatabaseRepository.ASYNC_INSERT_PLAYLIST);
                    break;
                case DatabaseRepository.ASYNC_MODIFY_PLAYLIST:
                    mainFragment.updateMainFragment(object, DatabaseRepository.ASYNC_MODIFY_PLAYLIST);
                    break;
                case DatabaseRepository.ASYNC_DELETE_PLAYLISTS_BY_ID:
                    mainFragment.updateMainFragment(object, DatabaseRepository.ASYNC_DELETE_PLAYLISTS_BY_ID);
                    break;
                case DatabaseRepository.ASYNC_GET_METADATA:
                    // metadata object guaranteed not null
                    metadata = (Metadata) object;
                    break;
                case DatabaseRepository.ASYNC_GET_ALL_SONGMETADATA:
                    ArrayList<SongMetadata> database_songs = (ArrayList<SongMetadata>) object;

                    if (database_songs != null) {
                        // create hashmap of every song id to song metadata
                        fullSongMetadataHashMap = new HashMap<>();
                        for (SongMetadata songMetadata : database_songs) {
                            fullSongMetadataHashMap.put(songMetadata.getId(), songMetadata);
                        }
                    }
                    break;
            }

            if (messenger != null) {
                // send message to end the addplaylistactivity, as the views are now updated
                Message msg = Message.obtain();
                Bundle bundle = new Bundle();
                bundle.putInt("msg", AddPlaylistActivity.FINISH);
                msg.setData(bundle);
                messenger.send(msg);
            }
        }catch (Exception e){
            Logger.logException(e);
        }
    }

    public View getMainActivityLayout(){
        return slidingUpPanelLayout;
    }
    public static int getRepeat_status(){
        return repeat_status;
    }
    public static Song getCurrent_song(){
        return current_song;
    }
    public static Playlist getCurrent_playlist(){
        return current_playlist;
    }
    public static ArrayList<Playlist> getPlaylists(){
        return playlistList;
    }
    public static Playlist getFullPlaylist(){
        return fullPlaylist;
    }
    public static void setCurrent_song(Song song){
        current_song = song;
    }
    public static void setCurrent_playlist(Playlist playlist){
        if (!isShuffled) {
            current_playlist = playlist;
        }
        else{
            current_playlist = playlist.shufflePlaylist(random_seed);
        }
    }
    public static void setCurrent_transientPlaylist(Playlist transient_playlist){
        Playlist current_playlist = new Playlist(transient_playlist.getName(), transient_playlist.getSongList());
        setCurrent_playlist(current_playlist);
    }

    public static Notification getNotification(){
        return notificationChannel1;
    }

    /**
     * Class is used to handle messages (mainly about updates) sent from other activities
     * in order to keep the main activity most updated
     */
    public class MessageHandler extends Handler
    {

        public MessageHandler(Looper looper) {
            super(looper);
        }

        @SuppressLint("RestrictedApi")
        @Override
        @TargetApi(26)
        public void handleMessage(Message msg) {

            Bundle bundle = msg.getData();
            int updateOperation = (int) bundle.get("update");
            switch (updateOperation) {
                case MusicPlayerService.UPDATE_PLAY:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mainDisplay_pauseplay_btn.setImageResource(R.drawable.ic_play28dp);
                            slidingUp_pauseplay_btn.setImageResource(R.drawable.ic_play24dp);

                            // change sliding menu pauseplay button color
                            Drawable unwrappedDrawablePauseplay = slidingUp_pauseplay_btn.getDrawable();
                            Drawable wrappedDrawablePauseplay = DrawableCompat.wrap(unwrappedDrawablePauseplay);
                            DrawableCompat.setTint(wrappedDrawablePauseplay, ThemeColors.getContrastColor());

                            // change main pauseplay button color
                            Drawable unwrappedMainDrawablePauseplay = mainDisplay_pauseplay_btn.getDrawable();
                            Drawable wrappedMainDrawablePauseplay = DrawableCompat.wrap(unwrappedMainDrawablePauseplay);
                            DrawableCompat.setTint(wrappedMainDrawablePauseplay, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
                        }
                    });
                    notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_play24dp, "play", PendingIntent.getService(getApplicationContext(), 1, notificationPauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                    notificationChannel1 = notificationBuilder.build();
                    notificationManager.notify(1, notificationChannel1);

                    // update the isPlaying and seekPosition values in the metadata
                    if ((boolean)bundle.get("updateDatabase")) {
                        databaseRepository.updateMetadataIsPlaying(false);

                        // use the seek position given by the music player service
                        if ((int)bundle.get("updateSeek") >= 0) {
                            databaseRepository.updateMetadataSeek((int)bundle.get("updateSeek"));
                        }
                        // use the seek position from the seekbar view
                        else {
                            databaseRepository.updateMetadataSeek(mainDisplay_seekBar.getProgress());
                        }
                    }
                    break;
                case MusicPlayerService.UPDATE_PAUSE:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mainDisplay_pauseplay_btn.setImageResource(R.drawable.ic_pause28dp);
                            slidingUp_pauseplay_btn.setImageResource(R.drawable.ic_pause24dp);

                            // change sliding menu pauseplay button color
                            Drawable unwrappedDrawablePauseplay = slidingUp_pauseplay_btn.getDrawable();
                            Drawable wrappedDrawablePauseplay = DrawableCompat.wrap(unwrappedDrawablePauseplay);
                            DrawableCompat.setTint(wrappedDrawablePauseplay, ThemeColors.getContrastColor());

                            // change main pauseplay button color
                            Drawable unwrappedMainDrawablePauseplay = mainDisplay_pauseplay_btn.getDrawable();
                            Drawable wrappedMainDrawablePauseplay = DrawableCompat.wrap(unwrappedMainDrawablePauseplay);
                            DrawableCompat.setTint(wrappedMainDrawablePauseplay, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
                        }
                    });
                    notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_pause24dp, "pause", PendingIntent.getService(getApplicationContext(), 1, notificationPauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                    notificationChannel1 = notificationBuilder.build();
                    notificationManager.notify(1, notificationChannel1);

                    // update the isPlaying status in the metadata
                    if ((boolean)bundle.get("updateDatabase")) {
                        databaseRepository.updateMetadataIsPlaying(true);
                    }
                    break;
                case MusicPlayerService.UPDATE_SEEKBAR_DURATION:
                    // init the seekbar & textview max duration and begin thread to track progress
                    final int musicMaxDuration = (int) bundle.get("time");
                    final String time = SongHelper.convertTime(musicMaxDuration);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mainDisplay_seekBar.setMax(musicMaxDuration);
                            mainDisplay_musicDuration.setText(time);
                        }
                    });

                    // spawn a thread to update seekbar progress each 100 milliseconds
                    final Messenger seekMessenger = new Messenger(seekbarHandler);
                    seekBar_progressIntent.putExtra("seekbarProgress", seekMessenger);
                    Thread seekbarUpdateThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!isDestroyed) {
                                if (!seekBar_isTracking) {
                                    startService(seekBar_progressIntent);
                                }
                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                    Logger.logException(e, "MainActivity");
                                }
                            }
                        }
                    });
                    seekbarUpdateThread.start();
                    break;
                case MusicPlayerService.UPDATE_SEEKBAR_PROGRESS:
                    int musicCurrentPosition = (int) bundle.get("time");
                    mainDisplay_seekBar.setProgress(musicCurrentPosition);
                    break;
                case MusicPlayerService.UPDATE_SONG:
                    // update main activity with the selected song from music list
                    current_song = (Song) bundle.get("song");

                    // grab song album art and duration
                    String albumID = current_song.getAlbumID();
                    long albumID_long = Long.parseLong(albumID);
                    Uri albumArtURI = ContentUris.withAppendedId(MusicPlayerService.artURI, albumID_long);
                    ContentResolver res = getContentResolver();
                    try {
                        InputStream in = res.openInputStream(albumArtURI);
                        current_albumImage = BitmapFactory.decodeStream(in);
                        if (in != null){
                            in.close();
                        }
                    }catch(Exception e){
                        current_albumImage = BitmapFactory.decodeResource(getResources(), R.drawable.default_albumart);
                        e.printStackTrace();
                    }

                    final int songDuration = current_song.getDuration();

                    // view changes must be done on the main ui thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // update sliding menu details
                            slidingUp_songName.setText(current_song.getTitle());
                            slidingUp_artistName.setText(current_song.getArtist());
                            slidingUp_albumArt.setImageBitmap(current_albumImage);

                            // update main activity details
                            mainDisplay_playlistHeader.setText(current_playlist.getName());
                            mainDisplay_songTitle.setText(current_song.getTitle());
                            mainDisplay_songArtist.setText(current_song.getArtist());
                            mainDisplay_albumArt.setImageBitmap(current_albumImage);
                            mainDisplay_seekBar.setMax(songDuration);
                            mainDisplay_musicDuration.setText(SongHelper.convertTime(songDuration));
                        }
                    });

                    // update notification details
                    notificationBuilder
                            .setContentTitle(current_song.getTitle())
                            .setPriority(NotificationManager.IMPORTANCE_LOW)
                            .setContentText(current_song.getArtist())
                            .setLargeIcon(current_albumImage);
                    notificationChannel1 = notificationBuilder.build();
                    notificationManager.notify(1, notificationChannel1);

                    // update palette swatch colors for the animated gradients
                    ThemeColors.generatePaletteColors(current_albumImage);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateMainColors();
                            updateSlidingMenuColors();
                        }
                    });

                    // update the index of the current song in database
                    // song can be updated within or outside of the app
                    databaseRepository.updateMetadataSongIndex(current_playlist.getSongList().indexOf(current_song));
                    break;
                case ChooseThemeFragment.THEME_SELECTED:
                    final int theme_resid = ThemeColors.getThemeResourceId();

                    // change theme colors and button image to match the current theme
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateTheme(theme_resid);
                            mainFragment.updateFragmentColors();
                        }
                    });
                    break;
                case ChooseThemeFragment.THEME_DONE:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mainFragment.rotateThemeButton();
                        }
                    });
                    break;
                case AddPlaylistActivity.ADD_PLAYLIST:
                    databaseRepository.asyncInsertPlaylist((Playlist) bundle.get("playlist"), (Messenger) bundle.get("messenger"));
                    break;
                case AddPlaylistActivity.MODIFY_PLAYLIST:
                    databaseRepository.asyncModifyPlaylist((Playlist) bundle.get("playlist"), (Messenger) bundle.get("messenger"));
                    break;
                case PlaylistTab.REMOVE_PLAYLISTS:
                    databaseRepository.asyncRemovePlaylistByIds((int[]) bundle.get("ids"));
                    break;
                case MusicPlayerService.UPDATE_SONG_PLAYED:
                    // increment played counter for the song metadata in memory and in database
                    SongMetadata played_songMetadata = fullSongMetadataHashMap.get(((Song) bundle.get("song")).getId());
                    played_songMetadata.setPlayed(played_songMetadata.getPlayed() + 1);
                    databaseRepository.updateSongMetadataPlayed(played_songMetadata);
                    break;
                case MusicPlayerService.UPDATE_SONG_LISTENED:
                    // update listened data for the song metadata in memory and in database
                    String data_listened = Long.toString(System.currentTimeMillis() / 1000);
                    SongMetadata listened_songMetadata = fullSongMetadataHashMap.get(((Song) bundle.get("song")).getId());
                    listened_songMetadata.setListened(listened_songMetadata.getListened() + 1);
                    listened_songMetadata.setDateListened(data_listened);
                    databaseRepository.updateSongMetadataListened(listened_songMetadata, data_listened);
                    break;
            }
        }
    }
}

