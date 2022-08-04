package com.example.musicplayer;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

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
    private static ImageView background;
    private static PlaylistAdapter m_playlistAdapter;
    private static MainActivity m_mainActivity;
    private static Messenger m_mainMessenger;
    private static ArrayList<Playlist> m_userSelection = new ArrayList<>();
    private static EditText renamePlaylist_input;
    private ViewGroup decorView;
    private View renamePlaylist_view;
    private AlertDialog renamePlaylist_dialog;
    private AlertDialog.Builder renamePlaylist_dialogBuilder;


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
        background = fragmentView.findViewById(R.id.background_layer);

        // init decorView (Action Mode toolbar)
        decorView = (ViewGroup) getActivity().getWindow().getDecorView();

        // init listview
        m_listView = fragmentView.findViewById(R.id.fragment_listview_playlists);
        m_listView.setAdapter(m_playlistAdapter);
        m_listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        m_listView.setNestedScrollingEnabled(true); // support scrolling with the coordinator layout

        // init rename alert dialog components
        renamePlaylist_view = LayoutInflater.from(m_mainActivity).inflate(R.layout.input_dialog_addplaylist, m_listView, false);
        renamePlaylist_input = renamePlaylist_view.findViewById(R.id.input);

        // init functionality for the views
        initListeners();
        return fragmentView;
    }

    /**
     * Initializes the following listeners:
     * listview onItemClick and onMultiChoice listeners
     * renameInput editText onTextChange listener
     */
    private void initListeners(){

        // init listview listeners
        m_listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected playlist object and launch playlist fragment
                Playlist playlist = (Playlist) m_listView.getItemAtPosition(position);

                PlaylistFragment playlistFragment = PlaylistFragment.getInstance(playlist);
                getActivity().getSupportFragmentManager().beginTransaction()
                        .setReorderingAllowed(true)
                        .add(R.id.fragment_main_primary, playlistFragment)
                        .addToBackStack("playlistTabFragment")
                        .commit();
            }
        });

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
                // finish any action modes from other fragments before creating a new one
                if (MainActivity.isActionMode){
                    MainActivity.actionMode.finish();
                }

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
            public boolean onActionItemClicked(final android.view.ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menuitem_renameplaylist:

                        // construct dialog to input playlist name
                        renamePlaylist_dialogBuilder = new AlertDialog.Builder(m_mainActivity, ThemeColors.getAlertDialogStyleResourceId());
                        renamePlaylist_dialogBuilder.setTitle(R.string.RenamePlaylist);
                        // avoid adding the child again if it already exists
                        if (renamePlaylist_view.getParent() != null) {
                            ((ViewGroup) renamePlaylist_view.getParent()).removeView(renamePlaylist_view);
                        }
                        renamePlaylist_dialogBuilder.setView(renamePlaylist_view);

                        // ok (rename) button
                        renamePlaylist_dialogBuilder.setPositiveButton(R.string.Rename, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                // rename playlist(s) by the order they were selected
                                for (int i = 0; i < m_userSelection.size(); i++){
                                    Playlist modify_playlist = m_userSelection.get(i);
                                    if (i == 0){
                                        modify_playlist.setName(renamePlaylist_input.getText().toString());
                                    }
                                    else{
                                        modify_playlist.setName(renamePlaylist_input.getText().toString() + " (" + i + ")");
                                    }

                                    // renamed playlists will not be transient
                                    modify_playlist.setTransientId(0);

                                    // notify MainActivity about modified playlist
                                    m_mainActivity.modifyPlaylist(modify_playlist, Playlist.RENAME);
                                }
                                renamePlaylist_input.getText().clear();

                                mode.finish(); // Action picked, so close the CAB
                            }
                        });

                        // cancel button
                        renamePlaylist_dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        // create and show dialog
                        renamePlaylist_dialog = renamePlaylist_dialogBuilder.show();

                        // initially disable ok button if there isn't already text
                        if (renamePlaylist_input.getText().toString().equals("")) {
                            renamePlaylist_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                        }
                        return true;

                    case R.id.menuitem_exportplaylist:
                        // construct alert dialog for exporting playlist
                        AlertDialog.Builder exportPlaylist_dialogBuilder = new AlertDialog.Builder(m_mainActivity, ThemeColors.getAlertDialogStyleResourceId());

                        if (m_userSelection.size() == 1) {
                            exportPlaylist_dialogBuilder.setTitle("Export Playlist to " + M3U.EXPORT_DIRECTORY + "?");
                        }
                        else{
                            exportPlaylist_dialogBuilder.setTitle("Export " + m_userSelection.size() + M3U.EXPORT_DIRECTORY + "?");
                        }

                        // ok (export) button
                        exportPlaylist_dialogBuilder.setPositiveButton(R.string.Export, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();

                                // export all playlists selected
                                for (Playlist playlist : m_userSelection){
                                    M3U.exportM3U(playlist);
                                }
                                Toast.makeText(m_mainActivity.getApplicationContext(), "Exported to " + M3U.EXPORT_DIRECTORY, Toast.LENGTH_LONG).show();
                                mode.finish(); // Action picked, so close the CAB
                            }
                        });

                        // cancel button
                        exportPlaylist_dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        // show dialog
                        exportPlaylist_dialogBuilder.show();
                        return true;

                    case R.id.menuitem_removeplaylist:
                        // construct alert dialog for removing playlist
                        AlertDialog.Builder removePlaylist_dialogBuilder = new AlertDialog.Builder(m_mainActivity, ThemeColors.getAlertDialogStyleResourceId());

                        if (m_userSelection.size() == 1) {
                            Playlist playlist = m_userSelection.get(0);
                            removePlaylist_dialogBuilder.setTitle("Remove Playlist " + playlist.getName() + " (" + playlist.getSize() + " songs)?");
                        }
                        else{
                            removePlaylist_dialogBuilder.setTitle("Remove " + m_userSelection.size() + " playlists?");
                        }

                        // ok (remove) button
                        removePlaylist_dialogBuilder.setPositiveButton(R.string.Remove, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();

                                // construct id array from the user selected playlists and send to main
                                int numPlaylists = m_userSelection.size();
                                int[] playlistIds = new int[numPlaylists];
                                for (int i = 0; i < numPlaylists; i++){
                                    playlistIds[i] = m_userSelection.get(i).getPlaylistId();
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
                                    Logger.logException(e, "PlaylistTab");
                                }

                                mode.finish(); // Action picked, so close the CAB
                            }
                        });

                        // cancel button
                        removePlaylist_dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        // show dialog
                        removePlaylist_dialogBuilder.show();
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

        // init edittext listener
        renamePlaylist_input.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // disable ok button if there is no text
                if (TextUtils.isEmpty(s)) {
                    renamePlaylist_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
                // enable ok button if there is text
                else {
                    renamePlaylist_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });
    }

    public static void toggleTabColor(){
        background.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        m_playlistAdapter.setItemsTitleTextColor(m_mainActivity.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.ITEM_TEXT_COLOR)));
        m_playlistAdapter.setItemsSizeTextColor(m_mainActivity.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
        renamePlaylist_input.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        renamePlaylist_input.setHintTextColor(m_mainActivity.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
        renamePlaylist_input.setBackgroundTintList(m_mainActivity.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
    }
}