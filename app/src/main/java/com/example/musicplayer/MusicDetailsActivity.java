package com.example.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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

        TextView heading = findViewById(R.id.music_details_heading);
        TextView msgWindow = findViewById(R.id.music_details);
        ImageView details_background = findViewById(R.id.music_details_background);
        if (MusicListActivity.nightMode){
            heading.setTextColor(Color.WHITE);
            msgWindow.setTextColor(getResources().getColor(R.color.lightPrimaryWhite));
            details_background.setBackgroundColor(getResources().getColor(R.color.nightPrimaryDark));
        }
        else{
            heading.setTextColor(Color.BLACK);
            msgWindow.setTextColor(getResources().getColor(R.color.colorTextDark));
            details_background.setBackgroundColor(getResources().getColor(R.color.lightPrimaryWhite));

        }
        appendDetail("ID", Integer.toString(song.getID()));
        appendDetail("Title", song.getTitle());
        appendDetail("Artist", song.getArtist());
        appendDetail("Album", song.getAlbum());
        appendDetail("Album ID", (song.getAlbumID()));
        appendDetail("Duration", MusicListActivity.convertTime(song.getDuration()));
        appendDetail("Data Path", song.getDataPath());
        appendDetail("Size", convertMegabytesString(song.getSize()));
        appendDetail("Relative Path", song.getRelativePath());
        appendDetail("Display Name", song.getDisplayName());
        appendDetail("Date Added", convertDateTimeString(song.getDateAdded()));
        appendDetail("Date Modified", convertDateTimeString(song.getDateModified()));
        appendDetail("MIME Type", song.getMimeType());
        appendDetail("Document ID", song.getDocumentID());
        appendDetail("Original Document ID", song.getOriginalDocumentID());
        appendDetail("Instance ID", song.getInstanceID());
        appendDetail("Bucket ID", song.getBucketID());
        appendDetail("Bucket Display Name", song.getBucketDisplayName());
        msgWindow.setText(details);
    }

    /**
     * Appends to the SpannableStringBuilder object with the label and the detail for that label
     * Will bold the label and append two newlines
     * @param label The label of the detail, e.g. Title, Artist, Album, etc.
     * @param detail The description of the label
     */
    private void appendDetail(String label, String detail){
        if (detail == null) {
            detail = "null";
        }
        details.append(label + ": " + detail + "\n\n");
        int label_length = label.length() + 2; // 2 for the ": "
        details.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), idx, idx + label_length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        details.setSpan(new RelativeSizeSpan(1.2f), idx, idx + label_length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        int total_length = label_length + detail.length() + 2; // 2 for the "\n\n"
        idx += total_length;
    }

    /**
     * Given a date timestamp as the string number of seconds since 1970-01-01T00:00:00Z,
     * convert it to MM/dd/YYYY HH:mm:ss format
     * @param date the string number of seconds since 1970-01-01T00:00:00Z
     * @return the MM/dd/YYYY HH:mm:ss format as a string
     */
    public static String convertDateTimeString(String date){
        if (date != null) {
            long milis = Long.parseLong(date) * 1000;

            Date d = new Date(milis);
            DateFormat formatter = new SimpleDateFormat("MM/dd/YYYY HH:mm:ss");

            return formatter.format(d);
        }
        return null;
    }

    /**
     * Given a size in bytes, calculate the size in megabytes
     * @param size the string number in bytes
     * @return the string number in megabytes
     */
    public static String convertMegabytesString(String size){
        if (size != null) {
            double bytes = Double.parseDouble(size);
            double megabytes = bytes / 1000000;
            return megabytes + " MB";
        }
        return null;
    }
}
