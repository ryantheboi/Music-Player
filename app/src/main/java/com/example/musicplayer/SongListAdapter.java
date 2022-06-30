package com.example.musicplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SongListAdapter extends ArrayAdapter implements SectionIndexer {

    private Activity mActivity;
    private Context mContext;
    private int mResource;
    private HashSet<ViewHolder> mItems;
    private HashMap<String, Integer> alphabetIndex;
    private String[] sections;

    /**
     * Holds variables about an item (song) in a View
     */
    class ViewHolder {
        TextView title;
        TextView artist;
        TextView album;
        ImageView albumArt;
    }

    public SongListAdapter(Context context, int resource, ArrayList<Song> objects, Activity activity) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
        mActivity = activity;
        mItems = new HashSet<>();

        alphabetIndex = new HashMap<>();
        int size = objects.size();

        for (int i = 0; i < size; i++) {
            // get the first letter of the song title
            String titleChar = objects.get(i).getTitle().substring(0, 1);

            // convert to uppercase otherwise lowercase a-z will be sorted after upper A-Z
            titleChar = titleChar.toUpperCase();

            // put only if the key does not exist
            if (!alphabetIndex.containsKey(titleChar)){
                alphabetIndex.put(titleChar, i);
            }
        }

        // create a list from the set to sort and populate sections
        ArrayList<String> sectionList = new ArrayList<>(alphabetIndex.keySet());
        Collections.sort(sectionList);
        sections = new String[sectionList.size()];
        sectionList.toArray(sections);
    }

    @Override
    @TargetApi(24)
    public View getView(final int position,  View convertView, final ViewGroup parent) {
        CompletableFuture<Object[]> cf;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);
            final View cv = convertView;
            final ViewHolder item = new ViewHolder();

            // perform asynchronously to return an array of objects
            cf = CompletableFuture.supplyAsync(new Supplier<Object[]>() {
                @Override
                public Object[] get() {
                    Object[] arr = new Object[5];

                    // get song info and create Song object
                    Song song = (Song) getItem(position);
                    String title = song.getTitle();
                    String artist = song.getArtist();
                    String album = song.getAlbum();
                    String albumID = song.getAlbumID();
                    Bitmap albumArt = Song.getAlbumArtBitmap(mContext, albumID);

                    // asynchronously find the views and hold them for immediate access later
                    item.title = cv.findViewById(R.id.textView1);
                    item.artist = cv.findViewById(R.id.textView2);
                    item.album = cv.findViewById(R.id.textView3);
                    item.albumArt = cv.findViewById(R.id.album_art);
                    cv.setTag(item);

                    arr[0] = title;
                    arr[1] = artist;
                    arr[2] = album;
                    arr[3] = albumArt;
                    arr[4] = item;
                    return arr;
                }
            });
        }
        else{
            final View cv = convertView;

            // perform asynchronously to return an array of objects
            cf = CompletableFuture.supplyAsync(new Supplier<Object[]>() {
                @Override
                public Object[] get() {
                    Object[] arr = new Object[5];

                    // get song info and create Song object
                    Song song = (Song) getItem(position);
                    String title = song.getTitle();
                    String artist = song.getArtist();
                    String album = song.getAlbum();
                    String albumID = song.getAlbumID();
                    Bitmap albumArt = Song.getAlbumArtBitmap(mContext, albumID);
                    ViewHolder item = (ViewHolder) cv.getTag();

                    arr[0] = title;
                    arr[1] = artist;
                    arr[2] = album;
                    arr[3] = albumArt;
                    arr[4] = item;
                    return arr;
                }
            });
        }
        // perform after the asynchronous operation is complete
        cf.thenAccept(new Consumer<Object[]>() {
            @Override
            public void accept(Object[] arr) {
                final String title = (String) arr[0];
                final String artist = (String) arr[1];
                final String album = (String) arr[2];
                final Bitmap albumArt = (Bitmap) arr[3];
                final ViewHolder item = (ViewHolder) arr[4];

                // set resources for the view, only through the ui (main) thread
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        item.title.setText(title);
                        item.artist.setText(artist);
                        item.album.setText(album);
                        item.albumArt.setImageBitmap(albumArt);
                        if (albumArt == null){
                            int defaultImage = mContext.getResources().getIdentifier("@drawable/default_albumart", null, mContext.getPackageName());
                            item.albumArt.setImageResource(defaultImage);
                        }
                        // put item in arraylist if it doesn't exist already and set the appropriate colors
                        if (!mItems.contains(item)) {
                            mItems.add(item);
                            setItemsTitleTextColor(mContext.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.ITEM_TEXT_COLOR)));
                            setItemsAlbumArtistTextColor(mContext.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
                        }
                    }
                });
            }
        });

        return convertView;
    }

    /**
     * sets the color of the title of every item in the list view
     * @param code the color resource code to set the title textview
     */
    public void setItemsTitleTextColor(ColorStateList code){
        for (ViewHolder item : mItems){
            item.title.setTextColor(code);
        }
    }

    /**
     * sets the color of the album and artist of every item in the list view
     * @param code the color resource code to set the album and artist textviews
     */
    public void setItemsAlbumArtistTextColor(ColorStateList code){
        for (ViewHolder item : mItems){
            item.album.setTextColor(code);
            item.artist.setTextColor(code);
        }
    }

    @Override
    public int getPositionForSection(int section) {
        return alphabetIndex.get(sections[section]);
    }

    @Override
    public int getSectionForPosition(int position) {
        return 0;
    }

    @Override
    public Object[] getSections() {
        return sections;
    }
}
