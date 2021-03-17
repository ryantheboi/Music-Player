package com.example.musicplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AddPlaylistActivity extends Activity {

    private Playlist addPlaylist;
    private ListView listView;
    private RelativeLayout addPlaylist_layout;
    private ImageButton back_btn;
    private Button addPlaylist_button;
    private TextView addTo_tv;
    private TextView addPlaylist_tv;
    private PlaylistAdapter playlistAdapter;
    private AlertDialog.Builder addPlaylist_dialogBuilder;
    private AlertDialog addPlaylist_dialog;
    private View addPlaylist_view;
    private EditText addPlaylist_input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addplaylist);

        initViews();
        initObjects();
        setWindowLayout();
        setThemeColors();

        // obtain the intent that started this activity (should contain an extra with a Playlist)
        Intent intent = this.getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {
            for (String key : b.keySet()) {
                if (key.equals("addPlaylist")) {
                    // get the song which needs its details to be displayed
                    addPlaylist = intent.getParcelableExtra("addPlaylist");
                    break;
                }
            }
        }

        // init listview adapter and item click functionality
        listView.setAdapter(playlistAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected playlist object
                Playlist playlist = (Playlist) listView.getItemAtPosition(position);
                playlist.extend(addPlaylist);
                finish();
            }
        });

        // init back button to finish the activity
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // init edittext listener
        addPlaylist_input.addTextChangedListener(new TextWatcher() {

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
                    addPlaylist_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
                // enable ok button if there is text
                else {
                    addPlaylist_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                }
            }
        });

        // init button to add a new playlist
        addPlaylist_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // construct dialog to input playlist name
                addPlaylist_dialogBuilder.setTitle(R.string.NewPlaylist);
                // avoid adding the child again if it already exists
                if (addPlaylist_view.getParent() != null){
                    ((ViewGroup)addPlaylist_view.getParent()).removeView(addPlaylist_view);
                }
                addPlaylist_dialogBuilder.setView(addPlaylist_view);

                // ok button
                addPlaylist_dialogBuilder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // change playlist name to the user input and clear edittext
                        addPlaylist.setName(addPlaylist_input.getText().toString());
                        addPlaylist_input.getText().clear();
                        // TODO add playlist to main activity's list of playlists
                        System.out.println("new playlist not added");
                    }
                });

                // cancel button
                addPlaylist_dialogBuilder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                // create and show dialog
                addPlaylist_dialog = addPlaylist_dialogBuilder.show();

                // initially disable ok button if there isn't already text
                if (addPlaylist_input.getText().toString().equals("")) {
                    addPlaylist_dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        });
    }

    private void initViews(){
        // init layout and inflated view
        addPlaylist_layout = findViewById(R.id.activity_playlist);
        addPlaylist_view = LayoutInflater.from(this).inflate(R.layout.input_dialog_playlist, addPlaylist_layout, false);

        // init textviews
        addTo_tv = findViewById(R.id.textview_addTo);
        addPlaylist_tv = findViewById(R.id.textview_addPlaylist);

        // init listview
        listView = findViewById(R.id.listview_playlists);

        // init buttons
        back_btn = findViewById(R.id.ibtn_addPlaylist_back);
        addPlaylist_button = findViewById(R.id.btn_addPlaylist);

        // init edittext
        addPlaylist_input = addPlaylist_view.findViewById(R.id.input);
    }

    private void initObjects(){
        playlistAdapter = new PlaylistAdapter(this, R.layout.adapter_playlist_layout, MainActivity.getPlaylists(), this);
        if (MainActivity.nightMode) {
            addPlaylist_dialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialogDark);
        }
        else{
            addPlaylist_dialogBuilder = new AlertDialog.Builder(this, R.style.AlertDialogLight);
        }
    }

    private void setWindowLayout(){
        // set the window layout for a clean look
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout(width, height);
    }

    private void setThemeColors(){
        // adjust activity colors for nightmode
        if (MainActivity.nightMode){
            addTo_tv.setTextColor(getResources().getColor(R.color.colorTextPrimaryLight));
            addPlaylist_tv.setTextColor(getResources().getColor(R.color.colorTextPrimaryLight));
            addPlaylist_input.setTextColor(getResources().getColor(R.color.colorTextPrimaryLight));
            playlistAdapter.setItemsFrameColor(getResources().getColor(R.color.nightPrimaryDark));
            playlistAdapter.setItemsTitleTextColor(getResources().getColorStateList(R.color.itemnightselectorblue));
            addPlaylist_layout.setBackgroundColor(getResources().getColor(R.color.nightPrimaryDark));
        }
        else{
            addTo_tv.setTextColor(getResources().getColor(R.color.colorTextPrimaryDark));
            addPlaylist_tv.setTextColor(getResources().getColor(R.color.colorTextPrimaryDark));
            addPlaylist_input.setTextColor(getResources().getColor(R.color.colorTextPrimaryDark));
            playlistAdapter.setItemsFrameColor(getResources().getColor(R.color.lightPrimaryWhite));
            playlistAdapter.setItemsTitleTextColor(getResources().getColorStateList(R.color.itemlightselectorblue));
            addPlaylist_layout.setBackgroundColor(getResources().getColor(R.color.lightPrimaryWhite));
        }
    }
}
