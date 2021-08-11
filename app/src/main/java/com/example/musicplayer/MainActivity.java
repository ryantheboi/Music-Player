package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.tabs.TabLayout;
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
    private boolean isThemeSelecting;
    private boolean isInfoDisplaying;
    private ImageView theme_btn;
    private RippleDrawable theme_btn_ripple;
    private CoordinatorLayout musicListRelativeLayout;
    private ArrayList<Song> fullSongList;
    private static ArrayList<Playlist> playlistList;
    private static Playlist current_playlist;
    private static Playlist fullPlaylist;
    private SongListAdapter songListadapter;
    private PlaylistAdapter playlistAdapter;
    private PagerAdapter pagerAdapter;
    private Messenger mainActivityMessenger;
    private Intent musicServiceIntent;
    public static boolean isActionMode = false;
    public static ActionMode actionMode = null;
    private EditText searchFilter_editText;
    private ImageView searchFilter_btn;
    private RippleDrawable searchFilter_btn_ripple;
    private TabLayout tabLayout;
    private DynamicViewPager viewPager;
    private Toolbar toolbar;
    private TextView toolbar_title;
    private ActionBar actionBar;
    private boolean isDestroyed = false;

    // sliding up panel
    private static int random_seed;
    private MessageHandler messageHandler;
    private MessageHandler seekbarHandler;
    private boolean isLargeAlbumArt;
    private ImageView albumArt;
    private CardView albumArt_cardView;
    private ObjectAnimator albumArt_cardView_animator_round;
    private ObjectAnimator albumArt_cardView_animator_square;
    private Button albumArt_btn;
    private ImageButton info_btn;
    private RippleDrawable info_btn_ripple;
    private ImageButton pauseplay_btn;
    private ImageButton next_btn;
    private ImageButton prev_btn;
    private RippleDrawable prev_btn_ripple;
    private RippleDrawable pauseplay_btn_ripple;
    private RippleDrawable next_btn_ripple;
    private ImageButton shuffle_btn;
    private RippleDrawable shuffle_btn_ripple;
    private static boolean isShuffled;
    private ImageButton repeat_btn;
    private RippleDrawable repeat_btn_ripple;
    private static int repeat_status;
    private GradientDrawable mainGradient;
    private SeekBar seekBar;
    private TextView playlistHeader;
    private TextView musicPosition;
    private TextView musicDuration;
    private TextView songName;
    private TextView artistName;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.ThemeOverlay_AppCompat_MusicLight);
        ThemeColors.generateThemeValues(this, R.style.ThemeOverlay_AppCompat_MusicLight);

        System.out.println("created");

        // initialize database repository to handle all retrievals and transactions
        databaseRepository = new DatabaseRepository(this, this);

        // initialize all views
        setContentView(R.layout.activity_main);
        musicListRelativeLayout = findViewById(R.id.activity_musiclist);
        searchFilter_editText = findViewById(R.id.toolbar_searchFilter);
        slidingUpMenuLayout = findViewById(R.id.sliding_menu);
        slidingUp_albumArt = findViewById(R.id.sliding_albumart);
        slidingUp_songName = findViewById(R.id.sliding_title);
        slidingUp_artistName = findViewById(R.id.sliding_artist);
        slidingUpPanelLayout = findViewById(R.id.slidingPanel);
        slidingUp_pauseplay_btn = findViewById(R.id.sliding_btn_play);
        slidingUp_next_btn = findViewById(R.id.sliding_btn_next);
        slidingUp_prev_btn = findViewById(R.id.sliding_btn_prev);
        mainActivityRelativeLayout = findViewById(R.id.mainlayout);
        albumArt = findViewById(R.id.song_albumart);
        albumArt_cardView = findViewById(R.id.song_cardview);
        albumArt_btn = findViewById(R.id.toggle_largeAlbumArt);
        songName = findViewById(R.id.song_title);
        artistName = findViewById(R.id.song_artist);
        pauseplay_btn = findViewById(R.id.btn_play);
        next_btn = findViewById(R.id.btn_next);
        prev_btn = findViewById(R.id.btn_prev);
        shuffle_btn = findViewById(R.id.btn_shuffle);
        repeat_btn = findViewById(R.id.btn_repeat);
        seekBar = findViewById(R.id.seekBar);
        playlistHeader = findViewById(R.id.playlist_header);
        musicPosition = findViewById(R.id.music_position);
        musicDuration = findViewById(R.id.music_duration);
        info_btn = findViewById(R.id.btn_info);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        toolbar = findViewById(R.id.toolbar);
        toolbar_title = findViewById(R.id.toolbar_title);
    }

    @Override
    @TargetApi(16)
    protected void onStart() {
        super.onStart();
        System.out.println("started");

        isPermissionGranted = ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        // check and request for read permissions
        if (isPermissionGranted) {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);

            // init thread for message handling
            HandlerThread messageHandlerThread = new HandlerThread("MessageHandler");
            messageHandlerThread.start();
            messageHandler = new MessageHandler(messageHandlerThread.getLooper());
            mainActivityMessenger = new Messenger(messageHandler);
            musicServiceIntent = new Intent(this, MusicPlayerService.class);

            // init listview functionality and playlist
            initMusicList();

            initNotification();
            initMainGradient();
            initMainButtons();
            initInfoButton();
            initActionBar();
        }
        else if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // present rationale to user and then request for permissions
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
        }
        else {
            // request for permissions
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
        }
    }

    @Override
    protected void onResume() {
        System.out.println("resumed");
        if (isPermissionGranted) {
            isInfoDisplaying = false;
        }
        super.onResume();
    }

    @Override
    @TargetApi(23)
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate the menu for the actionbar, if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        // theme button menu item
        FrameLayout menuitem_theme_layout = (FrameLayout) menu.findItem(R.id.menuitem_theme).getActionView();
        theme_btn = menuitem_theme_layout.findViewById(R.id.icon);
        theme_btn_ripple = (RippleDrawable) theme_btn.getBackground();
        theme_btn_ripple.setRadius((int) getResources().getDimension(R.dimen.theme_button_ripple));
        initThemeButton();

        // search filter button menu item and its corresponding edittext
        FrameLayout menuitem_searchfilter_layout = (FrameLayout) menu.findItem(R.id.menuitem_searchfilter).getActionView();
        searchFilter_btn = menuitem_searchfilter_layout.findViewById(R.id.icon);
        searchFilter_btn_ripple = (RippleDrawable) searchFilter_btn.getBackground();
        return true;
    }

    @Override
    protected void onPause() {
        System.out.println("paused");

        if (isPermissionGranted) {
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
        super.onPause();
    }

    @Override
    protected void onStop() {
        System.out.println("stopped");
        if (isPermissionGranted) {

        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        System.out.println("destroyed");
        isDestroyed = true;
        super.onDestroy();
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

        // asynchronously gets all playlists from database, then updates main activity
        databaseRepository.asyncInitAllPlaylists();
    }

    public void initThemeButton() {
        isThemeSelecting = false;
        Messenger themeMessenger = new Messenger(messageHandler);
        final Intent chooseThemeIntent = new Intent(this, ChooseThemeActivity.class);
        chooseThemeIntent.putExtra("mainActivityMessenger", themeMessenger);
        final Animation rotate = AnimationUtils.loadAnimation(this, R.anim.rotate_themebtn_animation);

        theme_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isThemeSelecting) {
                    isThemeSelecting = true;
                    theme_btn.startAnimation(rotate);
                    startActivity(chooseThemeIntent);
                }
            }
        });
    }

    public void getMusic() {
        ContentResolver contentResolver = getContentResolver();
        Uri songUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor songCursor = contentResolver.query(songUri, null, null, null, null);

        if (songCursor != null && songCursor.moveToFirst()) {
            SongHelper.setSongCursorIndexes(songCursor);

            do {
                Song song = SongHelper.createSong(songCursor);
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
                                Playlist current_playlist = new Playlist(DatabaseRepository.generatePlaylistId(), current_playlistName, current_playlistSongs);
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
                            playlistAdapter.add(mediastorePlaylist);
                        }
                    }
                });
                // update db to notify that mediastore playlists (if any) have been imported
                databaseRepository.updateMetadataIsMediaStorePlaylistsImported(true);
            }
        });
    }

    /**
     * Sets the action bar as the toolbar, which can be overlaid by an actionmode
     */
    public void initActionBar(){
        // using toolbar as ActionBar without title
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
    }

    @TargetApi(23)
    @SuppressLint("ClickableViewAccessibility")
    public void initFilterSearch() {
        // set this click listener to manually call the action mode's click listener
        searchFilter_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchFilter_editText.hasFocus()){
                    // hide keyboard and clear focus and text from searchfilter
                    InputMethodManager inputMethodManager =
                            (InputMethodManager) MainActivity.this.getSystemService(
                                    Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(
                            MainActivity.this.getCurrentFocus().getWindowToken(), 0);
                    searchFilter_editText.clearFocus();
                    searchFilter_editText.getText().clear();
                    searchFilter_editText.setVisibility(View.INVISIBLE);
                }
                else {
                    // show keyboard and focus on the searchfilter
                    searchFilter_editText.setVisibility(View.VISIBLE);
                    if (searchFilter_editText.requestFocus()) {
                        InputMethodManager inputMethodManager = (InputMethodManager)
                                getSystemService(Activity.INPUT_METHOD_SERVICE);
                        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }
                }
            }
        });

        // init searchfilter text usage functionality
        searchFilter_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // filter based on song name
                songListadapter.getFilter().filter(cs);
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void afterTextChanged(Editable arg0) {
            }
        });

        // init arraylist of all views under the sliding up panel layout
        ArrayList<View> views = getAllChildren(slidingUpPanelLayout);

        // theme btn is a menu item (not a child view of this layout), so manually add to arraylist
        views.add(theme_btn);

        // set touch listener for all views to hide the keyboard when touched
        for (View innerView : views){
            // excluding the searchfilter and its button
            if (innerView != searchFilter_editText && innerView != searchFilter_btn) {
                innerView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        // only attempt to hide keyboard if the list filter is in focus
                        if (searchFilter_editText.hasFocus()) {
                            InputMethodManager inputMethodManager =
                                    (InputMethodManager) MainActivity.this.getSystemService(
                                            Activity.INPUT_METHOD_SERVICE);
                            inputMethodManager.hideSoftInputFromWindow(
                                    MainActivity.this.getCurrentFocus().getWindowToken(), 0);
                        }
                        searchFilter_editText.clearFocus();
                        return false;
                    }
                });
            }
        }
    }

    /**
     * Method used to initialize all UI components that are related to the device's music
     */
    public void initMusicUI(){
        // init main ui
        initMainDisplay();
        initSeekbar();
        initSlidingUpPanel();
        initViewPager();

        // should be initialized last to set the touch listener for all views
        initFilterSearch();
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

        // set selected to be true for marquee left-right scrolling
        slidingUp_songName.setSelected(true);
        slidingUp_artistName.setSelected(true);

        // init slide and click controls for slide panel layout
        slidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                slidingUpMenuLayout.setAlpha(1 - slideOffset);
                mainActivityRelativeLayout.setAlpha(slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                    // disable buttons on the menu
                    slidingUp_prev_btn.setClickable(false);
                    slidingUp_pauseplay_btn.setClickable(false);
                    slidingUp_next_btn.setClickable(false);
                }
                if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    // enable buttons on the menu
                    slidingUp_prev_btn.setClickable(true);
                    slidingUp_pauseplay_btn.setClickable(true);
                    slidingUp_next_btn.setClickable(true);
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
        mainGradient = (GradientDrawable) ResourcesCompat.getDrawable(this.getResources(), R.drawable.gradient_default, null);
        mainGradient.mutate();
        mainActivityRelativeLayout.setBackground(mainGradient);
    }

    /**
     * Initializes the main display details, including the album art, song name, and artist name
     * On button click (overlaps with albumart), scales the view and button with an animation
     */
    public void initMainDisplay() {
        // init album art corner radius animations
        albumArt_cardView_animator_round = ObjectAnimator
                .ofFloat(albumArt_cardView, "radius", (float)albumArt_cardView.getWidth() / 2)
                .setDuration(350);
        albumArt_cardView_animator_square = ObjectAnimator
                .ofFloat(albumArt_cardView, "radius", (float)albumArt_cardView.getWidth() / 10)
                .setDuration(350);

        // if the intended album art height is greater than the actual height, resize based on screen dimensions
        if (albumArt_cardView.getLayoutParams().height > albumArt_cardView.getHeight()) {
            // out fields in [x,y] format
            int[] coords1 = new int[2];
            int[] coords2 = new int[2];
            playlistHeader.getLocationOnScreen(coords1);
            songName.getLocationOnScreen(coords2);

            // resize album art to fit between the playlist header and song title
            float height_position1 = (float) coords1[1] + (float) (playlistHeader.getHeight() * 2);
            float height_position2 = coords2[1] - (songName.getHeight() * 2);
            float height_difference = Math.abs(height_position1 - height_position2);
            float shrink_factor = height_difference / albumArt_cardView.getLayoutParams().height;

            albumArt_cardView.getLayoutParams().height *= shrink_factor;
            albumArt_cardView.getLayoutParams().width *= shrink_factor;
        }

        // init main display album art size if different from xml
        if (!isLargeAlbumArt) {
            albumArt_cardView.setScaleX(0.5f);
            albumArt_cardView.setScaleY(0.5f);
            albumArt_cardView.setRadius((float) albumArt_cardView.getWidth() / 2);
        }

        // init album art size toggle button
        albumArt_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLargeAlbumArt();
                databaseRepository.updateMetadataIsLargeAlbumArt(isLargeAlbumArt);
            }
        });

        // enable song name and artist name text with marquee scrolling
        songName.setSelected(true);
        artistName.setSelected(true);
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
        pauseplay_btn_ripple = (RippleDrawable) pauseplay_btn.getBackground();
        mainPausePlayIntent = new Intent(this, MusicPlayerService.class);
        mainPausePlayIntent.putExtra("pauseplay", mainMessenger);

        pauseplay_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(mainPausePlayIntent);
            }
        });

        // init next button click functionality and its ripple
        next_btn_ripple= (RippleDrawable) next_btn.getBackground();
        mainNextIntent = new Intent(this, MusicPlayerService.class);
        mainNextIntent.putExtra("next", mainMessenger);

        next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(mainNextIntent);
            }
        });

        // init prev button click functionality and its ripple
        prev_btn_ripple = (RippleDrawable) prev_btn.getBackground();
        mainPrevIntent = new Intent(this, MusicPlayerService.class);
        mainPrevIntent.putExtra("prev", mainMessenger);

        prev_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(mainPrevIntent);
            }
        });

        // init shuffle button functionality and its ripple
        shuffle_btn_ripple = (RippleDrawable) shuffle_btn.getBackground();
        shuffle_btn_ripple.setRadius(50);
        shuffle_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // toggle shuffle button
                if (isShuffled){
                    isShuffled = false;
                    shuffle_btn.setImageAlpha(40);
                    current_playlist = current_playlist.unshufflePlaylist(random_seed);
                }
                else {
                    isShuffled = true;
                    random_seed = Math.abs(new Random().nextInt());
                    shuffle_btn.setImageAlpha(255);
                    current_playlist = current_playlist.shufflePlaylist(random_seed);
                }
            }
        });

        // init repeat button functionality and its ripple
        repeat_btn_ripple = (RippleDrawable) repeat_btn.getBackground();
        repeat_btn_ripple.setRadius(50);
        repeat_btn.setOnClickListener(new View.OnClickListener() {
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

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                String time = SongHelper.convertTime(seekBar.getProgress());
                musicPosition.setText(time);
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
        info_btn_ripple = (RippleDrawable) info_btn.getBackground();
        infoIntent = new Intent(this, MusicDetailsActivity.class);
        info_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isInfoDisplaying) {
                    isInfoDisplaying = true;
                    infoIntent.putExtra("currentSong", current_song);
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

    public void initViewPager(){
        // remove any existing tabs prior to activity pause and re-add
        tabLayout.removeAllTabs();
        tabLayout.addTab(tabLayout.newTab().setText("Songs"));
        tabLayout.addTab(tabLayout.newTab().setText("Playlists"));

        pagerAdapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount(), songListadapter, playlistAdapter, mainActivityMessenger, this);

        // set dynamic viewpager
        viewPager.setMaxPages(pagerAdapter.getCount());
        viewPager.setBackgroundAsset(ThemeColors.getThemeBackgroundAssetResourceId());
        viewPager.setAdapter(pagerAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        viewPager.setPageTransformer(true, new ViewPager.PageTransformer() {
            private static final float MIN_SCALE = 0.85f;
            private static final float MIN_ALPHA = 0.5f;

            @Override
            public void transformPage(@NonNull View view, float position) {
                int pageWidth = view.getWidth();
                int pageHeight = view.getHeight();

                if (position < -1) { // [-Infinity,-1)
                    // This page is way off-screen to the left.
                    view.setAlpha(0f);

                } else if (position <= 1) { // [-1,1]
                    // Modify the default slide transition to shrink the page as well
                    float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                    float vertMargin = pageHeight * (1 - scaleFactor) / 2;
                    float horzMargin = pageWidth * (1 - scaleFactor) / 2;
                    if (position < 0) {
                        view.setTranslationX(horzMargin - vertMargin / 2);
                    } else {
                        view.setTranslationX(-horzMargin + vertMargin / 2);
                    }

                    // Scale the page down (between MIN_SCALE and 1)
                    view.setScaleX(scaleFactor);
                    view.setScaleY(scaleFactor);

                    // Fade the page relative to its size.
                    view.setAlpha(MIN_ALPHA +
                            (scaleFactor - MIN_SCALE) /
                                    (1 - MIN_SCALE) * (1 - MIN_ALPHA));

                } else { // (1,+Infinity]
                    // This page is way off-screen to the right.
                    view.setAlpha(0f);
                }
            }
        });
    }

    /**
     * Set the theme to the resource id provided and apply its colors to this activity
     * @param theme_resid the resource id of the theme to apply
     */
    public void updateTheme(int theme_resid) {
        setTheme(theme_resid);
        musicListRelativeLayout.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        SongListTab.toggleTabColor();
        PlaylistTab.toggleTabColor();
        updateSeekBarColors();
        updateTabLayoutColors();
        updateViewPager();
        updateSearchFilterColors();
        updateMainColors();
        updateSlidingMenuColors();
        updateActionBarColors();
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

        songName.setTextColor(textSongColor);
        artistName.setTextColor(textArtistColor);
        musicPosition.setTextColor(textSeekbarColor);
        musicDuration.setTextColor(textSeekbarColor);
        mainGradient.setColors(new int[]{primaryColor, secondaryColor});
        mainGradient.setOrientation(GradientDrawable.Orientation.BL_TR);

        // update drawable vector colors
        Drawable unwrappedDrawableInfo = info_btn.getDrawable();
        Drawable unwrappedDrawablePauseplay = pauseplay_btn.getDrawable();
        Drawable unwrappedDrawableNext = next_btn.getDrawable();
        Drawable unwrappedDrawablePrev = prev_btn.getDrawable();
        Drawable unwrappedDrawableShuffle = shuffle_btn.getDrawable();
        Drawable unwrappedDrawableRepeat = repeat_btn.getDrawable();
        Drawable wrappedDrawableInfo = DrawableCompat.wrap(unwrappedDrawableInfo);
        Drawable wrappedDrawablePauseplay = DrawableCompat.wrap(unwrappedDrawablePauseplay);
        Drawable wrappedDrawableNext = DrawableCompat.wrap(unwrappedDrawableNext);
        Drawable wrappedDrawablePrev = DrawableCompat.wrap(unwrappedDrawablePrev);
        Drawable wrappedDrawableShuffle = DrawableCompat.wrap(unwrappedDrawableShuffle);
        Drawable wrappedDrawableRepeat = DrawableCompat.wrap(unwrappedDrawableRepeat);
        DrawableCompat.setTint(wrappedDrawableInfo, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
        DrawableCompat.setTint(wrappedDrawablePauseplay, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
        DrawableCompat.setTint(wrappedDrawableNext, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
        DrawableCompat.setTint(wrappedDrawablePrev, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
        DrawableCompat.setTint(wrappedDrawableShuffle, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
        DrawableCompat.setTint(wrappedDrawableRepeat, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));

        // update ripple colors
        info_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
        pauseplay_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
        next_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
        prev_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
        shuffle_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
        repeat_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
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
        seekBar.getThumb().setTint(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        seekBar.getProgressDrawable().setTint(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        ((RippleDrawable)seekBar.getBackground()).setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getRippleDrawableColorId())));
    }

    private void updateTabLayoutColors(){
        tabLayout.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        tabLayout.setTabTextColors(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.TAB_TEXT_COLOR)));
        tabLayout.setSelectedTabIndicatorColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
    }

    private void updateViewPager(){
        viewPager.setBackgroundAsset(ThemeColors.getThemeBackgroundAssetResourceId());
    }

    @TargetApi(21)
    private void updateSearchFilterColors(){
        searchFilter_editText.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        searchFilter_editText.setHintTextColor(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
        searchFilter_editText.setBackgroundTintList(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));

        // update the drawable vector color
        Drawable unwrappedDrawableSearchFilter = searchFilter_btn.getDrawable();
        Drawable wrappedDrawableSearchFilter = DrawableCompat.wrap(unwrappedDrawableSearchFilter);
        DrawableCompat.setTint(wrappedDrawableSearchFilter, getResources().getColor(ThemeColors.getDrawableVectorColorId()));

        // update the ripple color
        searchFilter_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getRippleDrawableColorId())));
    }

    @TargetApi(21)
    private void updateActionBarColors(){
        // set color of the toolbar, which is the support action bar, and its title
        toolbar.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        toolbar_title.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));

        // update ripple color of theme button
        theme_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getRippleDrawableColorId())));
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
                Playlist updated_playlist = new Playlist(playlist.getId(), playlist.getName(), playlist_songs);
                databaseRepository.insertPlaylist(updated_playlist);
            }
        }
    }

    /**
     * Helper method to enable scaling of album art and visibility of song name & artist
     * Based on largeAlbumArt boolean conditions:
     * false - album art is not large and song name & artist are visible
     * true - album art is large and hides song name & artist
     */
    @TargetApi(16)
    public void toggleLargeAlbumArt(){
        if (isLargeAlbumArt) {
            isLargeAlbumArt = false;
            albumArt_cardView.animate().scaleX(0.5f).scaleY(0.5f);
            albumArt_cardView_animator_round.start();
        }
        else{
            isLargeAlbumArt = true;
            albumArt_cardView.animate().scaleX(1f).scaleY(1f);
            albumArt_cardView_animator_square.start();
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
                repeat_btn.setImageResource(R.drawable.ic_repeat28dp);
                repeat_btn.setImageAlpha(40);
                break;

            // repeat playlist
            case 1:
                repeat_btn.setImageResource(R.drawable.ic_repeat28dp);
                repeat_btn.setImageAlpha(255);
                break;

            // repeat one song
            case 2:
                repeat_btn.setImageResource(R.drawable.ic_repeat_one28dp);
                repeat_btn.setImageAlpha(255);
                break;
        }

        // set appropriate colors for the button
        Drawable unwrappedDrawableRepeat = repeat_btn.getDrawable();
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
        final MainActivity mainActivity = this;
        switch (operation) {
            case DatabaseRepository.ASYNC_INIT_ALL_PLAYLISTS:
                // remove any songs that were not able to be found in the device
                playlistList = (ArrayList<Playlist>) object;
                cleanPlaylistDatabase();
                songListadapter = new SongListAdapter(this, R.layout.adapter_song_layout, fullSongList, this);
                playlistAdapter = new PlaylistAdapter(this, R.layout.adapter_playlist_layout, playlistList, this);

                // initialize current playlist from database, if possible
                databaseRepository.asyncGetCurrentPlaylist();
                break;
            case DatabaseRepository.ASYNC_GET_CURRENT_PLAYLIST:
                current_playlist = (Playlist) object;

                // if a playlist wasn't retrieved from the database
                if (current_playlist == null) {
                    if (fullSongList.size() > 0) {
                        current_playlist = fullPlaylist;
                        current_song = fullSongList.get(0);
                    }
                }

                // retrieve metadata values from database
                databaseRepository.asyncGetMetadata();
                break;
            case DatabaseRepository.ASYNC_INSERT_PLAYLIST:
                // update current playlist adapter with the newly created playlist
                playlistAdapter.add((Playlist) object);

                // move to playlist tab
                viewPager.setCurrentItem(PagerAdapter.PLAYLISTS_TAB);
                break;
            case DatabaseRepository.ASYNC_MODIFY_PLAYLIST:
                Playlist temp_playlist = (Playlist) object;
                Playlist original_playlist = (Playlist) playlistAdapter.getItem(playlistAdapter.getPosition(temp_playlist));

                // check if songs were removed from existing playlist
                if (original_playlist.getSize() > temp_playlist.getSize()) {
                    // modify the original playlist to adopt the changes
                    original_playlist.adoptSongList(temp_playlist);

                    // reconstruct viewpager adapter to reflect changes to individual playlist
                    pagerAdapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount(), songListadapter, playlistAdapter, mainActivityMessenger, mainActivity);
                    viewPager.setAdapter(pagerAdapter);

                    // adjust tab colors
                    SongListTab.toggleTabColor();
                    PlaylistTab.toggleTabColor();

                    // move to playlist tab
                    viewPager.setCurrentItem(PagerAdapter.PLAYLISTS_TAB);
                }

                // playlist was simply renamed, or extended, notify playlist adapter
                else{
                    playlistAdapter.notifyDataSetChanged();
                }
                break;
            case DatabaseRepository.ASYNC_DELETE_PLAYLISTS_BY_ID:
                ArrayList<Playlist> playlists = (ArrayList<Playlist>) object;
                for (Playlist playlist : playlists){
                    playlistAdapter.remove(playlist);
                }
                break;
            case DatabaseRepository.ASYNC_GET_METADATA:
                if (object == null){
                    // insert metadata row into the database for the first time
                    databaseRepository.insertMetadata(Metadata.DEFAULT_METADATA);
                }

                Metadata metadata = object == null ? Metadata.DEFAULT_METADATA : (Metadata) object;
                boolean isPlaying = metadata.getIsPlaying();
                int seekPosition = metadata.getSeekPosition();
                int songIndex = metadata.getSongIndex();
                int themeResourceId = metadata.getThemeResourceId();
                int songtab_scrollindex = metadata.getSongtab_scrollindex();
                int songtab_scrolloffset = metadata.getSongtab_scrolloffset();
                isShuffled = metadata.getIsShuffled();
                repeat_status = metadata.getRepeatStatus();
                boolean isMediaStorePlaylistsImported = metadata.getIsMediaStorePlaylistsImported();
                isLargeAlbumArt = metadata.getIsLargeAlbumArt();
                random_seed = metadata.getRandom_seed();

                // set current song using the song index metadata
                if (current_playlist.getSize() > 0) {
                    current_song = current_playlist.getSongList().get(songIndex);
                }

                // set shuffle button transparency using the isShuffled metadata
                shuffle_btn.setImageAlpha(isShuffled ? 255 : 40);

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
                    seekBar.setProgress(seekPosition);
                }

                // set the current theme and generate theme values
                setTheme(themeResourceId);
                ThemeColors.generateThemeValues(this, themeResourceId);

                // after generating theme values, update the main ui
                updateTheme(themeResourceId);
                theme_btn.setImageResource(ThemeColors.getThemeBtnResourceId());
                SongListTab.setScrollSelection(songtab_scrollindex, songtab_scrolloffset);
                break;
            }

            if (messenger != null) {
                // send message to end the addplaylistactivity, as the views are now updated
                Message msg = Message.obtain();
                Bundle bundle = new Bundle();
                bundle.putInt("msg", AddPlaylistActivity.FINISH);
                msg.setData(bundle);
                try {
                    messenger.send(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
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

    public static Notification getNotification(){
        return notificationChannel1;
    }

    /**
     * Recursively find all child views (if any) from a view
     * @param v the view to find all children from
     * @return an arraylist of all child views under v
     */
    private ArrayList<View> getAllChildren(View v) {

        // base case for when the view is not a ViewGroup or is a ListView
        if (!(v instanceof ViewGroup) || (v instanceof ListView)) {
            ArrayList<View> viewArrayList = new ArrayList();
            viewArrayList.add(v);
            return viewArrayList;
        }

        // recursive case to add all child views from the ViewGroup, including the ViewGroup
        ArrayList<View> children = new ArrayList();

        ViewGroup viewGroup = (ViewGroup) v;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {

            View child = viewGroup.getChildAt(i);

            ArrayList<View> viewArrayList = new ArrayList();
            viewArrayList.add(v);
            viewArrayList.addAll(getAllChildren(child));

            children.addAll(viewArrayList);
        }
        return children;
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
                            pauseplay_btn.setImageResource(R.drawable.ic_play28dp);
                            slidingUp_pauseplay_btn.setImageResource(R.drawable.ic_play24dp);

                            // change sliding menu pauseplay button color
                            Drawable unwrappedDrawablePauseplay = slidingUp_pauseplay_btn.getDrawable();
                            Drawable wrappedDrawablePauseplay = DrawableCompat.wrap(unwrappedDrawablePauseplay);
                            DrawableCompat.setTint(wrappedDrawablePauseplay, ThemeColors.getContrastColor());

                            // change main pauseplay button color
                            Drawable unwrappedMainDrawablePauseplay = pauseplay_btn.getDrawable();
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
                            databaseRepository.updateMetadataSeek(seekBar.getProgress());
                        }
                    }
                    break;
                case MusicPlayerService.UPDATE_PAUSE:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pauseplay_btn.setImageResource(R.drawable.ic_pause28dp);
                            slidingUp_pauseplay_btn.setImageResource(R.drawable.ic_pause24dp);

                            // change sliding menu pauseplay button color
                            Drawable unwrappedDrawablePauseplay = slidingUp_pauseplay_btn.getDrawable();
                            Drawable wrappedDrawablePauseplay = DrawableCompat.wrap(unwrappedDrawablePauseplay);
                            DrawableCompat.setTint(wrappedDrawablePauseplay, ThemeColors.getContrastColor());

                            // change main pauseplay button color
                            Drawable unwrappedMainDrawablePauseplay = pauseplay_btn.getDrawable();
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
                            seekBar.setMax(musicMaxDuration);
                            musicDuration.setText(time);
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
                            songName.setText(current_song.getTitle());
                            artistName.setText(current_song.getArtist());
                            albumArt.setImageBitmap(current_albumImage);
                            seekBar.setMax(songDuration);
                            musicDuration.setText(SongHelper.convertTime(songDuration));
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
                case ChooseThemeActivity.THEME_SELECTED:
                    final int theme_resid = ThemeColors.getThemeResourceId();
                    final int theme_btn_resid = ThemeColors.getThemeBtnResourceId();

                    // change theme colors and button image to match the current theme
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            theme_btn.setImageResource(theme_btn_resid);
                            updateTheme(theme_resid);
                        }
                    });
                    break;
                case ChooseThemeActivity.THEME_DONE:
                    // user is finished selecting a theme
                    isThemeSelecting = false;

                    // rotate theme btn back to original orientation
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            theme_btn.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_themebtn_reverse_animation));
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
            }
        }
    }
}

