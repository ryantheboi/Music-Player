package com.example.musicplayer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MusicDetailsActivity extends Activity {

    private Song song;
    private SongMetadata songMetadata;
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
                switch (key) {
                    case "currentSong":
                        // get the song which needs its details to be displayed
                        song = intent.getParcelableExtra("currentSong");
                        break;
                    case "currentSongMetadata":
                        // get the song which needs its details to be displayed
                        songMetadata = intent.getParcelableExtra("currentSongMetadata");
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

        // set theme colors
        details_background.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_SECONDARY));
        heading.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        msgWindow.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));

        // init mediaextractor to obtain further media details from song
        MediaExtractor mex = new MediaExtractor();
        try {
            mex.setDataSource(song.getDataPath());
        } catch (IOException e) {
            System.out.println("MediaExtractor not available for this song");
        }
        MediaFormat mf = mex.getTrackFormat(0);

        appendDetail("Title", song.getTitle());
        appendDetail("Artist", song.getArtist());
        appendDetail("Album", song.getAlbum());
        appendDetail("Times Played", Integer.toString(songMetadata.getPlayed()));
        appendDetail("Times Listened (30s)", Integer.toString(songMetadata.getListened()));
        appendDetail("Date Listened", convertDateTimeString(songMetadata.getDateListened()));
        appendDetail("Bitrate", convertBitrate(mf.getInteger(MediaFormat.KEY_BIT_RATE)), "kb/s");
        appendDetail("Sample Rate", String.valueOf(mf.getInteger(MediaFormat.KEY_SAMPLE_RATE)), "Hz");
        appendDetail("Channel Count", String.valueOf(mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT)));
        appendDetail("MIME Type", song.getMimeType());
        appendDetail("Data Path", song.getDataPath());
        appendDetail("Size", convertMegabytesString(song.getSize()), "MB");
        appendDetail("Duration", SongHelper.convertTime(song.getDuration()));
        appendDetail("Date Added", convertDateTimeString(song.getDateAdded()));
        appendDetail("Date Modified", convertDateTimeString(song.getDateModified()));
        appendDetail("ID", Integer.toString(song.getId()));
        appendDetail("Album ID", (song.getAlbumID()));
        appendDetail("Relative Path", song.getRelativePath());
        appendDetail("Display Name", song.getDisplayName());
        appendDetail("Bucket Display Name", song.getBucketDisplayName());
        appendDetail("Bucket ID", song.getBucketID());
        appendDetail("Document ID", song.getDocumentID());
        appendDetail("Original Document ID", song.getOriginalDocumentID());
        appendDetail("Instance ID", song.getInstanceID());
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
            detail = "N/A";
        }
        details.append(label + ": " + detail + "\n\n");
        int label_length = label.length() + 2; // 2 for the ": "
        details.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), idx, idx + label_length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        details.setSpan(new RelativeSizeSpan(1.2f), idx, idx + label_length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        int total_length = label_length + detail.length() + 2; // 2 for the "\n\n"
        idx += total_length;
    }

    /**
     * Overloaded appendDetail method to allow for units to be concatenated to a detail
     * @param label The label of the detail, e.g. Title, Artist, Album, etc.
     * @param detail The description of the label
     * @param unit The unit of measurement of the label, e.g. Hz, MB
     */
    private void appendDetail(String label, String detail, String unit){
        if (detail == null) {
            detail = "N/A";
            details.append(label).append(": ").append(detail).append("\n\n");
        }
        else{
            details.append(label).append(": ").append(detail).append(" ").append(unit).append("\n\n");
        }
        int label_length = label.length() + 2; // 2 for the ": "
        details.setSpan(new android.text.style.StyleSpan(Typeface.BOLD), idx, idx + label_length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        details.setSpan(new RelativeSizeSpan(1.2f), idx, idx + label_length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        int total_length = label_length + detail.length() + unit.length() + 3; // 3 for the " [unit]\n\n"
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
     * @return the string number in megabytes, or null
     */
    public static String convertMegabytesString(String size){
        if (size != null) {
            double bytes = Double.parseDouble(size);
            double megabytes = bytes / 1000000;
            return String.valueOf(megabytes);
        }
        return null;
    }

    /**
     * Given a bitrate in bits per second, calculate the kb per second
     * @param bitrate the int bitrate in bits per second
     * @return the string bitrate in kb/s, or null
     */
    public static String convertBitrate(int bitrate){
        int kbps = bitrate / 1000;
        return String.valueOf(kbps);
    }
}
