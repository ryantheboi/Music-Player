package com.example.musicplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.InputStream;
import java.util.ArrayList;

public class SongListAdapter extends ArrayAdapter {

    private Context mContext;
    private int mResource;
    private SparseArray<ViewItem> items;
    private ViewItem selectedItem;

    /**
     * Holds variables about an item (song) in a View
     */
    static class ViewItem{
        int position;
        TextView title;
        TextView artist;
        TextView album;
        ImageView albumArt;
    }

    public SongListAdapter(Context context, int resource, ArrayList<Song> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
        items = new SparseArray<>();
    }

    @Override
    public View getView(int position,  View convertView, ViewGroup parent) {

        // get song info and create Song object
        String title = ((Song) getItem(position)).getTitle();
        String artist = ((Song) getItem(position)).getArtist();
        String album = ((Song) getItem(position)).getAlbum();
        String albumID = ( (Song) getItem(position)).getAlbumID();

        Bitmap albumArt = getAlbumArt(albumID);
        ViewItem item;

        if (convertView == null){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);

            item = new ViewItem();
            item.position = position;
            item.title = convertView.findViewById(R.id.textView1);
            item.artist = convertView.findViewById(R.id.textView2);
            item.album = convertView.findViewById(R.id.textView3);
            item.albumArt = convertView.findViewById(R.id.album_art);
            convertView.setTag(item);
        }
        else{
            item = (ViewItem) convertView.getTag();
        }

        // set resources for the view
        if (title.length() > 35){
            title = title.substring(0,35) + "...";
        }
        if (artist.length() > 35){
            artist = artist.substring(0,35) + "...";
        }
        if (album.length() > 35){
            album = album.substring(0,35) + "...";
        }
        item.title.setText(title);
        item.artist.setText(artist);
        item.album.setText(album);
        item.albumArt.setImageBitmap(albumArt);
        if (albumArt == null){
            int defaultImage = mContext.getResources().getIdentifier("@drawable/default_image", null, mContext.getPackageName());
            item.albumArt.setImageResource(defaultImage);
        }

        // put item in sparse array with key: position, value: item
        items.put(position, item);
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
            e.printStackTrace();
        }

        return albumArt;
    }

    /**
     * sets the color of the title of every item, excluding the selected item, in the list view
     * @param code the color resource code to set the title
     */
    public void setItemsTitleTextColor(int code){
        int size = items.size();
        if (selectedItem != null) {
            for (int i = 0; i < size; i++) {
                ViewItem item = items.valueAt(i);
                if (item.position != selectedItem.position) {
                    item.title.setTextColor(code);
                }
            }
        }
        else {
            for (int i = 0; i < size; i++) {
                ViewItem item = items.valueAt(i);
                item.title.setTextColor(code);
            }
        }
    }

    /**
     * highlights an item selected from the list view, given a position and html color code
     * @param position the position of the selected item from the list view
     */
    public void highlightItem(int position){
        // un-highlight the previously selected item
        if (selectedItem != null){
            if (MusicListActivity.nightMode){
                selectedItem.title.setTextColor(mContext.getResources().getColor(R.color.lightPrimaryWhite));
            }
            else {
                selectedItem.title.setTextColor(mContext.getResources().getColor(R.color.colorTextDark));
            }
            selectedItem.artist.setTextColor(mContext.getResources().getColor(R.color.colorTextGrey));
            selectedItem.album.setTextColor(mContext.getResources().getColor(R.color.colorTextGrey));
        }

        // highlight new selected item with blue
        selectedItem = items.get(position);
        selectedItem.title.setTextColor(mContext.getResources().getColor(R.color.colorTextBlue));
        selectedItem.artist.setTextColor(mContext.getResources().getColor(R.color.colorTextBlue));
        selectedItem.album.setTextColor(mContext.getResources().getColor(R.color.colorTextBlue));

    }

}
