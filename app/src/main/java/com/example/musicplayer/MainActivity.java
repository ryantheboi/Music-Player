package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.palette.graphics.Palette;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
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
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.tabs.TabLayout;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;
import static android.os.Build.VERSION_CODES.Q;
import static com.example.musicplayer.Notifications.CHANNEL_ID_1;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSION_REQUEST = 1;
    private ImageButton nightModeButton;
    public static boolean nightMode = false;
    private RelativeLayout musicListRelativeLayout;
    private ArrayList<Song> fullSongList;
    private ArrayList<Playlist> playlistList;
    private static Playlist current_playlist;
    private static Playlist fullPlaylist;
    private SongListAdapter songListadapter;
    private PlaylistAdapter playlistAdapter;
    private PagerAdapter pagerAdapter;
    private Messenger mainActivityMessenger;
    private Intent musicServiceIntent;
    public static boolean isActionMode = false;
    public static ActionMode actionMode = null;

    // sliding up panel
    private boolean largeAlbumArt;
    private ImageView albumArt;
    private Button albumArt_btn;
    private ImageButton info_btn;
    private ImageButton pauseplay_btn;
    private ImageButton next_btn;
    private ImageButton prev_btn;
    private AnimationDrawable mainAnimation;
    private GradientDrawable gradient1;
    private GradientDrawable gradient2;
    private SeekBar seekBar;
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
    private Intent seekBarProgressIntent;
    private Intent seekBarSeekIntent;
    private static Song current_song = Song.EMPTY_SONG;
    private Bitmap current_albumImage;
    private Palette.Swatch vibrantSwatch;
    private Palette.Swatch darkVibrantSwatch;
    private Palette.Swatch dominantSwatch;
    private Palette.Swatch contrastSwatch;
    private List<Palette.Swatch> swatchList;
    private RelativeLayout mainActivityRelativeLayout;
    private LinearLayout slidingUpMenuLayout;
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
    private MessageHandler messageHandler;
    private MessageHandler seekbarHandler;
    private EditText searchFilter;
    private ImageView btn_searchFilter;
    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("created");

        // initialize all views
        setContentView(R.layout.activity_musiclist);
        musicListRelativeLayout = findViewById(R.id.activity_musiclist);
        nightModeButton = findViewById(R.id.btn_nightmode);
        searchFilter = findViewById(R.id.searchFilter);
        btn_searchFilter = findViewById(R.id.btn_searchfilter);
        slidingUpMenuLayout = findViewById(R.id.sliding_menu);
        slidingUp_albumArt = findViewById(R.id.sliding_albumart);
        slidingUp_songName = findViewById(R.id.sliding_title);
        slidingUp_artistName = findViewById(R.id.sliding_artist);
        slidingUpPanelLayout = findViewById(R.id.slidingPanel);
        slidingUp_pauseplay_btn = findViewById(R.id.sliding_btn_play);
        slidingUp_next_btn = findViewById(R.id.sliding_btn_next);
        slidingUp_prev_btn = findViewById(R.id.sliding_btn_prev);
        mainActivityRelativeLayout = findViewById(R.id.mainlayout);
        albumArt = findViewById(R.id.circularImageView);
        albumArt_btn = findViewById(R.id.toggle_largeAlbumArt);
        songName = findViewById(R.id.song_title);
        artistName = findViewById(R.id.song_artist);
        pauseplay_btn = findViewById((R.id.btn_play));
        next_btn = findViewById((R.id.btn_next));
        prev_btn = findViewById((R.id.btn_prev));
        seekBar = findViewById(R.id.seekBar);
        musicPosition = findViewById(R.id.music_position);
        musicDuration = findViewById(R.id.music_duration);
        info_btn = findViewById(R.id.btn_info);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
    }

    @Override
    @TargetApi(16)
    protected void onStart() {
        super.onStart();
        System.out.println("started");

        // check and request for read permissions
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            } else { // temp else branch
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
            }
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST);
        } else {
            setVolumeControlStream(AudioManager.STREAM_MUSIC);

            // init thread for message handling
            HandlerThread messageHandlerThread = new HandlerThread("MessageHandler");
            messageHandlerThread.start();
            messageHandler = new MessageHandler(messageHandlerThread.getLooper());
            mainActivityMessenger = new Messenger(messageHandler);
            musicServiceIntent = new Intent(this, MusicPlayerService.class);

            // init listview functionality and playlist
            initMusicList(); // starts music service for the first time

            initNightMode();

            // init main sliding up panel
            initMainAnimation();

            initMainDisplay();

            initMainButtons();

            initSeekbar();

            initInfoButton();

            initSlidingUpPanel();

            initFilterSearch();

            initNotification();

            initViewPager();
        }
    }

    @Override
    protected void onResume() {
        System.out.println("resumed");
        mainAnimation.start();
        super.onResume();
    }

    @Override
    protected void onPause() {
        System.out.println("paused");
        super.onPause();
    }

    @Override
    protected void onStop() {
        System.out.println("stopped");
        mainAnimation.stop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        System.out.println("destroyed");
        super.onDestroy();
    }

    /**
     * Initializes the listview with item click and multi choice listeners, and starts music service
     * On item click, the song that was clicked will be played
     * On multi choice, multiple songs can be selected to either create a queue or a new playlist
     * The music service is started here in order to send the main activity's messenger
     */
    public void initMusicList() {
        playlistList = new ArrayList<>();
        fullSongList = new ArrayList<>();
        getMusic(); // populates fullSongList
        fullPlaylist = new Playlist("FULL_PLAYLIST", fullSongList);

        // initialize current playlist and song
        if (fullSongList.size() > 0){
            current_playlist = fullPlaylist;
            current_song = fullSongList.get(0);
        }

        // start music service for the first time
        musicServiceIntent.putExtra("musicListInit", mainActivityMessenger);
        startService(musicServiceIntent);

        songListadapter = new SongListAdapter(this, R.layout.adapter_view_layout, fullSongList, this);
        playlistAdapter = new PlaylistAdapter(this, R.layout.adapter_playlist_layout, playlistList, this);
    }

    public void initNightMode() {
        nightMode = false;
        nightModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNightMode();
            }
        });
    }

    public void toggleNightMode() {
        if (!nightMode) {
            tabLayout.setBackgroundColor(getResources().getColor(R.color.nightPrimaryDark));
            tabLayout.setTabTextColors(getResources().getColorStateList(R.color.itemnightselectorblue));
            searchFilter.setTextColor(getResources().getColor(R.color.colorTextPrimaryLight));
            musicListRelativeLayout.setBackgroundColor(getResources().getColor(R.color.nightPrimaryDark));
            nightModeButton.setImageResource(R.drawable.night);
            nightMode = true;

            // swap info button color
            info_btn.setImageResource(R.drawable.info_light);
        } else {
            tabLayout.setBackgroundColor(getResources().getColor(R.color.lightPrimaryWhite));
            tabLayout.setTabTextColors(getResources().getColorStateList(R.color.itemlightselectorblue));
            searchFilter.setTextColor(getResources().getColor(R.color.colorTextPrimaryDark));
            musicListRelativeLayout.setBackgroundColor(getResources().getColor(R.color.lightPrimaryWhite));
            nightModeButton.setImageResource(R.drawable.light);
            nightMode = false;

            // swap info button color
            info_btn.setImageResource(R.drawable.info_night);
        }
        SongListTab.toggleTabColor();
        PlaylistTab.toggleTabColor();
        swapMainColors();
        swapSlidingMenuColors();
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
                fullSongList.add(song);
            } while (songCursor.moveToNext());

            songCursor.close();
        }
    }

    public void initFilterSearch() {
        // init searchfilter button functionality
        btn_searchFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchFilter.hasFocus()){
                    // hide keyboard and clear focus from searchfilter
                    InputMethodManager inputMethodManager =
                            (InputMethodManager) MainActivity.this.getSystemService(
                                    Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(
                            MainActivity.this.getCurrentFocus().getWindowToken(), 0);
                    searchFilter.clearFocus();
                    searchFilter.setVisibility(View.INVISIBLE);
                }
                else {
                    // show keyboard and focus on the searchfilter
                    searchFilter.setVisibility(View.VISIBLE);
                    if (searchFilter.requestFocus()) {
                        InputMethodManager inputMethodManager = (InputMethodManager)
                                getSystemService(Activity.INPUT_METHOD_SERVICE);
                        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
                    }
                }
            }
        });

        // init searchfilter text usage functionality
        searchFilter.addTextChangedListener(new TextWatcher() {
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

        // set touch listener for all views to hide the keyboard when touched
        ArrayList<View> views = getAllChildren(slidingUpPanelLayout);
        for (View innerView : views){
            // excluding the searchfilter and its button
            if (innerView != searchFilter && innerView != btn_searchFilter) {
                innerView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        // only attempt to hide keyboard if the list filter is in focus
                        if (searchFilter.hasFocus()) {
                            InputMethodManager inputMethodManager =
                                    (InputMethodManager) MainActivity.this.getSystemService(
                                            Activity.INPUT_METHOD_SERVICE);
                            inputMethodManager.hideSoftInputFromWindow(
                                    MainActivity.this.getCurrentFocus().getWindowToken(), 0);
                        }
                        searchFilter.clearFocus();
                        return false;
                    }
                });
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this,
                            Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show();
                        initMusicList();
                        initNightMode();
                    }
                } else {
                    Toast.makeText(this, "Permission denied..", Toast.LENGTH_SHORT).show();
                }
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

                    // enable buttons on the main display
                    info_btn.setClickable(true);
                }
                if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    // enable buttons on the menu
                    slidingUp_prev_btn.setClickable(true);
                    slidingUp_pauseplay_btn.setClickable(true);
                    slidingUp_next_btn.setClickable(true);

                    // disable buttons on the main display
                    info_btn.setClickable(false);
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
     * Initialize the animated gradient based on night mode and album art
     * Gradients are set up to be mutated, here
     */
    @TargetApi(16)
    public void initMainAnimation() {
        // set up gradients that can be mutated
        gradient1 = (GradientDrawable) ResourcesCompat.getDrawable(this.getResources(), R.drawable.gradient_default1, null);
        gradient2 = (GradientDrawable) ResourcesCompat.getDrawable(this.getResources(), R.drawable.gradient_default2, null);
        gradient1.mutate();
        gradient2.mutate();

        // 6 second animated gradients with 3 second transitions
        mainAnimation = new AnimationDrawable();
        mainAnimation.addFrame(gradient1, 6000);
        mainAnimation.addFrame(gradient2, 6000);
        mainAnimation.setEnterFadeDuration(3000);
        mainAnimation.setExitFadeDuration(3000);
        mainAnimation.setOneShot(false);
        mainActivityRelativeLayout.setBackground(mainAnimation);
    }

    /**
     * Initializes the main display details, including the album art, song name, and artist name
     * On button click (overlaps with albumart), scales the view and button with an animation
     */
    public void initMainDisplay() {
        // init album art and transparent album art button
        largeAlbumArt = true; // init album art as large
        albumArt_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLargeAlbumArt();
            }
        });

        // enable song name and artist name text with marquee scrolling
        songName.setSelected(true);
        artistName.setSelected(true);
    }

    /**
     * Initializes the three main buttons in main activity:
     * Pauseplay, previous, and next
     * Each one will trigger a unique event from MusicPlayerService
     */
    @SuppressLint("ClickableViewAccessibility")
    public void initMainButtons() {
        Messenger mainMessenger = new Messenger(messageHandler);

        // init pauseplay button click functionality
        mainPausePlayIntent = new Intent(this, MusicPlayerService.class);
        mainPausePlayIntent.putExtra("pauseplay", mainMessenger);

        pauseplay_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(mainPausePlayIntent);
            }
        });

        // init next button click functionality
        mainNextIntent = new Intent(this, MusicPlayerService.class);
        mainNextIntent.putExtra("next", mainMessenger);

        next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(mainNextIntent);
            }
        });

        // init prev button click functionality
        mainPrevIntent = new Intent(this, MusicPlayerService.class);
        mainPrevIntent.putExtra("prev", mainMessenger);

        prev_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startService(mainPrevIntent);
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
        seekBarProgressIntent = new Intent(this, MusicPlayerService.class);
        seekBarSeekIntent = new Intent(this, MusicPlayerService.class);
        Messenger mainMessenger = new Messenger(messageHandler);
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
    public void initInfoButton() {
        infoIntent = new Intent(this, MusicDetailsActivity.class);
        info_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                infoIntent.putExtra("currentSong", current_song);
                startActivity(infoIntent);
            }
        });

        // initially hidden and unclickable
        info_btn.setClickable(false);
    }


    @TargetApi(19)
    public void initNotification() {
        mediaSession = new MediaSessionCompat(this, "media");
        notificationManager = NotificationManagerCompat.from(this);

        Bitmap largeImage = BitmapFactory.decodeResource(getResources(), R.drawable.kaminomanimani);
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
        tabLayout.setTabTextColors(getResources().getColorStateList(R.color.itemlightselectorblue));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        pagerAdapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount(), songListadapter, playlistAdapter, mainActivityMessenger, this);
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
     * Swaps the two main gradients and the text colors in the main display
     * swaps gradients to darker colors if nightmode
     * swaps gradients to lighter colors otherwise
     */
    @TargetApi(16)
    public void swapMainColors() {
        int textSongColor;
        int textArtistColor;
        int textSeekbarColor;
        int primaryColor;
        int secondaryColor;
        if (nightMode) {
            // assign primary and secondary colors
            textSongColor = getResources().getColor(R.color.colorTextPrimaryLight);
            textArtistColor = getResources().getColor(R.color.colorTextSecondaryLight);
            textSeekbarColor = getResources().getColor(R.color.colorTextPrimaryLight);
            primaryColor = getResources().getColor(R.color.nightPrimaryDark);
            if (darkVibrantSwatch != null) {
                secondaryColor = darkVibrantSwatch.getRgb();
            } else if (dominantSwatch != null) {
                secondaryColor = dominantSwatch.getRgb();
            } else {
                secondaryColor = getResources().getColor(R.color.nightPrimaryGrey);
            }
        } else {
            // assign primary and secondary colors
            textSongColor = getResources().getColor(R.color.colorTextPrimaryDark);
            textArtistColor = getResources().getColor(R.color.colorTextSecondaryDark);
            textSeekbarColor = getResources().getColor(R.color.colorTextPrimaryDark);
            primaryColor = getResources().getColor(R.color.lightPrimaryWhite);
            if (vibrantSwatch != null) {
                secondaryColor = vibrantSwatch.getRgb();
            } else if (dominantSwatch != null) {
                secondaryColor = dominantSwatch.getRgb();
            } else {
                secondaryColor = Color.YELLOW;
            }
        }
        songName.setTextColor(textSongColor);
        artistName.setTextColor(textArtistColor);
        musicPosition.setTextColor(textSeekbarColor);
        musicDuration.setTextColor(textSeekbarColor);
        gradient1.setColors(new int[]{primaryColor, secondaryColor});
        gradient1.setOrientation(GradientDrawable.Orientation.TR_BL);
        gradient2.setColors(new int[]{primaryColor, secondaryColor});
        gradient2.setOrientation(GradientDrawable.Orientation.BL_TR);
    }

    /**
     * swaps the background, text, and button colors of the sliding menu
     * text and button colors are decided as the swatch that contrasts the most with the background
     */
    @TargetApi(21)
    private void swapSlidingMenuColors() {
        if (swatchList != null) {
            int base;
            if (nightMode) {
                base = getResources().getColor(R.color.nightSecondaryDark);
            } else {
                base = getResources().getColor(R.color.lightSecondaryWhite);
            }

            // find the swatch that contrasts the most with the base
            contrastSwatch = swatchList.get(0);
            int maxDiffRGB = 0;
            for (Palette.Swatch swatch : swatchList) {
                if (Math.abs(base - swatch.getRgb()) > maxDiffRGB) {
                    maxDiffRGB = Math.abs(base - swatch.getRgb());
                    contrastSwatch = swatch;
                }
            }

            // change color of sliding menu buttons and text
            slidingUp_songName.setTextColor(contrastSwatch.getRgb());
            slidingUp_artistName.setTextColor(contrastSwatch.getRgb());
            slidingUpPanelLayout.setBackgroundColor(base);
            Drawable unwrappedDrawablePauseplay = slidingUp_pauseplay_btn.getDrawable();
            Drawable unwrappedDrawableNext = slidingUp_next_btn.getDrawable();
            Drawable unwrappedDrawablePrev = slidingUp_prev_btn.getDrawable();
            Drawable wrappedDrawablePauseplay = DrawableCompat.wrap(unwrappedDrawablePauseplay);
            Drawable wrappedDrawableNext = DrawableCompat.wrap(unwrappedDrawableNext);
            Drawable wrappedDrawablePrev = DrawableCompat.wrap(unwrappedDrawablePrev);
            DrawableCompat.setTint(wrappedDrawablePauseplay, contrastSwatch.getRgb());
            DrawableCompat.setTint(wrappedDrawableNext, contrastSwatch.getRgb());
            DrawableCompat.setTint(wrappedDrawablePrev, contrastSwatch.getRgb());

            // change color of the button ripples
            slidingUp_pauseplay_btn_ripple.setColor(ColorStateList.valueOf(contrastSwatch.getRgb()));
            slidingUp_next_btn_ripple.setColor(ColorStateList.valueOf(contrastSwatch.getRgb()));
            slidingUp_prev_btn_ripple.setColor(ColorStateList.valueOf(contrastSwatch.getRgb()));
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
    public static Playlist getCurrent_playlist(){
        return current_playlist;
    }
    public static Playlist getFullPlaylist(){
        return fullPlaylist;
    }
    public static void setCurrent_song(Song song){
        current_song = song;
    }
    public static void setCurrent_playlist(Playlist playlist){
        current_playlist = playlist;
    }

    /**
     * adds a new playlist to the playlist tab of the viewpager
     * @param playlist the playlist item to be added
     */
    public void addPlaylist(Playlist playlist){
        // add playlist item to the adapter
        playlistAdapter.add(playlist);

        // reconstruct viewpager adapter with the new playlist adapter change
        PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager(), tabLayout.getTabCount(), songListadapter, playlistAdapter, mainActivityMessenger, this);
        viewPager.setAdapter(pagerAdapter);

        // adjust tab colors and move to playlist tab
        SongListTab.toggleTabColor();
        PlaylistTab.toggleTabColor();
        viewPager.setCurrentItem(PagerAdapter.PLAYLISTS_TAB);

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
                            pauseplay_btn.setImageResource(R.drawable.ic_play);
                            slidingUp_pauseplay_btn.setImageResource(R.drawable.ic_play24dp);
                            if (contrastSwatch != null){ // change sliding menu pauseplay button color
                                Drawable unwrappedDrawablePauseplay = slidingUp_pauseplay_btn.getDrawable();
                                Drawable wrappedDrawablePauseplay = DrawableCompat.wrap(unwrappedDrawablePauseplay);
                                DrawableCompat.setTint(wrappedDrawablePauseplay, contrastSwatch.getRgb());
                            }
                        }
                    });
                    notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_play24dp, "play", PendingIntent.getService(getApplicationContext(), 1, notificationPauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                    notificationChannel1 = notificationBuilder.build();
                    notificationManager.notify(1, notificationChannel1);
                    break;
                case MusicPlayerService.UPDATE_PAUSE:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            pauseplay_btn.setImageResource(R.drawable.ic_pause);
                            slidingUp_pauseplay_btn.setImageResource(R.drawable.ic_pause24dp);
                            if (contrastSwatch != null){ // change sliding menu pauseplay button color
                                Drawable unwrappedDrawablePauseplay = slidingUp_pauseplay_btn.getDrawable();
                                Drawable wrappedDrawablePauseplay = DrawableCompat.wrap(unwrappedDrawablePauseplay);
                                DrawableCompat.setTint(wrappedDrawablePauseplay, contrastSwatch.getRgb());
                            }
                        }
                    });
                    notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_pause24dp, "pause", PendingIntent.getService(getApplicationContext(), 1, notificationPauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                    notificationChannel1 = notificationBuilder.build();
                    notificationManager.notify(1, notificationChannel1);
                    break;
                case MusicPlayerService.UPDATE_SEEKBAR_DURATION:
                    // init the seekbar & textview max duration and begin thread to track progress
                    final int musicMaxDuration = (int) bundle.get("time");
                    final String time = convertTime(musicMaxDuration);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            seekBar.setMax(musicMaxDuration);
                            musicDuration.setText(time);
                        }
                    });

                    // spawn a thread to update seekbar progress each 100 milliseconds
                    final Messenger seekMessenger = new Messenger(seekbarHandler);
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
                    Uri albumArtURI = ContentUris.withAppendedId(MusicPlayerService.artURI, albumID_long);
                    ContentResolver res = getContentResolver();
                    try {
                        InputStream in = res.openInputStream(albumArtURI);
                        current_albumImage = BitmapFactory.decodeStream(in);
                        if (in != null){
                            in.close();
                        }
                    }catch(Exception e){
                        current_albumImage = BitmapFactory.decodeResource(getResources(), R.drawable.default_image);
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
                            musicDuration.setText(convertTime(songDuration));
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
                    Palette.from(current_albumImage).maximumColorCount(8).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(@Nullable Palette palette) {
                            vibrantSwatch = palette.getVibrantSwatch();
                            darkVibrantSwatch = palette.getDarkVibrantSwatch();
                            dominantSwatch = palette.getDominantSwatch();
                            swatchList = palette.getSwatches();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    swapMainColors();
                                    swapSlidingMenuColors();
                                }
                            });
                        }
                    });
                    break;
            }
        }
    }
}

