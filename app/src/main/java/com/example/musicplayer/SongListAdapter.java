package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
        int albumID = ((Song) getItem(position)).getAlbumID();
        Song song = new Song(title, artist, album, albumID);

        ViewHolder holder;

        if (convertView == null){
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent, false);

            holder = new ViewHolder();
            holder.title = convertView.findViewById(R.id.textView1);
            holder.artist = convertView.findViewById(R.id.textView2);
            holder.album = convertView.findViewById(R.id.textView3);

            convertView.setTag(holder);
        }
        else{
            holder = (ViewHolder) convertView.getTag();
        }

        holder.title.setText(song.getTitle());
        holder.artist.setText(song.getArtist());
        holder.album.setText(song.getAlbum());

        return convertView;
    }
}
