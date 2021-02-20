package com.example.musicplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.drawable.DrawableCompat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SongListAdapter extends ArrayAdapter {

    private Activity mActivity;
    private Context mContext;
    private int mResource;
    private HashSet<ViewHolder> mItems;

    /**
     * Holds variables about an item (song) in a View
     */
    class ViewHolder {
        TextView title;
        TextView artist;
        TextView album;
        ImageView albumArt;
        ImageView innerFrame;
        ImageView outerFrame;
    }

    public SongListAdapter(Context context, int resource, ArrayList<Song> objects, Activity activity) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
        mActivity = activity;
        mItems = new HashSet<>();
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
                    Bitmap albumArt = getAlbumArt(albumID);

                    // asynchronously find the views and hold them for immediate access later
                    item.title = cv.findViewById(R.id.textView1);
                    item.artist = cv.findViewById(R.id.textView2);
                    item.album = cv.findViewById(R.id.textView3);
                    item.albumArt = cv.findViewById(R.id.album_art);
                    item.innerFrame = cv.findViewById(R.id.albumart_innerframe);
                    item.outerFrame = cv.findViewById(R.id.albumart_outerframe);
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
                    Bitmap albumArt = getAlbumArt(albumID);
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
                            int defaultImage = mContext.getResources().getIdentifier("@drawable/default_image", null, mContext.getPackageName());
                            item.albumArt.setImageResource(defaultImage);
                        }
                        // put item in arraylist if it doesn't exist already and set the appropriate colors
                        if (!mItems.contains(item)) {
                            mItems.add(item);
                            if (MainActivity.nightMode){
                                setItemsFrameColor(mContext.getResources().getColor(R.color.nightPrimaryDark));
                                setItemsTitleTextColor(mContext.getResources().getColorStateList(R.color.itemnightselectorblue));
                            }
                            else{
                                setItemsFrameColor(mContext.getResources().getColor(R.color.lightPrimaryWhite));
                                setItemsTitleTextColor(mContext.getResources().getColorStateList(R.color.itemlightselectorblue));
                            }
                        }
                    }
                });
            }
        });

        return convertView;
    }

    public Bitmap getAlbumArt(String albumId){
        long albumId_long = Long.parseLong(albumId);
        Bitmap albumArt = null;
        // get album art for song
        Uri albumArtURI = ContentUris.withAppendedId(MusicPlayerService.artURI, albumId_long);
        ContentResolver res = mContext.getContentResolver();
        try {
            InputStream in = res.openInputStream(albumArtURI);
            albumArt = BitmapFactory.decodeStream(in);
            if (in != null) {
                in.close();
            }
        }catch(Exception e){
            System.out.println("Album Art cannot be found by adapter");
        }

        return albumArt;
    }

    /**
     * sets the color of the title of every item in the list view
     * @param code the color resource code to set the title
     */
    public void setItemsTitleTextColor(ColorStateList code){
        for (ViewHolder item : mItems){
            item.title.setTextColor(code);
        }
    }

    /**
     * sets the color of the title of every item in the list view
     * @param code the color resource code to set the title
     */
    public void setItemsFrameColor(int code){
        for (ViewHolder item : mItems){
            Drawable unwrappedInnerFrame = item.innerFrame.getDrawable();
            Drawable wrappedInnerFrame = DrawableCompat.wrap(unwrappedInnerFrame);
            DrawableCompat.setTint(wrappedInnerFrame, code);

            Drawable unwrappedOuterFrame = item.outerFrame.getDrawable();
            Drawable wrappedOuterFrame = DrawableCompat.wrap(unwrappedOuterFrame);
            DrawableCompat.setTint(wrappedOuterFrame, code);
        }

    }

}
