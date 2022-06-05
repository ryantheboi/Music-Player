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
import android.support.v4.media.session.MediaControllerCompat;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
    private static final int MAX_TIMER_CHARACTERS = 3;

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
    private ImageButton m_timer_btn;
    private TextView m_playlist_name_tv;
    private TextView m_playlist_size_tv;
    private TextView m_playlist_divider_tv;
    private TextView m_playlist_time_tv;
    private int m_total_time;
    private static ArrayList<Song> m_userSelection = new ArrayList<>();
    private Playlist m_timerPlaylist;
    private AlertDialog.Builder m_timer_confirm_dialogBuilder;
    private AlertDialog.Builder m_timer_dialogBuilder;
    private AlertDialog m_timer_dialog;
    private TextView m_timer_minutes_tv;
    private View m_timer_inputdialog_view;
    private EditText m_timer_inputdialog;

    public PlaylistFragment() {
        super(R.layout.fragment_playlist);
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
        m_timer_btn = view.findViewById(R.id.ibtn_timer);
        m_listView = view.findViewById(R.id.listview_playlist_songs);
        m_playlist_cardview = view.findViewById(R.id.cardview_playlist);
        m_playlist_background_layer = view.findViewById(R.id.imageview_playlist_background_layer);
        m_playlist_background_image = view.findViewById(R.id.imageview_playlist_background_image);

        // init inputdialog edittext and textview
        m_timer_inputdialog_view = LayoutInflater.from(getContext()).inflate(R.layout.input_dialog_timer, m_playlist_layout, false);
        m_timer_inputdialog = m_timer_inputdialog_view.findViewById(R.id.timer_input);
        m_timer_minutes_tv = m_timer_inputdialog_view.findViewById(R.id.timer_text_minutes);
    }

    /**
     * Initializes new instances of objects that cannot be found by views
     */
    private void initObjects() {
        m_songListAdapter = new SongListAdapter(getContext(), R.layout.adapter_song_layout, m_playlist.getSongList(), getActivity());
        m_timer_dialogBuilder = new AlertDialog.Builder(getContext(), ThemeColors.getAlertDialogStyleResourceId());
        m_timer_confirm_dialogBuilder = new AlertDialog.Builder(getActivity(), ThemeColors.getAlertDialogStyleResourceId());
        m_timer_confirm_dialogBuilder.setTitle("Confirm Timer?");
    }

    /**
     * Initializes the following listeners:
     * listview onItemClick and onMultiChoice listeners
     * back button onClick listener
     * timer edittext onTextChanged listener
     * timer button onClick listener
     */
    private void initListeners(){
        // init intents and attributes for listview listeners
        final Intent musicListSelectIntent = new Intent(getContext(), MusicPlayerService.class);
        final Intent musicListQueueIntent = new Intent(getContext(), MusicPlayerService.class);
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
                Bundle song_bundle = new Bundle();
                song_bundle.putParcelable("song", song);
                MediaControllerCompat.getMediaController(getActivity()).getTransportControls().sendCustomAction(MusicPlayerService.CUSTOM_ACTION_PLAY_SONG, song_bundle);
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
                        Playlist transient_playlist = Playlist.createTransientPlaylist(m_userSelection);
                        Song song = m_userSelection.get(0);
                        MainActivity.setCurrent_transientPlaylist(transient_playlist);
                        MainActivity.setCurrent_song(song);

                        // replace existing transient playlist with new current playlist
                        if (Playlist.isNumTransientsMaxed()){
                            AddPlaylistFragment.sendPlaylistUpdateMessage(transient_playlist, m_mainMessenger, AddPlaylistFragment.MODIFY_PLAYLIST);
                        }

                        // construct new transient and current playlist
                        else {
                            AddPlaylistFragment.sendPlaylistUpdateMessage(transient_playlist, m_mainMessenger, AddPlaylistFragment.ADD_PLAYLIST);
                        }

                        // notify music player service to start the new song in the new playlist (queue)
                        Bundle song_bundle = new Bundle();
                        song_bundle.putParcelable("song", song);
                        MediaControllerCompat.getMediaController(getActivity()).getTransportControls().sendCustomAction(MusicPlayerService.CUSTOM_ACTION_PLAY_SONG, song_bundle);

                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.menuitem_createplaylist:
                        // construct named playlist and send it to addPlaylist fragment
                        Playlist playlist = new Playlist(getString(R.string.Favorites), m_userSelection);

                        AddPlaylistFragment addPlaylistFragment = AddPlaylistFragment.getInstance(playlist, m_mainMessenger);
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .setReorderingAllowed(true)
                                .add(R.id.fragment_main_primary, addPlaylistFragment)
                                .addToBackStack("addPlaylistFragment")
                                .commit();

                        mode.finish(); // Action picked, so close the CAB
                        return true;

                    case R.id.menuitem_removesong:
                        // construct alert dialog for removing playlist
                        AlertDialog.Builder removeSong_dialogBuilder = new AlertDialog.Builder(getActivity(), ThemeColors.getAlertDialogStyleResourceId());
                        if (m_userSelection.size() == 1) {
                            removeSong_dialogBuilder.setTitle("Remove Song " + m_userSelection.get(0).getTitle() + "?");
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

        // init edittext listener
        m_timer_inputdialog.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                // prevent number from being further incremented if it will exceed the max timer allowed
                if (!TextUtils.isEmpty(s)) {
                    if (s.length() > MAX_TIMER_CHARACTERS){
                        m_timer_inputdialog.setText(s.subSequence(0, s.length() - 1));
                        m_timer_inputdialog.setSelection(m_timer_inputdialog.getText().length());
                    }
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // disable ok button if there is no text or if timer is 0, enable otherwise
                if (!TextUtils.isEmpty(s)) {
                    m_timer_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(Integer.parseInt(s.toString()) > 0);
                }
                else{
                    m_timer_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });

        // init button to create a timer with the songs in this playlist
        m_timer_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // construct dialog to input playlist name
                m_timer_dialogBuilder.setTitle(R.string.SetTimer);
                // avoid adding the child again if it already exists
                if (m_timer_inputdialog_view.getParent() != null) {
                    ((ViewGroup) m_timer_inputdialog_view.getParent()).removeView(m_timer_inputdialog_view);
                }
                m_timer_dialogBuilder.setView(m_timer_inputdialog_view);

                // ok button
                m_timer_dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        int timer_minutes = Integer.parseInt(m_timer_inputdialog.getText().toString());

                        m_timerPlaylist = m_playlist.createTimerPlaylist(timer_minutes);

                        // create show confirmation dialog
                        // ok button
                        m_timer_confirm_dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                // start current timer playlist and first song
                                Song song = m_timerPlaylist.getSongList().get(0);
                                MainActivity.setCurrent_playlist(m_timerPlaylist);
                                MainActivity.setCurrent_song(song);

                                // notify music player service about the current song change
                                Bundle song_bundle = new Bundle();
                                song_bundle.putParcelable("song", song);
                                MediaControllerCompat.getMediaController(getActivity()).getTransportControls().sendCustomAction(MusicPlayerService.CUSTOM_ACTION_PLAY_SONG, song_bundle);
                            }
                        });

                        // retry (generate new timer playlist) button
                        m_timer_confirm_dialogBuilder.setNegativeButton(R.string.GenerateAgain, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                // create new timer playlist and ask again
                                m_timerPlaylist = m_playlist.createTimerPlaylist(timer_minutes);

                                // different message if timer playlist varies by over a minute
                                int timer_difference_minutes = (int) Math.round(((double) (m_timerPlaylist.getTotalDurationMS() / 1000) / 60)) - timer_minutes;
                                if (timer_difference_minutes > 1){
                                    m_timer_confirm_dialogBuilder.setMessage("Playlist with " + m_timerPlaylist.getSizeString() +
                                            " Songs - " + SongHelper.convertTime(m_timerPlaylist.getTotalDurationMS()) +
                                            " is about " + timer_difference_minutes + " minutes more than the set timer");
                                }
                                else if (timer_difference_minutes < -1){
                                    m_timer_confirm_dialogBuilder.setMessage("Playlist with " + m_timerPlaylist.getSizeString() +
                                            " Songs - " + SongHelper.convertTime(m_timerPlaylist.getTotalDurationMS()) +
                                            " is about " + Math.abs(timer_difference_minutes) + " minutes less than the set timer");
                                }
                                else{
                                    m_timer_confirm_dialogBuilder.setMessage("Playlist with " + m_timerPlaylist.getSizeString() +
                                            " Songs - " + SongHelper.convertTime(m_timerPlaylist.getTotalDurationMS()) + " Duration");
                                }
                                m_timer_confirm_dialogBuilder.show();
                            }
                        });

                        if (m_timerPlaylist.getSize() > 0) {
                            // different message for timer dialog if timer playlist varies by over a minute
                            int timer_difference_minutes = (int) Math.round(((double) (m_timerPlaylist.getTotalDurationMS() / 1000) / 60)) - timer_minutes;
                            if (timer_difference_minutes > 1) {
                                m_timer_confirm_dialogBuilder.setMessage("Playlist with " + m_timerPlaylist.getSizeString() +
                                        " Songs - " + SongHelper.convertTime(m_timerPlaylist.getTotalDurationMS()) +
                                        " is about " + timer_difference_minutes + " minutes more than the set timer");
                            } else if (timer_difference_minutes < -1) {
                                m_timer_confirm_dialogBuilder.setMessage("Playlist with " + m_timerPlaylist.getSizeString() +
                                        " Songs - " + SongHelper.convertTime(m_timerPlaylist.getTotalDurationMS()) +
                                        " is about " + Math.abs(timer_difference_minutes) + " minutes less than the set timer");
                            } else {
                                m_timer_confirm_dialogBuilder.setMessage("Playlist with " + m_timerPlaylist.getSizeString() +
                                        " Songs - " + SongHelper.convertTime(m_timerPlaylist.getTotalDurationMS()) + " Duration");
                            }
                            m_timer_confirm_dialogBuilder.show();
                        }
                        else{
                            Toast.makeText(getActivity(),
                                    "Unable to create timer, no songs found within the time specified",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                // cancel button
                m_timer_dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                // create and show dialog
                m_timer_dialog = m_timer_dialogBuilder.show();

                // initially disable ok button if there isn't already text
                if (m_timer_inputdialog.getText().toString().equals("")) {
                    m_timer_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });
    }

    /**
     * sets the text for every textview in this activity appropriate to this playlist
     */
    private void setTextViews(){
        String playlist_size = m_playlist.getSize() + " Songs";
        m_total_time = m_playlist.getTotalDurationMS();

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
        setTimerColors();
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

    /**
     * sets the color of the timer's imagebutton and input dialog
     */
    private void setTimerColors(){
        Drawable unwrappedBackBtn = m_timer_btn.getDrawable();
        Drawable wrappedBackBtn = DrawableCompat.wrap(unwrappedBackBtn);
        DrawableCompat.setTint(wrappedBackBtn, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
        RippleDrawable back_btn_ripple = (RippleDrawable) m_timer_btn.getBackground();
        back_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));

        m_timer_minutes_tv.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        m_timer_inputdialog.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        m_timer_inputdialog.setHintTextColor(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
        m_timer_inputdialog.setBackgroundTintList(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
    }
}
