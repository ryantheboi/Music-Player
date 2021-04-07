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

public class PlaylistAdapter extends ArrayAdapter {

    private Activity mActivity;
    private Context mContext;
    private int mResource;
    private HashSet<ViewHolder> items;

    /**
     * Holds variables about an item (playlist) in a View
     */
    class ViewHolder {
        TextView name;
        TextView size;
        ImageView albumArt;
        ImageView innerFrame;
        ImageView outerFrame;
    }

    public PlaylistAdapter(Context context, int resource, ArrayList<Playlist> objects, Activity activity) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
        mActivity = activity;
        items = new HashSet<>();
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
                    Object[] arr = new Object[4];

                    // get playlist info
                    Playlist playlist = (Playlist) getItem(position);
                    String name = playlist.getName();
                    String size = playlist.getSizeString();
                    Bitmap albumArt;
                    if (!size.equals("0")) {
                        String albumID = playlist.getSongList().get(0).getAlbumID();
                        albumArt = getAlbumArt(albumID);
                    }
                    else{
                        albumArt = null;
                    }

                    // asynchronously find the views and hold them for immediate access later
                    item.name = cv.findViewById(R.id.textView1);
                    item.size = cv.findViewById(R.id.textView2);
                    item.albumArt = cv.findViewById(R.id.album_art);
                    item.innerFrame = cv.findViewById(R.id.albumart_innerframe);
                    item.outerFrame = cv.findViewById(R.id.albumart_outerframe);
                    cv.setTag(item);

                    arr[0] = name;
                    arr[1] = size;
                    arr[2] = albumArt;
                    arr[3] = item;
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
                    Object[] arr = new Object[4];

                    // get playlist info
                    Playlist playlist = (Playlist) getItem(position);
                    String name = playlist.getName();
                    String size = playlist.getSizeString();
                    Bitmap albumArt;
                    if (!size.equals("0")) {
                        String albumID = playlist.getSongList().get(0).getAlbumID();
                        albumArt = getAlbumArt(albumID);
                    }
                    else{
                        albumArt = null;
                    }
                    ViewHolder item = (ViewHolder) cv.getTag();

                    arr[0] = name;
                    arr[1] = size;
                    arr[2] = albumArt;
                    arr[3] = item;
                    return arr;
                }
            });
        }
        // perform after the asynchronous operation is complete
        cf.thenAccept(new Consumer<Object[]>() {
            @Override
            public void accept(Object[] arr) {
                final String name = (String) arr[0];
                final String size = ((String) arr[1]) + " songs";
                final Bitmap albumArt = (Bitmap) arr[2];
                final ViewHolder item = (ViewHolder) arr[3];

                // set resources for the view, only through the ui (main) thread
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        item.name.setText(name);
                        item.size.setText(size);
                        item.albumArt.setImageBitmap(albumArt);
                        if (albumArt == null){
                            int defaultImage = mContext.getResources().getIdentifier("@drawable/default_image", null, mContext.getPackageName());
                            item.albumArt.setImageResource(defaultImage);
                        }
                        // put item in arraylist if it doesn't exist already and set the appropriate colors
                        if (!items.contains(item)) {
                            items.add(item);
                            setItemsFrameColor(ThemeColors.getColor(ThemeColors.COLOR_PRIMARY));
                            setItemsTitleTextColor(mContext.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.ITEM_TEXT_COLOR)));
                            setItemsSizeTextColor(mContext.getResources().getColorStateList(ThemeColors.getColor(ThemeColors.SUBTITLE_TEXT_COLOR)));
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
     * @param code the color resource code to set the title textview
     */
    public void setItemsTitleTextColor(ColorStateList code){
        for (ViewHolder item : items){
            item.name.setTextColor(code);
        }
    }

    /**
     * sets the color of the size of every item in the list view
     * @param code the color resource code to set the size textview
     */
    public void setItemsSizeTextColor(ColorStateList code){
        for (ViewHolder item : items){
            item.size.setTextColor(code);
        }
    }

    /**
     * sets the color of the albumart frame of every item in the list view
     * @param code the color resource code to set the frame
     */
    public void setItemsFrameColor(int code){
        for (ViewHolder item : items){
            Drawable unwrappedInnerFrame = item.innerFrame.getDrawable();
            Drawable wrappedInnerFrame = DrawableCompat.wrap(unwrappedInnerFrame);
            DrawableCompat.setTint(wrappedInnerFrame, code);

            Drawable unwrappedOuterFrame = item.outerFrame.getDrawable();
            Drawable wrappedOuterFrame = DrawableCompat.wrap(unwrappedOuterFrame);
            DrawableCompat.setTint(wrappedOuterFrame, code);
        }

    }

}
