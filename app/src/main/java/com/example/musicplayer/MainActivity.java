package com.example.musicplayer;
import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;
import static com.example.musicplayer.Notifications.CHANNEL_ID_1;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
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
import android.support.v4.media.session.MediaSessionCompat;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import androidx.core.content.res.ResourcesCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import androidx.palette.graphics.Palette;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class MainActivity
        extends AppCompatActivity{

    private boolean largeAlbumArt;
    private ImageView albumArt;
    private ImageView pauseplay_background;
    private Animation pauseplayAnim;
    private Animation pauseplayBackgroundAnim;
    private Button albumArt_btn;
    private ImageButton musicList;
    private ImageButton pauseplay;
    private ImageButton next_btn;
    private ImageButton prev_btn;
    private RelativeLayout relativeLayout;
    private AnimationDrawable mainAnimation;
    private GradientDrawable gradient1;
    private GradientDrawable gradient2;
    private GradientDrawable pauseplay_background_gradient;
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
    private Intent pauseplayIntent;
    private Intent musicListIntent;
    private Intent prevIntent;
    private Intent nextIntent;
    private Intent seekBarProgressIntent;
    private Intent seekBarSeekIntent;
    private Palette.Swatch vibrantSwatch;
    private Palette.Swatch darkVibrantSwatch;
    private Palette.Swatch dominantSwatch;

    @Override
    @TargetApi(26)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initMainAnimation();

        initAlbumArt();

        initMainButtons();

        initSeekbar();

        initMusicList();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mediaSession = new MediaSessionCompat(this, "media");
        notificationManager = NotificationManagerCompat.from(this);
        showNotification();
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
        relativeLayout = findViewById(R.id.layout);
        mainAnimation = new AnimationDrawable();
        mainAnimation.addFrame(gradient1, 6000);
        mainAnimation.addFrame(gradient2, 6000);
        mainAnimation.setEnterFadeDuration(3000);
        mainAnimation.setExitFadeDuration(3000);
        mainAnimation.setOneShot(false);
        relativeLayout.setBackground(mainAnimation);
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
     * Initialize the button to display the music list activity
     * Creates messenger and initializes intent specifically for the activity
     */
    public void initMusicList(){
        // init button for displaying music list
        musicList = findViewById(R.id.btn_musiclist);
        Messenger musicListMessenger = new Messenger(new MessageHandler());
        musicListIntent = new Intent(this, MusicListActivity.class);
        musicListIntent.putExtra("mainActivity", musicListMessenger);
        musicListIntent.addFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
        musicList.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                startActivity(musicListIntent);
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
        mainPausePlayIntent = new Intent(this, MusicPlayerService.class);
        mainPausePlayIntent.putExtra("pauseplay", mainMessenger);

        pauseplayAnim = AnimationUtils.loadAnimation(this, R.anim.blink_animation);
        pauseplayBackgroundAnim = AnimationUtils.loadAnimation(this, R.anim.blink_animation_background);
        pauseplay_background_gradient = (GradientDrawable) pauseplay_background.getBackground().getCurrent();
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

        // init prev and next buttons
        prev_btn = findViewById((R.id.btn_prev));
        mainPrevIntent = new Intent(this, MusicPlayerService.class);
        mainPrevIntent.putExtra("prev", mainMessenger);
        prev_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                startService(mainPrevIntent);
            }
        });
        next_btn = findViewById((R.id.btn_next));
        mainNextIntent = new Intent(this, MusicPlayerService.class);
        mainNextIntent.putExtra("next", mainMessenger);
        next_btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                startService(mainNextIntent);
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

    @TargetApi(19)
    public void showNotification(){
        Bitmap largeImage = BitmapFactory.decodeResource(getResources(), R.drawable.kaminomanimani);
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

        // create intents for the notification action buttons
        Messenger notificationMessenger = new Messenger(new MessageHandler());
        prevIntent = new Intent(this, MusicPlayerService.class).putExtra("prev", notificationMessenger);
        pauseplayIntent =  new Intent(this, MusicPlayerService.class).putExtra("pauseplay", notificationMessenger);
        nextIntent = new Intent(this, MusicPlayerService.class).putExtra("next", notificationMessenger);

        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID_1)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_music)
                .setContentTitle("song")
                .setContentText("artist")
                .setLargeIcon(largeImage)
                .addAction(R.drawable.ic_prev24dp, "prev", PendingIntent.getService(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_play24dp, "play", PendingIntent.getService(this, 1, pauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_next24dp, "next", PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setContentIntent(contentIntent)
                .setStyle(new MediaStyle()
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
        songName.setTextColor(Color.parseColor(MusicPlayerService.DARK_TEXT));
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
        }
        else{
            pauseplay_background_gradient.setColor(Color.parseColor(MusicPlayerService.DARK_BACKGROUND));
        }
    }

    /**
     * helper method used to swap the two main gradients to dark and darkvibrant swatch, if it exists
     * swaps to dark and dominant if vibrant swatch doesn't exist
     * swaps to dark and grey if neither swatch exists
     */
    @TargetApi(16)
    public void swapDarkVibrantGradient(){
        songName.setTextColor(Color.parseColor(MusicPlayerService.ALMOST_WHITE));
        if (darkVibrantSwatch != null) {
            gradient1.setColors(new int[]{Color.parseColor(MusicPlayerService.DARK_BACKGROUND), darkVibrantSwatch.getRgb()});
            gradient1.setOrientation(GradientDrawable.Orientation.TR_BL);
            gradient2.setColors(new int[]{Color.parseColor(MusicPlayerService.DARK_BACKGROUND), darkVibrantSwatch.getRgb()});
            gradient2.setOrientation(GradientDrawable.Orientation.BL_TR);
        }
        else if (dominantSwatch != null){
            gradient1.setColors(new int[]{Color.parseColor(MusicPlayerService.DARK_BACKGROUND), dominantSwatch.getRgb()});
            gradient1.setOrientation(GradientDrawable.Orientation.TR_BL);
            gradient2.setColors(new int[]{Color.parseColor(MusicPlayerService.DARK_BACKGROUND), dominantSwatch.getRgb()});
            gradient2.setOrientation(GradientDrawable.Orientation.BL_TR);
        }
        else {
            gradient1.setColors(new int[]{Color.parseColor(MusicPlayerService.DARK_BACKGROUND), Color.parseColor(MusicPlayerService.GREY_BACKGROUND)});
            gradient1.setOrientation(GradientDrawable.Orientation.TR_BL);
            gradient2.setColors(new int[]{Color.parseColor(MusicPlayerService.DARK_BACKGROUND), Color.parseColor(MusicPlayerService.GREY_BACKGROUND)});
            gradient2.setOrientation(GradientDrawable.Orientation.BL_TR);
        }

        // change background gradient of buttons to contrast with dark theme
        if (vibrantSwatch != null){
            pauseplay_background_gradient.setColor(vibrantSwatch.getRgb());
        }
        else{
            pauseplay_background_gradient.setColor(Color.WHITE);
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

    /**
     * Class is used to handle messages (mainly about updates) sent from other activities
     * in order to keep the main activity most updated
     */
    public class MessageHandler extends Handler
    {
        @Override
        @TargetApi(24)
        public void handleMessage(Message msg) {

            Bundle bundle = msg.getData();
            int updateOperation = (int) bundle.get("update");
            switch (updateOperation) {
                case MusicPlayerService.UPDATE_PLAY:
                    pauseplay.setImageResource(R.drawable.ic_play);
                    notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_play24dp, "play", PendingIntent.getService(getApplicationContext(), 1, pauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
                    notificationChannel1 = notificationBuilder.build();
                    notificationManager.notify(1, notificationChannel1);
                    break;
                case MusicPlayerService.UPDATE_PAUSE:
                    pauseplay.setImageResource(R.drawable.ic_pause);
                    notificationBuilder.mActions.set(1, new NotificationCompat.Action(R.drawable.ic_pause24dp, "pause", PendingIntent.getService(getApplicationContext(), 1, pauseplayIntent, PendingIntent.FLAG_UPDATE_CURRENT)));
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
                    Song song = (Song) bundle.get("song");

                    // grab song album art and duration
                    int albumID = song.getAlbumID();
                    Bitmap albumImage;
                    Uri artURI = Uri.parse("content://media/external/audio/albumart");
                    Uri albumArtURI = ContentUris.withAppendedId(artURI, albumID);
                    ContentResolver res = getContentResolver();
                    try {
                        InputStream in = res.openInputStream(albumArtURI);
                        albumImage = BitmapFactory.decodeStream(in);
                    }catch(FileNotFoundException e){
                        albumImage = BitmapFactory.decodeResource(getResources(), R.drawable.default_image);
                        e.printStackTrace();
                    }
                    int songDuration = song.getDuration();


                    // update notification details
                    notificationBuilder
                            .setContentTitle(song.getTitle())
                            .setPriority(NotificationManager.IMPORTANCE_LOW)
                            .setContentText(song.getArtist())
                            .setLargeIcon(albumImage);
                    notificationChannel1 = notificationBuilder.build();
                    notificationManager.notify(1, notificationChannel1);

                    // update main activity details
                    songName.setText(song.getTitle());
                    artistName.setText(song.getArtist());
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
                    break;
                case MusicPlayerService.UPDATE_LIGHT:
                    swapVibrantGradient();
                    break;
            }
        }
    }
}
