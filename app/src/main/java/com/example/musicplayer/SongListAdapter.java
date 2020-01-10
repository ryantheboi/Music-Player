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

        LayoutInflater inflater = LayoutInflater.from(mContext);
        convertView = inflater.inflate(mResource, parent, false);

        TextView tvTitle = convertView.findViewById(R.id.textView1);
        TextView tvArtist = convertView.findViewById(R.id.textView2);
        TextView tvAlbum = convertView.findViewById(R.id.textView3);

        tvTitle.setText(title);
        tvArtist.setText(artist);
        tvAlbum.setText(album);

        return convertView;
    }
}
