package com.example.musicplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.drawable.DrawableCompat;

public class AddPlaylistActivity extends Activity {

    private Playlist addPlaylist;
    private ListView listView;
    private RelativeLayout addPlaylist_layout;
    private ImageButton back_btn;
    private ImageView addPlaylist_imageView;
    private Button addPlaylist_button;
    private TextView addPlaylist_tv;
    private TextView addTo_tv;
    private PlaylistAdapter playlistAdapter;
    private AlertDialog.Builder addPlaylist_dialogBuilder;
    private AlertDialog addPlaylist_dialog;
    private View addPlaylist_view;
    private EditText addPlaylist_input;
    private Messenger mainActivityMessenger;
    private Messenger addPlaylistMessenger;

    public static final int FINISH = 0;
    public static final int ADD_PLAYLIST = 97;
    public static final int MODIFY_PLAYLIST = 96;

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
                switch (key){
                    case "addPlaylist":
                        // get the playlist of interest for this activity
                        addPlaylist = intent.getParcelableExtra("addPlaylist");
                        break;
                    case "messenger":
                        mainActivityMessenger = intent.getParcelableExtra("messenger");
                }
            }
        }

        // init this messenger
        addPlaylistMessenger = new Messenger(new AddPlaylistMessenger());

        // init listview adapter and item click functionality
        listView.setAdapter(playlistAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                // obtain the selected playlist object to extend it with the current selection
                Playlist playlist = (Playlist) listView.getItemAtPosition(position);
                boolean isExtended = playlist.extend(addPlaylist);

                // extend was successful, notify MainActivity about change to existing playlist
                if (isExtended) {
                    sendPlaylistUpdateMessage(playlist, MODIFY_PLAYLIST);
                }
                else{
                    Toast.makeText(getApplicationContext(), "Song(s) already exist in playlist!", Toast.LENGTH_SHORT).show();
                }
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
                if (addPlaylist_view.getParent() != null) {
                    ((ViewGroup) addPlaylist_view.getParent()).removeView(addPlaylist_view);
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
                        // assign unique id to playlist
                        addPlaylist.setId(DatabaseRepository.generatePlaylistId());

                        // notify MainActivity about new playlist
                        sendPlaylistUpdateMessage(addPlaylist, ADD_PLAYLIST);
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

    private void initViews() {
        // init layout and inflated view
        addPlaylist_layout = findViewById(R.id.layout_playlists);
        addPlaylist_view = LayoutInflater.from(this).inflate(R.layout.input_dialog_addplaylist, addPlaylist_layout, false);

        // init textviews
        addTo_tv = findViewById(R.id.textview_addTo);
        addPlaylist_tv = findViewById(R.id.textview_addPlaylist);

        // init listview
        listView = findViewById(R.id.listview_playlists);

        // init addPlaylist imageview and its frame
        addPlaylist_imageView = findViewById(R.id.imageview_addPlaylist);

        // init buttons
        back_btn = findViewById(R.id.ibtn_addPlaylist_back);
        addPlaylist_button = findViewById(R.id.btn_addPlaylist);

        // init edittext
        addPlaylist_input = addPlaylist_view.findViewById(R.id.input);
    }

    /**
     * Initializes new instances of objects that cannot be found by views
     */
    private void initObjects() {
        playlistAdapter = new PlaylistAdapter(this, R.layout.adapter_playlist_layout, MainActivity.getPlaylists(), this);
        addPlaylist_dialogBuilder = new AlertDialog.Builder(this, ThemeColors.getAlertDialogStyleResourceId());
    }

    private void setWindowLayout() {
        // set the window layout for a clean look
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout(width, height);
    }

    /**
     * adjust activity colors for the current theme
     */
    @TargetApi(21)
    private void setThemeColors() {
        addPlaylist_layout.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
        addTo_tv.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        addPlaylist_tv.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        addPlaylist_input.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        addPlaylist_input.setHintTextColor(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
        addPlaylist_input.setBackgroundTintList(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
        playlistAdapter.setItemsTitleTextColor(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.ITEM_TEXT_COLOR)));
        playlistAdapter.setItemsSizeTextColor(getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
        setBackBtnColor();
        setAddPlaylistImageViewColor();
    }

    /**
     * sets the color of the back button and its ripple to match the current theme
     */
    @TargetApi(21)
    private void setBackBtnColor(){
        Drawable unwrappedBackBtn = back_btn.getDrawable();
        Drawable wrappedBackBtn = DrawableCompat.wrap(unwrappedBackBtn);
        DrawableCompat.setTint(wrappedBackBtn, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));

        RippleDrawable back_btn_ripple = (RippleDrawable) back_btn.getBackground();
        back_btn_ripple.setColor(ColorStateList.valueOf(getResources().getColor(ThemeColors.getMainRippleDrawableColorId())));
    }

    /**
     * sets the color of the addplaylist imageview and its ripple to match the current theme
     */
    @TargetApi(21)
    private void setAddPlaylistImageViewColor(){
        Drawable unwrappedImageView = addPlaylist_imageView.getDrawable();
        Drawable wrappedImageView = DrawableCompat.wrap(unwrappedImageView);
        DrawableCompat.setTint(wrappedImageView, getResources().getColor(ThemeColors.getMainDrawableVectorColorId()));
    }

    private void sendPlaylistUpdateMessage(Playlist playlist, int operation){
        // send message
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt("update", operation);
        bundle.putParcelable("playlist", playlist);
        bundle.putParcelable("messenger", addPlaylistMessenger);
        msg.setData(bundle);
        try {
            mainActivityMessenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            Logger.logException(e, "AddPlaylistActivity");
        }
    }

    public static void sendPlaylistUpdateMessage(Playlist playlist, Messenger messenger, int operation){
        // send message
        Message msg = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt("update", operation);
        bundle.putParcelable("playlist", playlist);
        bundle.putParcelable("messenger", null);
        msg.setData(bundle);
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            Logger.logException(e, "AddPlaylistActivity");
        }
    }

    private final class AddPlaylistMessenger extends Handler {

        @Override
        public void handleMessage(final Message msg) {
            Bundle bundle = msg.getData();
            int operation = (int) bundle.get("msg");
            switch (operation) {
                case FINISH:
                    finish();
                    break;
            }
        }
    }
}
