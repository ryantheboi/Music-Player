package com.example.musicplayer;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.graphics.Typeface;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MusicDetailsFragment extends Fragment {

    private static final String SONG_TAG = "Song";
    private static final String SONGMETADATA_TAG = "SongMetadata";

    private Song song;
    private SongMetadata songMetadata;
    private int idx;
    private TextView heading;
    private TextView msgWindow;
    private ImageView details_background;
    private ImageView exit_background;
    private SpannableStringBuilder details = new SpannableStringBuilder();
    private MainActivity mainActivity;

    public MusicDetailsFragment() {
        super(R.layout.activity_details);
    }

    public static MusicDetailsFragment getInstance(Song song, SongMetadata songMetadata) {
        MusicDetailsFragment fragment = new MusicDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(SONG_TAG, song);
        bundle.putParcelable(SONGMETADATA_TAG, songMetadata);
        fragment.setArguments(bundle);
        return fragment;
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.song = getArguments().getParcelable(SONG_TAG);
            this.songMetadata = getArguments().getParcelable(SONGMETADATA_TAG);
        }
        this.mainActivity = (MainActivity) getActivity();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        initViews(view);
        initBackgroundClickListener();
        initObjects();
        setThemeColors();
    }

    @Override
    public void onDetach() {
        mainActivity.setIsInfoDisplaying(false);
        mainActivity.setSlidingUpPanelTouchEnabled(true);
        super.onDetach();
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

    private void initViews(View view) {
        heading = view.findViewById(R.id.music_details_heading);
        msgWindow = view.findViewById(R.id.music_details);
        details_background = view.findViewById(R.id.music_details_background);
        exit_background = view.findViewById(R.id.details_exit_background);
    }

    private void initBackgroundClickListener(){
        // set background image to finish activity on click

        exit_background.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.getSupportFragmentManager().popBackStackImmediate();
                onDetach();
            }
        });
    }

    private void initObjects() {
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

    private void setThemeColors() {
        // set theme colors
        details_background.setBackgroundColor(ThemeColors.getColor(ThemeColors.COLOR_SECONDARY));
        heading.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
        msgWindow.setTextColor(ThemeColors.getColor(ThemeColors.TITLE_TEXT_COLOR));
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
}
