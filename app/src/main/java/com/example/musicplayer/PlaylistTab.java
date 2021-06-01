package com.example.musicplayer;

import android.annotation.TargetApi;
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
public class PlaylistTab extends Fragment {

    public static final int REMOVE_PLAYLISTS = 95;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static ListView m_listView;
    private static PlaylistAdapter m_playlistAdapter;
    private static MainActivity m_mainActivity;
    private static Messenger m_mainMessenger;
    private static ArrayList<Playlist> m_userSelection = new ArrayList<>();

    // TODO: Rename and change types of parameters
    private String mParam1;

    public PlaylistTab() {
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
    public static PlaylistTab newInstance(String param1, PlaylistAdapter adapter, Messenger messenger, MainActivity activity) {
        PlaylistTab fragment = new PlaylistTab();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        m_playlistAdapter = adapter;
        m_mainMessenger = messenger;
        m_mainActivity = activity;
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
        View fragmentView = inflater.inflate(R.layout.fragment_tab_playlists, container, false);
        m_listView = fragmentView.findViewById(R.id.fragment_listview_playlists);
        m_listView.setAdapter(m_playlistAdapter);

        // init decorView (Action Mode toolbar)
        final ViewGroup decorView = (ViewGroup) getActivity().getWindow().getDecorView();

        // support scrolling with the coordinator layout
        m_listView.setNestedScrollingEnabled(true);
        m_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected playlist object
                Playlist playlist = (Playlist) m_listView.getItemAtPosition(position);

                Intent playlistIntent = new Intent(m_mainActivity, PlaylistActivity.class);
                playlistIntent.putExtra("playlist", playlist);
                playlistIntent.putExtra("mainMessenger", m_mainMessenger);
                m_mainActivity.startActivity(playlistIntent);
            }
        });

        m_listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        m_listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
                // obtain the selected song object and add to user selection arraylist
                Playlist playlist = (Playlist) m_listView.getItemAtPosition(position);

                if (!checked) {
                    m_userSelection.remove(playlist);
                } else {
                    m_userSelection.add(playlist);
                }

                if (m_userSelection.size() == 1) {
                    String titleString = m_userSelection.get(0).getName();
                    SpannableString titleSpannableString =  new SpannableString(titleString);
                    titleSpannableString.setSpan(new ForegroundColorSpan(ThemeColors.getColor(ThemeColors.COLOR_ACCENT)), 0, titleString.length(), 0);
                    mode.setTitle(titleSpannableString);
                } else {
                    String titleString = m_userSelection.size() + " playlists selected";
                    SpannableString titleSpannableString =  new SpannableString(titleString);
                    titleSpannableString.setSpan(new ForegroundColorSpan(ThemeColors.getColor(ThemeColors.COLOR_ACCENT)), 0, titleString.length(), 0);
                    mode.setTitle(titleSpannableString);
                }
            }

            @Override
            @TargetApi(21)
            public boolean onCreateActionMode(final android.view.ActionMode mode, final Menu menu) {
                mode.getMenuInflater().inflate(R.menu.playlist_menu, menu);

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
                    case R.id.menuitem_removeplaylist:
                        // construct id array from the user selected playlists and send to main
                        int numPlaylists = m_userSelection.size();
                        int[] playlistIds = new int[numPlaylists];
                        for (int i = 0; i < numPlaylists; i++){
                            playlistIds[i] = m_userSelection.get(i).getId();
                        }

                        // send message to update mainactivity
                        Message msg = Message.obtain();
                        Bundle bundle = new Bundle();
                        bundle.putInt("update", REMOVE_PLAYLISTS);
                        bundle.putIntArray("ids", playlistIds);
                        msg.setData(bundle);
                        try {
                            m_mainMessenger.send(msg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

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
        

        return fragmentView;
    }

    public static void toggleTabColor(){
        m_listView.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        m_playlistAdapter.setItemsFrameColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        m_playlistAdapter.setItemsTitleTextColor(m_mainActivity.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.ITEM_TEXT_COLOR)));
        m_playlistAdapter.setItemsSizeTextColor(m_mainActivity.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
    }
}