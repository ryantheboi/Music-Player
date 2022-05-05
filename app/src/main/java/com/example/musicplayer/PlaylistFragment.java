package com.example.musicplayer;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
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

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

public class PlaylistFragment extends Fragment {

    private static final String MESSENGER_TAG = "Messenger";
    private static final String PLAYLIST_TAG = "Playlist";

    private Messenger m_mainMessenger;
    private Playlist m_playlist;
    private SongListAdapter m_songListAdapter;
    private RelativeLayout m_playlist_layout;
    private CardView m_playlist_cardview;
    private ListView m_listView;
    private ImageView m_playlist_background_layer;
    private ImageView m_playlist_background_image;
    private ViewGroup decorView;
    private Toolbar m_playlist_toolbar;
    private ActionBar m_playlist_actionBar;
    private ImageButton m_back_btn;
    private TextView m_playlist_name_tv;
    private TextView m_playlist_size_tv;
    private TextView m_playlist_divider_tv;
    private TextView m_playlist_time_tv;
    private int m_total_time;
    private static ArrayList<Song> m_userSelection = new ArrayList<>();

    public PlaylistFragment() {
        super(R.layout.activity_playlist);
    }

    public static PlaylistFragment getInstance(Playlist playlist, Messenger messenger) {
        PlaylistFragment fragment = new PlaylistFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(MESSENGER_TAG, messenger);
        bundle.putParcelable(PLAYLIST_TAG, playlist);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.m_playlist = getArguments().getParcelable(PLAYLIST_TAG);
            this.m_mainMessenger = getArguments().getParcelable(MESSENGER_TAG);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        initViews(view);
        initObjects();
        initListeners();
        setTextViews();
        setThemeColors();
    }

    private void initViews(View view) {
        decorView = (ViewGroup) getActivity().getWindow().getDecorView();
        m_playlist_layout = view.findViewById(R.id.layout_playlist);
        m_playlist_toolbar = view.findViewById(R.id.toolbar_playlist);
        m_playlist_name_tv = view.findViewById(R.id.textview_playlist_name);
        m_playlist_size_tv = view.findViewById(R.id.textview_playlist_size);
        m_playlist_divider_tv = view.findViewById(R.id.textview_playlist_divider);
        m_playlist_time_tv = view.findViewById(R.id.textview_playlist_time);
        m_back_btn = view.findViewById(R.id.ibtn_playlist_back);
        m_listView = view.findViewById(R.id.listview_playlist_songs);
        m_playlist_cardview = view.findViewById(R.id.cardview_playlist);
        m_playlist_background_layer = view.findViewById(R.id.imageview_playlist_background_layer);
        m_playlist_background_image = view.findViewById(R.id.imageview_playlist_background_image);
    }

    /**
     * Initializes new instances of objects that cannot be found by views
     */
    private void initObjects() {
        m_songListAdapter = new SongListAdapter(getContext(), R.layout.adapter_song_layout, m_playlist.getSongList(), getActivity());
    }

    /**
     * Initializes the following listeners:
     * listview onItemClick and onMultiChoice listeners
     * back button onClick listener
     */
    private void initListeners(){
        // init intents and attributes for listview listeners
        final Intent musicListSelectIntent = new Intent(getContext(), MusicPlayerService.class);
        final Intent musicListQueueIntent = new Intent(getContext(), MusicPlayerService.class);
        final Intent addPlaylistIntent = new Intent(getContext(), AddPlaylistFragment.class);
        m_listView.setAdapter(m_songListAdapter);
        m_listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);

        // init listview single and multi click listeners
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
                getActivity().startService(musicListSelectIntent);
            }
        });

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
                    titleSpannableString.setSpan(new ForegroundColorSpan(ThemeColors.getColor(ThemeColors.COLOR_ACCENT)), 0, titleString.length(), 0);
                    mode.setTitle(titleSpannableString);
                } else {
                    String titleString = m_userSelection.size() + " songs selected";
                    SpannableString titleSpannableString =  new SpannableString(titleString);
                    titleSpannableString.setSpan(new ForegroundColorSpan(ThemeColors.getColor(ThemeColors.COLOR_ACCENT)), 0, titleString.length(), 0);
                    mode.setTitle(titleSpannableString);
                }
            }

            @Override
            @TargetApi(21)
            public boolean onCreateActionMode(final android.view.ActionMode mode, final Menu menu) {
                // finish any action modes from other fragments before creating a new one
                if (MainActivity.isActionMode){
                    MainActivity.actionMode.finish();
                }

                mode.getMenuInflater().inflate(R.menu.playlist_songs_menu, menu);

                // update the tint and ripple color of every item in the menu
                for (int i = 0; i < menu.size(); i++) {
                    final int item_id = i;
                    FrameLayout menuitem_layout = (FrameLayout) menu.getItem(i).getActionView();
                    ImageView menuitem_iv = menuitem_layout.findViewById(R.id.icon);

                    Drawable unwrappedDrawable = menuitem_iv.getDrawable();
                    Drawable wrappedDrawable = DrawableCompat.wrap(unwrappedDrawable);
                    DrawableCompat.setTint(wrappedDrawable, getResources().getColor(ThemeColors.getDrawableVectorColorIdAlt()));

                    RippleDrawable rippleDrawable = (RippleDrawable) menuitem_iv.getBackground();
                    rippleDrawable.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getRippleDrawableColorIdAlt())));

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
                            ((RippleDrawable)v.getBackground()).setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getRippleDrawableColorIdAlt())));
                            v.setColorFilter(getResources().getColor(ThemeColors.getDrawableVectorColorIdAlt()));
                        }
                    }
                });
                return false;
            }

            @Override
            public boolean onActionItemClicked(final android.view.ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menuitem_createqueue:
                        // count the current number of temporary (transient) queues
                        int num_transients = 0;
                        int[] transient_ids = new int[Playlist.MAX_TRANSIENTS + 1];
                        Playlist oldest_transient_playlist = null;
                        ArrayList<Playlist> allPlaylists = MainActivity.getPlaylists();
                        for (Playlist p : allPlaylists){
                            int p_transientId = p.getTransientId();
                            if (p_transientId > 0){
                                transient_ids[p_transientId] = 1;
                                num_transients++;
                                if (oldest_transient_playlist == null){
                                    oldest_transient_playlist = p;
                                }
                                else{
                                    if (p.getDateAdded() < oldest_transient_playlist.getDateAdded()){
                                        oldest_transient_playlist = p;
                                    }
                                }
                            }

                            // maximum transient playlists reached, stop counting
                            if (num_transients == Playlist.MAX_TRANSIENTS){
                                break;
                            }
                        }

                        // replace existing transient playlist with new current playlist, given the user selections
                        if (num_transients == Playlist.MAX_TRANSIENTS){
                            Playlist transient_playlist = new Playlist(oldest_transient_playlist.getId(), oldest_transient_playlist.getName(), m_userSelection, oldest_transient_playlist.getTransientId());
                            MainActivity.setCurrent_transientPlaylist(transient_playlist);
                            MainActivity.setCurrent_song(m_userSelection.get(0));

                            AddPlaylistFragment.sendPlaylistUpdateMessage(transient_playlist, m_mainMessenger, AddPlaylistFragment.MODIFY_PLAYLIST);
                        }

                        // construct new transient and current playlist, given the user selections
                        else {
                            // create transient id (less than or equal to MAX_TRANSIENTS)
                            for (int curr_transient_id = 1; curr_transient_id < Playlist.MAX_TRANSIENTS + 1; curr_transient_id++){
                                int transient_id_flag = transient_ids[curr_transient_id];
                                if (transient_id_flag == 0){
                                    // transient id currently does not exist and may be used
                                    Playlist transient_playlist = new Playlist(DatabaseRepository.generatePlaylistId(), "TEMP_QUEUE_" + curr_transient_id, m_userSelection, curr_transient_id);
                                    MainActivity.setCurrent_transientPlaylist(transient_playlist);
                                    MainActivity.setCurrent_song(m_userSelection.get(0));

                                    AddPlaylistFragment.sendPlaylistUpdateMessage(transient_playlist, m_mainMessenger, AddPlaylistFragment.ADD_PLAYLIST);
                                    break;
                                }
                            }
                        }

                        // notify music player service to start the new song in the new playlist (queue)
                        musicListQueueIntent.putExtra("musicListSong", "");
                        getActivity().startService(musicListQueueIntent);

                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.menuitem_createplaylist:
                        // construct named playlist
                        Playlist playlist = new Playlist(getString(R.string.Favorites), m_userSelection);
                        addPlaylistIntent.putExtra("addPlaylist", playlist);
                        addPlaylistIntent.putExtra("messenger", m_mainMessenger);
                        startActivity(addPlaylistIntent);

                        mode.finish(); // Action picked, so close the CAB
                        return true;

                    case R.id.menuitem_removesong:
                        // construct alert dialog for removing playlist
                        AlertDialog.Builder removeSong_dialogBuilder = new AlertDialog.Builder(getActivity(), ThemeColors.getAlertDialogStyleResourceId());
                        if (m_userSelection.size() == 1) {
                            Song song = m_userSelection.get(0);
                            removeSong_dialogBuilder.setTitle("Remove Song " + song.getTitle() + "?");
                        }
                        else{
                            removeSong_dialogBuilder.setTitle("Remove " + m_userSelection.size() + " songs?");
                        }

                        // ok (remove) button
                        removeSong_dialogBuilder.setPositiveButton(R.string.Remove, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();

                                // remove selected song(s) from this playlist
                                m_playlist.removeAll(m_userSelection);

                                // send message to update mainactivity
                                Message msg = Message.obtain();
                                Bundle bundle = new Bundle();
                                bundle.putInt("update", AddPlaylistFragment.MODIFY_PLAYLIST);
                                bundle.putParcelable("playlist", m_playlist);
                                msg.setData(bundle);
                                try {
                                    m_mainMessenger.send(msg);
                                } catch (RemoteException e) {
                                    Logger.logException(e, "PlaylistFragment");
                                }

                                // update playlist size textview with new songs count
                                String playlist_size = m_playlist.getSize() + " Songs";
                                m_playlist_size_tv.setText(playlist_size);

                                // update playlist time textview with new total time
                                int remove_time = 0;
                                for (Song playlist_song : m_userSelection){
                                    remove_time += playlist_song.getDuration();
                                }
                                m_total_time -= remove_time;
                                m_playlist_time_tv.setText(SongHelper.convertTime(m_total_time));

                                mode.finish(); // Action picked, so close the CAB
                            }
                        });

                        // cancel button
                        removeSong_dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        // show dialog
                        removeSong_dialogBuilder.show();
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
        m_back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStackImmediate();
            }
        });
    }

    /**
     * sets the text for every textview in this activity appropriate to this playlist
     */
    private void setTextViews(){
        // calculate playlist total time
        ArrayList<Song> playlists_songs = m_playlist.getSongList();
        m_total_time = 0;
        for (Song playlist_song : playlists_songs){
            m_total_time += playlist_song.getDuration();
        }
        String playlist_size = m_playlist.getSize() + " Songs";

        m_playlist_name_tv.setText(m_playlist.getName());
        m_playlist_size_tv.setText(playlist_size);
        m_playlist_time_tv.setText(SongHelper.convertTime(m_total_time));
    }

    /**
     * adjust activity colors for the current theme
     */
    private void setThemeColors() {
        m_playlist_layout.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        m_playlist_cardview.setCardBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_SECONDARY));
        m_playlist_background_layer.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        m_playlist_background_image.setImageResource(ThemeColors.getThemeBackgroundAssetResourceId());
        m_playlist_name_tv.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        m_playlist_size_tv.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        m_playlist_divider_tv.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        m_playlist_time_tv.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
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
        DrawableCompat.setTint(wrappedBackBtn, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));

        RippleDrawable back_btn_ripple = (RippleDrawable) m_back_btn.getBackground();
        back_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
    }
}
