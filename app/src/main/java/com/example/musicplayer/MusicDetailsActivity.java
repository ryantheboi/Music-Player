package com.example.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.TextView;

public class MusicDetailsActivity extends Activity {

    private Song song;
    private int idx;
    private SpannableStringBuilder details = new SpannableStringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // obtain the intent that started this activity (should contain an extra with a Song)
        Intent intent = this.getIntent();
        Bundle b = intent.getExtras();
        if (b != null) {
            for (String key : b.keySet()) {
                if (key.equals("currentSong")) {
                        // get the song which needs its details to be displayed
                        song = intent.getParcelableExtra("currentSong");
                        break;
                }
            }
        }

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int height = dm.heightPixels;

        getWindow().setLayout((int) (width*.7), (int) (height*.5));

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.gravity = Gravity.CENTER;
        params.x = 30;
        params.y = -220;

        getWindow().setAttributes(params);

        TextView msgWindow = findViewById(R.id.music_details);
        for (int i = 0; i < 6; i ++){
            int detail_length = 0;
            switch(i){
                case 0:
                    appendDetail("ID", Integer.toString(song.getID()));
                    break;
                case 1:
                    appendDetail("Title", song.getTitle());
                    break;
                case 2:
                    appendDetail("Artist", song.getArtist());
                    break;
                case 3:
                    appendDetail("Album", song.getAlbum());
                    break;
                case 4:
                    appendDetail("Album ID", Integer.toString(song.getAlbumID()));
                    break;
                case 5:
                    appendDetail("Duration", MainActivity.convertTime(song.getDuration()));
                    break;
            }
            idx += detail_length;
        }
        msgWindow.setText(details);
    }

    /**
     * Appends to the SpannableStringBuilder object with the label and the detail for that label
     * Will bold the label and append two newlines
     * @param label The label of the detail, e.g. Title, Artist, Album, etc.
     * @param detail The description of the label
     */
    private void appendDetail(String label, String detail){
        details.append(label + ": " + detail + "\n\n");
        int label_length = label.length() + 2; // 2 for the ": "
        details.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), idx, idx + label_length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        details.setSpan(new RelativeSizeSpan(1.2f), idx,idx + label_length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        int total_length = label_length + detail.length() + 2; // 2 for the "\n\n"
        idx += total_length;
    }
}
