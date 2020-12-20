package com.example.musicplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class SongListAdapter extends ArrayAdapter {

    private Context mContext;
    private int mResource;
    private HashMap<Song, ViewItem> items;
    private ViewItem selectedItem;

    /**
     * Holds variables about an item (song) in a View
     */
    class ViewItem{
        TextView title;
        TextView artist;
        TextView album;
        ImageView albumArt;
    }

    public SongListAdapter(Context context, int resource, ArrayList<Song> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
        items = new HashMap<>();
    }

    @Override
    public View getView(int position,  View convertView, ViewGroup parent) {
        // get song info and create Song object
        Song song = (Song) getItem(position);
        String title = song.getTitle();
        String artist = song.getArtist();
        String album = song.getAlbum();
        String albumID = song.getAlbumID();
        Bitmap albumArt = getAlbumArt(albumID);
        ViewItem item;

        if (convertView == null){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);
            item = new ViewItem();
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

        // put item in hashmap with key: song, value: item
        items.put(song, item);
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
     * sets the color of the title of every item in the list view
     * @param code the color resource code to set the title
     */
    public void setItemsTitleTextColor(ColorStateList code){
        for (Song song : items.keySet()){
            ViewItem item = items.get(song);
            item.title.setTextColor(code);
        }
    }

    /**
     * highlights item selected from the list view to blue and unhighlights any previously selected
     * @param song the song object of the selected item from the list view
     */
    public void highlightItem(Song song){
//        // un-highlight the previously selected item
//        if (selectedItem != null){
//            if (MusicListActivity.nightMode){
//                selectedItem.title.setTextColor(mContext.getResources().getColor(R.color.lightPrimaryWhite));
//            }
//            else {
//                selectedItem.title.setTextColor(mContext.getResources().getColor(R.color.colorTextDark));
//            }
//            selectedItem.artist.setTextColor(mContext.getResources().getColor(R.color.colorTextGrey));
//            selectedItem.album.setTextColor(mContext.getResources().getColor(R.color.colorTextGrey));
//        }
//
//        // highlight new selected item with blue
//        selectedItem = items.get(song);
//        selectedItem.title.setTextColor(mContext.getResources().getColor(R.color.colorTextBlue));
//        selectedItem.artist.setTextColor(mContext.getResources().getColor(R.color.colorTextBlue));
//        selectedItem.album.setTextColor(mContext.getResources().getColor(R.color.colorTextBlue));
    }

}
