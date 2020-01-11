package com.example.musicplayer;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class SongListAdapter extends ArrayAdapter {

    private Context mContext;
    private int mResource;

    /**
     * Holds variables in a View
     */
    static class ViewHolder{
        TextView title;
        TextView artist;
        TextView album;
        ImageView albumArt;
    }

    public SongListAdapter(Context context, int resource, ArrayList<Song> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @Override
    public View getView(int position,  View convertView, ViewGroup parent) {

        // get song info and create Song object
        String title = ((Song) getItem(position)).getTitle();
        String artist = ((Song) getItem(position)).getArtist();
        String album = ((Song) getItem(position)).getAlbum();
        Bitmap albumArt = ((Song) getItem(position)).getAlbumArt();

        ViewHolder holder;

        if (convertView == null){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);

            holder = new ViewHolder();
            holder.title = convertView.findViewById(R.id.textView1);
            holder.artist = convertView.findViewById(R.id.textView2);
            holder.album = convertView.findViewById(R.id.textView3);
            holder.albumArt = convertView.findViewById(R.id.album_art);
            convertView.setTag(holder);
        }
        else{
            holder = (ViewHolder) convertView.getTag();
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
        holder.title.setText(title);
        holder.artist.setText(artist);
        holder.album.setText(album);
        holder.albumArt.setImageBitmap(albumArt);
        if (albumArt == null){
            int defaultImage = mContext.getResources().getIdentifier("@drawable/default_image", null, mContext.getPackageName());
            holder.albumArt.setImageResource(defaultImage);
        }

        return convertView;
    }
}
