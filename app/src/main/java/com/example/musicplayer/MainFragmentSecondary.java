package com.example.musicplayer;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Messenger;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

public class MainFragmentSecondary extends Fragment {

    private static final String MESSENGER_TAG = "Messenger";
    private MainActivity mainActivity;
    private Messenger mainActivityMessenger;

    // sliding up panel
    private SlidingUpPanelLayout slidingUpPanelLayout;
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
    private Intent mainDisplay_PausePlayIntent;
    private Intent mainDisplay_PrevIntent;
    private Intent mainDisplay_NextIntent;
    private boolean seekBar_isTracking;
    private RelativeLayout mainDisplay_mainLayout;
    private RelativeLayout slidingUpMenuLayout;
    private ImageView slidingUp_albumArt;
    private TextView slidingUp_songName;
    private TextView slidingUp_artistName;
    private Intent slidingUp_pauseplayIntent;
    private Intent slidingUp_prevIntent;
    private Intent slidingUp_nextIntent;
    private ImageButton slidingUp_prev_btn;
    private ImageButton slidingUp_pauseplay_btn;
    private ImageButton slidingUp_next_btn;
    private RippleDrawable slidingUp_prev_btn_ripple;
    private RippleDrawable slidingUp_pauseplay_btn_ripple;
    private RippleDrawable slidingUp_next_btn_ripple;

    public MainFragmentSecondary() {
        super(R.layout.fragment_main_secondary);
    }

    public static MainFragmentSecondary getInstance(Messenger messenger) {
        MainFragmentSecondary fragment = new MainFragmentSecondary();
        Bundle bundle = new Bundle();
        bundle.putParcelable(MESSENGER_TAG, messenger);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.mainActivityMessenger = getArguments().getParcelable(MESSENGER_TAG);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        slidingUpPanelLayout = (SlidingUpPanelLayout) mainActivity.getMainActivityLayout();
        initViews(view);
    }

    public void initMainFragmentSecondaryUI(){
        initMainGradient();
        initMainDisplay();
        initMainDisplayButtons();
        initSeekbar();
        initSlidingUpPanel();
    }

    public void updateFragmentColors(){
        updateSeekBarColors();
        updateMainDisplayColors();
        updateSlidingMenuColors();
    }

    public int getSeekbarProgress(){
        return mainDisplay_seekBar.getProgress();
    }

    public boolean getSeekbar_isTracking(){
        return seekBar_isTracking;
    }

    /**
     * Sets the seekbar progress
     * @param seekPosition the progress position in the seekbar
     */
    public void setSeekbarProgress(int seekPosition){
        if (!seekBar_isTracking) {
            mainDisplay_seekBar.setProgress(seekPosition);
        }
    }

    public void setPausePlayBtns(int mainBtnId, int slidingBtnId){
        mainDisplay_pauseplay_btn.setImageResource(mainBtnId);
        slidingUp_pauseplay_btn.setImageResource(slidingBtnId);

        // change sliding menu pauseplay button color
        Drawable unwrappedDrawablePauseplay = slidingUp_pauseplay_btn.getDrawable();
        Drawable wrappedDrawablePauseplay = DrawableCompat.wrap(unwrappedDrawablePauseplay);
        DrawableCompat.setTint(wrappedDrawablePauseplay, ThemeColors.getContrastColor());

        // change main pauseplay button color
        Drawable unwrappedMainDrawablePauseplay = mainDisplay_pauseplay_btn.getDrawable();
        Drawable wrappedMainDrawablePauseplay = DrawableCompat.wrap(unwrappedMainDrawablePauseplay);
        DrawableCompat.setTint(wrappedMainDrawablePauseplay, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
    }

    public void setShuffleBtnAlpha(int alpha){
        mainDisplay_shuffle_btn.setImageAlpha(alpha);
    }

    /**
     * Set the appearance of the repeat button, based on the repeat mode
     * @param mode 0 for repeat none, 1 for repeat one song, 2 for repeat playlist
     */
    public void setRepeatButton(int mode){
        switch (mode){
            case PlaybackStateCompat.REPEAT_MODE_NONE:
                mainDisplay_repeat_btn.setImageResource(R.drawable.ic_repeat28dp);
                mainDisplay_repeat_btn.setImageAlpha(40);
                break;
            case PlaybackStateCompat.REPEAT_MODE_ONE:
                mainDisplay_repeat_btn.setImageResource(R.drawable.ic_repeat_one28dp);
                mainDisplay_repeat_btn.setImageAlpha(255);
                break;
            case PlaybackStateCompat.REPEAT_MODE_ALL:
                mainDisplay_repeat_btn.setImageResource(R.drawable.ic_repeat28dp);
                mainDisplay_repeat_btn.setImageAlpha(255);
                break;
        }

        // set appropriate colors for the button
        Drawable unwrappedDrawableRepeat = mainDisplay_repeat_btn.getDrawable();
        Drawable wrappedDrawableRepeat = DrawableCompat.wrap(unwrappedDrawableRepeat);
        DrawableCompat.setTint(wrappedDrawableRepeat, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
    }

    /**
     * Set the scaling of album art and visibility of song name & artist
     * based on isAlbumArtCircular boolean flag
     * @param isAlbumArtCircular
     *      false - album art is not large and song name & artist are visible
     *      true - album art is large and hides song name & artist
     */
    public void setAlbumArtCircular(boolean isAlbumArtCircular){
        if (isAlbumArtCircular) {
            // set main display album art circular
            mainDisplay_albumArt_cardView.setScaleX(0.95f);
            mainDisplay_albumArt_cardView.setScaleY(0.95f);
            mainDisplay_albumArt_cardView.setRadius((float) mainDisplay_albumArt_cardView.getWidth() / 2);
        }
        else{
            // set main display album art rounded rectangular
            mainDisplay_albumArt_cardView.setScaleX(1f);
            mainDisplay_albumArt_cardView.setScaleY(1f);
            mainDisplay_albumArt_cardView.setRadius((float) mainDisplay_albumArt_cardView.getWidth() / 10);
        }
    }

    /**
     * Updates sliding up panel with details about the song and playlist,
     * using a mediametadata object from a mediacontroller
     * @param metadata title, artist, album, albumart, and duration of a song
     * @param playlist the playlist the current song is from
     */
    public void updateMainSongDetails(MediaMetadataCompat metadata, Playlist playlist){
        String songTitle = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE);
        String songArtist = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
        int songDuration = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        Bitmap albumArt = Song.getAlbumArtBitmap(mainActivity, metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI));

        // update sliding menu details
        slidingUp_songName.setText(songTitle);
        slidingUp_artistName.setText(songArtist);
        slidingUp_albumArt.setImageBitmap(albumArt);

        // update main activity details
        mainDisplay_playlistHeader.setText(playlist.getName());
        mainDisplay_songTitle.setText(songTitle);
        mainDisplay_songArtist.setText(songArtist);
        mainDisplay_albumArt.setImageBitmap(albumArt);
        mainDisplay_seekBar.setMax(songDuration);
        mainDisplay_musicDuration.setText(SongHelper.convertTime(songDuration));

        // generate appropriate palette swatch colors using this song's album art
        ThemeColors.generatePaletteColors(albumArt);
    }

    /**
     * Overloaded method to updates sliding up panel with details about the song and playlist,
     * using a song object (without relying on media metadata change)
     * @param song the song to update this fragment's details with
     * @param playlist the playlist the current song is from
     */
    public void updateMainSongDetails(Song song, Playlist playlist){
        String songTitle = song.getTitle();
        String songArtist = song.getArtist();
        int songDuration = song.getDuration();
        Bitmap albumArt = Song.getAlbumArtBitmap(mainActivity, song.getAlbumID());

        // update sliding menu details
        slidingUp_songName.setText(songTitle);
        slidingUp_artistName.setText(songArtist);
        slidingUp_albumArt.setImageBitmap(albumArt);

        // update main activity details
        mainDisplay_playlistHeader.setText(playlist.getName());
        mainDisplay_songTitle.setText(songTitle);
        mainDisplay_songArtist.setText(songArtist);
        mainDisplay_albumArt.setImageBitmap(albumArt);
        mainDisplay_seekBar.setMax(songDuration);
        mainDisplay_musicDuration.setText(SongHelper.convertTime(songDuration));

        // generate appropriate palette swatch colors using this song's album art
        ThemeColors.generatePaletteColors(albumArt);
    }

    private void initViews(View view){
        slidingUpMenuLayout = view.findViewById(R.id.sliding_menu);
        slidingUp_albumArt = view.findViewById(R.id.sliding_albumart);
        slidingUp_songName = view.findViewById(R.id.sliding_title);
        slidingUp_artistName = view.findViewById(R.id.sliding_artist);
        slidingUp_pauseplay_btn = view.findViewById(R.id.sliding_btn_play);
        slidingUp_next_btn = view.findViewById(R.id.sliding_btn_next);
        slidingUp_prev_btn = view.findViewById(R.id.sliding_btn_prev);
        mainDisplay_mainLayout = view.findViewById(R.id.mainlayout);
        mainDisplay_albumArt = view.findViewById(R.id.song_albumart);
        mainDisplay_albumArt_cardView = view.findViewById(R.id.song_cardview);
        mainDisplay_albumArt_btn = view.findViewById(R.id.toggle_largeAlbumArt);
        mainDisplay_songTitle = view.findViewById(R.id.song_title);
        mainDisplay_songArtist = view.findViewById(R.id.song_artist);
        mainDisplay_seekBar = view.findViewById(R.id.seekBar);
        mainDisplay_slidedown_btn = view.findViewById(R.id.slidedown_btn);
        mainDisplay_info_btn = view.findViewById(R.id.btn_info);
        mainDisplay_pauseplay_btn = view.findViewById(R.id.btn_play);
        mainDisplay_next_btn = view.findViewById(R.id.btn_next);
        mainDisplay_prev_btn = view.findViewById(R.id.btn_prev);
        mainDisplay_shuffle_btn = view.findViewById(R.id.btn_shuffle);
        mainDisplay_repeat_btn = view.findViewById(R.id.btn_repeat);
        mainDisplay_musicPosition = view.findViewById(R.id.music_position);
        mainDisplay_musicDuration = view.findViewById(R.id.music_duration);
        mainDisplay_playlistHeader = view.findViewById(R.id.playlist_header);
    }

    private void initMainDisplayButtons(){
        initMainButtons();
        initInfoButton();
    }

    /**
     * Initialize views and buttons in the sliding up panel
     * The sliding up panel can be expanded upon click, but not collapsed
     * Buttons will be invisible and unclickable depending on the panel state
     */
    private void initSlidingUpPanel() {
        // init functionality for buttons on sliding menu
        initSlidingUpPanelButtons();

        // enable sliding menu text marquee left-right scrolling
        slidingUp_songName.setSelected(true);
        slidingUp_artistName.setSelected(true);

        // init state of sliding panel views
        if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            // set layout alphas
            slidingUpMenuLayout.setAlpha(0);
            mainDisplay_mainLayout.setAlpha(1);

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
            mainDisplay_mainLayout.setAlpha(0);

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
                mainDisplay_mainLayout.setAlpha(slideOffset);
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
    private void initSlidingUpPanelButtons() {
        // init pauseplay button and ripple drawable
        slidingUp_pauseplay_btn_ripple = (RippleDrawable) slidingUp_pauseplay_btn.getBackground();

        slidingUp_pauseplayIntent = new Intent(mainActivity, MusicPlayerService.class);
        slidingUp_pauseplayIntent.putExtra("pauseplay", mainActivityMessenger);

        slidingUp_pauseplay_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pbState = MediaControllerCompat.getMediaController(mainActivity).getPlaybackState().getState();
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    MediaControllerCompat.getMediaController(mainActivity).getTransportControls().pause();
                } else {
                    MediaControllerCompat.getMediaController(mainActivity).getTransportControls().play();
                }
            }
        });

        // init next button and ripple drawable
        slidingUp_next_btn_ripple = (RippleDrawable) slidingUp_next_btn.getBackground();

        slidingUp_nextIntent = new Intent(mainActivity, MusicPlayerService.class);
        slidingUp_nextIntent.putExtra("next", mainActivityMessenger);

        slidingUp_next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(mainActivity).getTransportControls().skipToNext();
            }
        });

        // init prev button and ripple drawable
        slidingUp_prev_btn_ripple = (RippleDrawable) slidingUp_prev_btn.getBackground();

        slidingUp_prevIntent = new Intent(mainActivity, MusicPlayerService.class);
        slidingUp_prevIntent.putExtra("prev", mainActivityMessenger);

        slidingUp_prev_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(mainActivity).getTransportControls().skipToPrevious();
            }
        });
    }

    /**
     * Initialize the gradient, which can be mutated based on the on the theme and album art
     */
    @TargetApi(16)
    private void initMainGradient() {
        // set up gradient background which can be mutated
        mainDisplay_mainGradient = (GradientDrawable) ResourcesCompat.getDrawable(this.getResources(), R.drawable.gradient_default, null);
        mainDisplay_mainGradient.mutate();
        mainDisplay_mainLayout.setBackground(mainDisplay_mainGradient);
    }

    /**
     * Initializes the main display details, including the album art, song name, and artist name
     * On button click (overlaps with albumart), scales the view and button with an animation
     */
    private void initMainDisplay() {
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

        // init album art size toggle button
        mainDisplay_albumArt_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleLargeAlbumArt();
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
    private void initMainButtons() {
        // init pauseplay button click functionality and its ripple
        mainDisplay_pauseplay_btn_ripple = (RippleDrawable) mainDisplay_pauseplay_btn.getBackground();
        mainDisplay_pauseplay_btn_ripple.setRadius(70);
        mainDisplay_PausePlayIntent = new Intent(mainActivity, MusicPlayerService.class);
        mainDisplay_PausePlayIntent.putExtra("pauseplay", mainActivityMessenger);

        mainDisplay_pauseplay_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pbState = MediaControllerCompat.getMediaController(mainActivity).getPlaybackState().getState();
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    MediaControllerCompat.getMediaController(mainActivity).getTransportControls().pause();
                } else {
                    MediaControllerCompat.getMediaController(mainActivity).getTransportControls().play();
                }
            }
        });

        // init next button click functionality and its ripple
        mainDisplay_next_btn_ripple = (RippleDrawable) mainDisplay_next_btn.getBackground();
        mainDisplay_next_btn_ripple.setRadius(70);
        mainDisplay_NextIntent = new Intent(mainActivity, MusicPlayerService.class);
        mainDisplay_NextIntent.putExtra("next", mainActivityMessenger);

        mainDisplay_next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(mainActivity).getTransportControls().skipToNext();
            }
        });

        // init prev button click functionality and its ripple
        mainDisplay_prev_btn_ripple = (RippleDrawable) mainDisplay_prev_btn.getBackground();
        mainDisplay_prev_btn_ripple.setRadius(70);
        mainDisplay_PrevIntent = new Intent(mainActivity, MusicPlayerService.class);
        mainDisplay_PrevIntent.putExtra("prev", mainActivityMessenger);

        mainDisplay_prev_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaControllerCompat.getMediaController(mainActivity).getTransportControls().skipToPrevious();
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
                if (MainActivity.getShuffleMode() == PlaybackStateCompat.SHUFFLE_MODE_ALL){
                    mainDisplay_shuffle_btn.setImageAlpha(40);
                    MediaControllerCompat.getMediaController(mainActivity).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_NONE);
                }
                else {
                    mainDisplay_shuffle_btn.setImageAlpha(255);
                    MediaControllerCompat.getMediaController(mainActivity).getTransportControls().setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
                }
            }
        });

        // init repeat button functionality and its ripple
        mainDisplay_repeat_btn_ripple = (RippleDrawable) mainDisplay_repeat_btn.getBackground();
        mainDisplay_repeat_btn_ripple.setRadius(70);
        mainDisplay_repeat_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleRepeatButton(MainActivity.getRepeat_mode());
            }
        });
    }

    /**
     * Initializes the seekbar and the textviews for current position and max duration
     * Time is converted from milliseconds to HH:MM:SS format
     * Textview for current position is updated upon user changing the seekbar
     */
    private void initSeekbar() {
        // set the seekbar & textview duration and sync with mediaplayer
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
                MediaControllerCompat.getMediaController(mainActivity).getTransportControls().seekTo(seekBar.getProgress());
                seekBar_isTracking = false;
            }
        });
    }

    /**
     * Initializes the button for displaying details about a song in a scrollview
     * Upon clicking the button, the current song will be sent in the intent
     */
    private void initInfoButton() {
        mainDisplay_info_btn_ripple = (RippleDrawable) mainDisplay_info_btn.getBackground();
        mainDisplay_info_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mainActivity.getIsInfoDisplaying()) {
                    mainActivity.setIsInfoDisplaying(true);
                    mainActivity.setSlidingUpPanelTouchEnabled(false);
                    Song current_song = MainActivity.getCurrent_song();
                    MusicDetailsFragment musicDetailsFragment = MusicDetailsFragment.getInstance(
                            current_song, mainActivity.getFullSongMetadataHashMap().get(current_song.getId()));
                    mainActivity.getSupportFragmentManager().beginTransaction()
                            .setReorderingAllowed(true)
                            .add(R.id.fragment_main_secondary, musicDetailsFragment)
                            .addToBackStack("musicDetailsFragment")
                            .commit();
                }
            }
        });
    }

    /**
     * updates the two main gradients and the text colors in the main display
     * gradient colors are derived from the current theme and song
     */
    @TargetApi(21)
    private void updateMainDisplayColors() {
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
    private void updateSlidingMenuColors() {
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
     * Helper method to enable scaling of album art and visibility of song name & artist
     * Based on largeAlbumArt boolean conditions:
     * false - album art is not large and song name & artist are visible
     * true - album art is large and hides song name & artist
     */
    @TargetApi(16)
    private void toggleLargeAlbumArt(){
        if (mainActivity.getIsAlbumArtCircular()) {
            mainActivity.setIsAlbumArtCircular(false);
            mainDisplay_albumArt_cardView.animate().scaleX(1f).scaleY(1f);
            mainDisplay_albumArt_cardView_animator_square.start();
        }
        else{
            mainActivity.setIsAlbumArtCircular(true);
            mainDisplay_albumArt_cardView.animate().scaleX(0.95f).scaleY(0.95f);
            mainDisplay_albumArt_cardView_animator_round.start();
        }
    }

    /**
     * Helper method to set the next appearance of the repeat button, based on the repeat mode
     * and set the next repeat mode through media controller
     * Progression Loop: NONE -> ALL -> ONE -> NONE -> ...
     * @param mode 0 for repeat none, 1 for repeat one song, 2 for repeat playlist
     */
    private void toggleRepeatButton(int mode){
        switch (mode){
            // next state will repeat all
            case PlaybackStateCompat.REPEAT_MODE_NONE:
                setRepeatButton(PlaybackStateCompat.REPEAT_MODE_ALL);
                MediaControllerCompat.getMediaController(mainActivity).getTransportControls().setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ALL);
                break;

            // next state will disable repeat
            case PlaybackStateCompat.REPEAT_MODE_ONE:
                setRepeatButton(PlaybackStateCompat.REPEAT_MODE_NONE);
                MediaControllerCompat.getMediaController(mainActivity).getTransportControls().setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE);
                break;

            // next state will repeat one song
            case PlaybackStateCompat.REPEAT_MODE_ALL:
                setRepeatButton(PlaybackStateCompat.REPEAT_MODE_ONE);
                MediaControllerCompat.getMediaController(mainActivity).getTransportControls().setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE);
                break;
        }
    }
}
