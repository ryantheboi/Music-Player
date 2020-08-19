package com.example.musicplayer;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;

public class SongListAdapter extends ArrayAdapter {

    private Context mContext;
    private int mResource;
    private ArrayList<ViewItem> items;

    /**
     * Holds variables in a View
     */
    static class ViewItem{
        TextView title;
        TextView artist;
        TextView album;
        ImageView albumArt;
    }

    public SongListAdapter(Context context, int resource, ArrayList<Song> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
        items = new ArrayList<>();
    }

    @Override
    public View getView(int position,  View convertView, ViewGroup parent) {

        // get song info and create Song object
        String title = ((Song) getItem(position)).getTitle();
        String artist = ((Song) getItem(position)).getArtist();
        String album = ((Song) getItem(position)).getAlbum();
        int albumID = ( (Song) getItem(position)).getAlbumID();

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

        items.add(item);
        return convertView;
    }

    public Bitmap getAlbumArt(int albumId){
        Bitmap albumArt = null;
        // get album art for song
        Uri artURI = Uri.parse("content://media/external/audio/albumart");
        Uri albumArtURI = ContentUris.withAppendedId(artURI, albumId);
        ContentResolver res = mContext.getContentResolver();
        try {
            InputStream in = res.openInputStream(albumArtURI);
            albumArt = BitmapFactory.decodeStream(in);
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }

        return albumArt;
    }

    public void setItemTitleTextColor(String code){
        for (ViewItem item : items){
            item.title.setTextColor(Color.parseColor(code));
        }
    }
}
