package com.example.musicplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.graphics.drawable.DrawableCompat;

import java.util.ArrayList;

public class PlaylistActivity extends Activity {

    private Messenger m_mainMessenger;
    private Playlist m_playlist;
    private ListView m_listView;
    private RelativeLayout m_playlist_layout;
    private ImageButton m_back_btn;
    private TextView m_playlist_name_tv;
    private TextView m_playlist_size_tv;
    private SongListAdapter m_songListAdapter;
    private static ArrayList<Song> m_userSelection = new ArrayList<>();

    @Override
    @TargetApi(21)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_playlist);
        m_playlist_layout = findViewById(R.id.activity_playlist);

        // obtain the intent that started this activity (should contain an extra with a Playlist)
        Intent intent = this.getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {
            for (String key : b.keySet()) {
                switch (key) {
                    case "playlist":
                        // get the song which needs its details to be displayed
                        m_playlist = intent.getParcelableExtra("playlist");
                        break;
                    case "mainMessenger":
                        m_mainMessenger = intent.getParcelableExtra("mainMessenger");
                        break;
                }
            }
        }

        // initialize and set text in textviews
        m_playlist_size_tv = findViewById(R.id.textview_playlist_size);
        m_playlist_name_tv = findViewById(R.id.textview_playlist_name);
        String playlist_size = m_playlist.getSize() + " Songs";
        m_playlist_size_tv.setText(playlist_size);
        m_playlist_name_tv.setText(m_playlist.getName());

        // set the window layout for a clean look
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout((int) (width), (int) (height));

        // init intents
        final Intent musicListSelectIntent = new Intent(this, MusicPlayerService.class);
        final Intent musicListQueueIntent = new Intent(this, MusicPlayerService.class);
        final Intent addPlaylistIntent = new Intent(this, AddPlaylistActivity.class);

        // initialize listview and adapter using the songs in the playlist
        m_listView = findViewById(R.id.listview_playlist_songs);
        m_songListAdapter = new SongListAdapter(this, R.layout.adapter_song_layout, m_playlist.getSongList(), this);
        m_listView.setAdapter(m_songListAdapter);

        // init decorView (Action Mode toolbar)
        final ViewGroup decorView = (ViewGroup) this.getWindow().getDecorView();

        m_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected song object
                Song song = (Song) m_listView.getItemAtPosition(position);

                // redirect the current playlist to reference the songs in this playlist
                MainActivity.setCurrent_playlist(new Playlist(m_playlist.getName(), m_playlist.getSongList()));

                // change current song
                MainActivity.setCurrent_song(song);

                // notify music player service about the current song change
                musicListSelectIntent.putExtra("musicListSong", "");
                startService(musicListSelectIntent);
            }
        });

        m_listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        m_listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
                // obtain the selected song object and add to user selection arraylist
                Song song = (Song) m_listView.getItemAtPosition(position);

                if (!checked) {
                    m_userSelection.remove(song);
                } else {
                    m_userSelection.add(song);
                }

                if (m_userSelection.size() == 1) {
                    String titleString = m_userSelection.get(0).getTitle();
                    SpannableString titleSpannableString =  new SpannableString(titleString);
                    titleSpannableString.setSpan(new ForegroundColorSpan(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR)), 0, titleString.length(), 0);
                    mode.setTitle(titleSpannableString);
                } else {
                    String titleString = m_userSelection.size() + " songs selected";
                    SpannableString titleSpannableString =  new SpannableString(titleString);
                    titleSpannableString.setSpan(new ForegroundColorSpan(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR)), 0, titleString.length(), 0);
                    mode.setTitle(titleSpannableString);
                }
            }

            @Override
            @TargetApi(21)
            public boolean onCreateActionMode(final android.view.ActionMode mode, final Menu menu) {
                mode.getMenuInflater().inflate(R.menu.playlist_songs_menu, menu);

                // update the tint and ripple color of every item in the menu
                for (int i = 0; i < menu.size(); i++) {
                    final int item_id = i;
                    FrameLayout menuitem_layout = (FrameLayout) menu.getItem(i).getActionView();
                    ImageView menuitem_iv = menuitem_layout.findViewById(R.id.icon);

                    Drawable unwrappedDrawable = menuitem_iv.getDrawable();
                    Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
                    DrawableCompat.setTint(wrappedDrawable, getResources().getColor(ThemeColors.getDrawableVectorColorId()));

                    RippleDrawable rippleDrawable = (RippleDrawable) menuitem_iv.getBackground();
                    rippleDrawable.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getRippleDrawableColorId())));

                    // set this click listener to manually call the action mode's click listener
                    menuitem_iv.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onActionItemClicked(mode, menu.getItem(item_id));
                        }
                    });
                }

                MainActivity.isActionMode = true;
                MainActivity.actionMode = mode;
                return true;
            }

            @Override
            @TargetApi(21)
            public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                // update the colors of the close button and background bar on the ui thread
                decorView.post(new Runnable() {
                    @Override
                    public void run()
                    {
                        int buttonId = R.id.action_mode_close_button;
                        androidx.appcompat.widget.AppCompatImageView v = decorView.findViewById(buttonId);

                        if (v != null)
                        {
                            ((View)v.getParent()).setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
                            ((RippleDrawable)v.getBackground()).setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getRippleDrawableColorId())));
                            v.setColorFilter(getResources().getColor(ThemeColors.getDrawableVectorColorId()));
                        }
                    }
                });
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menuitem_createqueue:
                        // construct new current playlist, given the user selections
                        MainActivity.setCurrent_playlist(new Playlist("USER_SELECTION", m_userSelection));
                        MainActivity.setCurrent_song(m_userSelection.get(0));

                        // notify music player service to start the new song in the new playlist (queue)
                        musicListQueueIntent.putExtra("musicListSong", "");
                        startService(musicListQueueIntent);

                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.menuitem_createplaylist:
                        // construct named playlist
                        Playlist playlist = new Playlist(getString(R.string.Favorites), m_userSelection);
                        addPlaylistIntent.putExtra("addPlaylist", playlist);
                        startActivity(addPlaylistIntent);

                        mode.finish(); // Action picked, so close the CAB
                        return true;

                    case R.id.menuitem_removesong:
                        // remove selected song(s) from this playlist
                        m_playlist.removeAll(m_userSelection);

                        // send message to update mainactivity
                        Message msg = Message.obtain();
                        Bundle bundle = new Bundle();
                        bundle.putInt("update", AddPlaylistActivity.EXTEND_PLAYLIST);
                        bundle.putParcelable("playlist", m_playlist);
                        msg.setData(bundle);
                        try {
                            m_mainMessenger.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

                        // update playlist size textview with new songs count
                        String playlist_size = m_playlist.getSize() + " Songs";
                        m_playlist_size_tv.setText(playlist_size);

                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                MainActivity.isActionMode = false;
                MainActivity.actionMode = null;
                m_userSelection.clear();
            }
        });

        // initialize button to return to previous activity
        m_back_btn = findViewById(R.id.ibtn_playlist_back);
        m_back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // adjust activity colors for the current theme
        setThemeColors();

    }

    /**
     * adjust activity colors for the current theme
     */
    private void setThemeColors() {
        m_playlist_layout.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        m_songListAdapter.setItemsFrameColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        m_playlist_name_tv.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        m_songListAdapter.setItemsTitleTextColor(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.ITEM_TEXT_COLOR)));
        m_songListAdapter.setItemsAlbumArtistTextColor(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
        setBackBtnColor();
    }

    /**
     * sets the color of the back button and its ripple to match the current theme
     */
    @TargetApi(21)
    private void setBackBtnColor(){
        Drawable unwrappedBackBtn = m_back_btn.getDrawable();
        Drawable wrappedBackBtn = DrawableCompat.wrap(unwrappedBackBtn);
        DrawableCompat.setTint(wrappedBackBtn, getResources().getColor(ThemeColors.getDrawableVectorColorId()));

        RippleDrawable back_btn_ripple = (RippleDrawable) m_back_btn.getBackground();
        back_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getRippleDrawableColorId())));
    }
}
