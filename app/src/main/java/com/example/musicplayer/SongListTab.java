package com.example.musicplayer;

import android.annotation.TargetApi;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Messenger;
import android.support.v4.media.session.MediaControllerCompat;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SongListTab#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SongListTab extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static ListView listView;
    private static ImageView background;
    private static SongListAdapter songListAdapter;
    private static Messenger mainActivityMessenger;
    private static MainActivity mainActivity;
    private static ArrayList<Song> userSelection;
    private ViewGroup decorView;

    // TODO: Rename and change types of parameters
    private String mParam1;

    public SongListTab() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment Tab.
     */
    // TODO: Rename and change types and number of parameters
    public static SongListTab newInstance(String param1, SongListAdapter adapter, Messenger messenger, MainActivity activity) {
        SongListTab fragment = new SongListTab();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        userSelection = new ArrayList<>();
        songListAdapter = adapter;
        mainActivityMessenger = messenger;
        mainActivity = activity;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    @TargetApi(21)
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_tab_songs, container, false);
        background = fragmentView.findViewById(R.id.background_layer);

        // init decorView (Action Mode toolbar)
        decorView = (ViewGroup) getActivity().getWindow().getDecorView();

        // init listview
        listView = fragmentView.findViewById(R.id.fragment_listview_songs);
        listView.setAdapter(songListAdapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setFastScrollEnabled(true); // support index scrolling
        listView.setNestedScrollingEnabled(true); // support scrolling with the coordinator layout

        // init functionality for the views
        initListeners();
        return fragmentView;
    }

    /**
     * Initializes the following listeners:
     * listview onItemClick and onMultiChoice listeners
     */
    private void initListeners(){
        // init listview listeners
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected song object
                Song song = (Song) listView.getItemAtPosition(position);

                // redirect the current playlist to reference the full (original) playlist
                MainActivity.setCurrent_playlist_shufflemode(MainActivity.getFullPlaylist());

                // change current song
                MainActivity.setCurrent_song(song);

                // notify music player service about the current song change
                Bundle song_bundle = new Bundle();
                song_bundle.putParcelable("song", song);
                MediaControllerCompat.getMediaController(mainActivity).getTransportControls().sendCustomAction(MusicPlayerService.CUSTOM_ACTION_PLAY_SONG, song_bundle);
            }
        });

        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
                // obtain the selected song object and add to user selection arraylist
                Song song = (Song) listView.getItemAtPosition(position);

                if (!checked) {
                    userSelection.remove(song);
                } else {
                    userSelection.add(song);
                }

                if (userSelection.size() == 1) {
                    String titleString = userSelection.get(0).getTitle();
                    SpannableString titleSpannableString =  new SpannableString(titleString);
                    titleSpannableString.setSpan(new ForegroundColorSpan(ThemeColors.getColor(ThemeColors.COLOR_ACCENT)), 0, titleString.length(), 0);
                    mode.setTitle(titleSpannableString);
                } else {
                    String titleString = userSelection.size() + " songs selected";
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

                mode.getMenuInflater().inflate(R.menu.songs_menu, menu);

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
            public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menuitem_createqueue:
                        Playlist transient_playlist = Playlist.createTransientPlaylist(userSelection);
                        Song song = userSelection.get(0);
                        MainActivity.setCurrent_playlist_shufflemode(transient_playlist);
                        MainActivity.setCurrent_song(song);

                        // replace existing transient playlist with new current playlist
                        if (Playlist.isNumTransientsMaxed()){
                            AddPlaylistFragment.sendPlaylistUpdateMessage(transient_playlist, mainActivityMessenger, AddPlaylistFragment.MODIFY_PLAYLIST);
                        }

                        // construct new transient and current playlist
                        else {
                            AddPlaylistFragment.sendPlaylistUpdateMessage(transient_playlist, mainActivityMessenger, AddPlaylistFragment.ADD_PLAYLIST);
                        }

                        // notify music player service to start the new song in the new playlist (queue)
                        Bundle song_bundle = new Bundle();
                        song_bundle.putParcelable("song", song);
                        MediaControllerCompat.getMediaController(mainActivity).getTransportControls().sendCustomAction(MusicPlayerService.CUSTOM_ACTION_PLAY_SONG, song_bundle);

                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    case R.id.menuitem_createplaylist:
                        // construct named playlist and send it to addPlaylist fragment
                        Playlist playlist = new Playlist(getString(R.string.Favorites), userSelection);

                        AddPlaylistFragment addPlaylistFragment = AddPlaylistFragment.getInstance(playlist, mainActivityMessenger);
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .setReorderingAllowed(true)
                                .add(R.id.fragment_main_primary, addPlaylistFragment)
                                .addToBackStack("addPlaylistFragment")
                                .commit();

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
                userSelection.clear();
            }
        });
    }

    public static void toggleTabColor(){
        background.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        listView.setFastScrollStyle(ThemeColors.getThemeResourceId());
        songListAdapter.setItemsTitleTextColor(mainActivity.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.ITEM_TEXT_COLOR)));
        songListAdapter.setItemsAlbumArtistTextColor(mainActivity.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
    }

    /**
     * Gets the top visible list item, which may be partially scrolled out of view.
     * Must be used in conjunction with an offset to calculate the exact scroll position.
     * @return the position within the adapter's data set for the first item displayed on screen
     */
    public static int getScrollIndex(){
        return listView.getFirstVisiblePosition();
    }

    /**
     * Calculates the relative offset from the top of the listview
     * @return the relative offset from the top of the ListView, if there is a top, otherwise 0
     */
    public static int getScrollOffset(){
        View top_item = listView.getChildAt(0);
        int offset = (top_item == null) ? 0 : (top_item.getTop() - listView.getPaddingTop());
        return offset;
    }

    /**
     * Sets the listview's position, given an item's index and offset from the top
     * @param index the top visible list item
     * @param offset the top item's relative offset from the top of the listview
     */
    public static void setScrollSelection(int index, int offset){
        listView.setSelectionFromTop(index, offset);
    }

}