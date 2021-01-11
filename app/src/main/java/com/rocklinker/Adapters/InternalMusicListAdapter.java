package com.rocklinker.Adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.rocklinker.R;
import com.rocklinker.Services.PlayerService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import pl.droidsonroids.gif.GifImageView;

public class InternalMusicListAdapter extends RecyclerView.Adapter <InternalMusicListAdapter.ViewHolder> {

    private final List<File> files;
    private List<File> filteredFiles;
    private final Activity activity;
    private final com.rocklinker.Common.Handler Handler = new com.rocklinker.Common.Handler();
    private final int R_ID;
    private boolean isBindViewHolderError;

    public InternalMusicListAdapter(List<File> files, Activity activity, int R_ID) {
        this.files = files;
        this.filteredFiles = files;
        this.activity = activity;
        this.R_ID = R_ID;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int ViewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_internal_music_list,parent,false);
        return new ViewHolder(view);
    }

    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        try {
            GetMusicInfo(
                    filteredFiles.get(position).getAbsolutePath(),
                    viewHolder.imageViewArt,
                    viewHolder.textViewArtistName,
                    viewHolder.textViewMusicName
            );

            String fileName = filteredFiles.get(position).getName();
            if (PlayerService.getFileName().equals(fileName)){
                viewHolder.buttonPlaying.setVisibility(View.VISIBLE);
                if (!PlayerService.isPlaying()) {
                    viewHolder.buttonPlaying.setBackground(ResourcesCompat.getDrawable(
                            activity.getResources(),R.drawable.ic_pause_24,activity.getTheme()));
                    viewHolder.gifImageView.setVisibility(View.INVISIBLE);
                } else {
                    viewHolder.buttonPlaying.setBackground(ResourcesCompat.getDrawable(
                            activity.getResources(),R.drawable.ic_play_arrow_24,activity.getTheme()));
                    viewHolder.gifImageView.setVisibility(View.VISIBLE);
                }
            } else {
                viewHolder.buttonPlaying.setVisibility(View.INVISIBLE);
                viewHolder.gifImageView.setVisibility(View.INVISIBLE);
            }

        }catch (Exception e){
            if(!isBindViewHolderError) {
                Handler.ShowSnack(
                        "Houve um erro",
                        "InternalMusicListAdapter.onBindViewHolder: " + e.getMessage(),
                        activity,
                        R_ID
                );
                isBindViewHolderError=true;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ImageView imageViewArt;
        Button buttonPlaying;
        TextView textViewArtistName, textViewMusicName;
        GifImageView gifImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            imageViewArt = itemView.findViewById(R.id.itemInternalMusicList_ImageView_Art);
            textViewArtistName = itemView.findViewById(R.id.itemInternalMusicList_TextView_ArtistName);
            textViewMusicName = itemView.findViewById(R.id.itemInternalMusicList_TextView_MusicName);
            buttonPlaying = itemView.findViewById(R.id.itemInternalMusicList_Button_Playing);
            gifImageView = itemView.findViewById(R.id.itemInternalMusicList_ImageView_Gif);
        }
    }

    public int getItemCount() {
        return filteredFiles.size();
    }

    public File getFile(int position){
        return filteredFiles.get(position);
    }

    public List<File> getFiles(){
        return files;
    }

    public void setShuffle(boolean isShuffle){
        if(isShuffle){
            Collections.shuffle(filteredFiles);
        }else{
            Collections.sort(filteredFiles);
        }
    }

    public int getFilePosition(String fileName){
        for (int i = 0; i < filteredFiles.size(); i++) {
            if(fileName.equals(filteredFiles.get(i).getName())) return i;
        }
        return -1;
    }

    public Filter getFilter()
    {
        return new Filter()
        {
            protected FilterResults performFiltering(CharSequence charSequence)
            {
                FilterResults results = new FilterResults();

                if(charSequence == null || charSequence.length() == 0){
                    results.values = files;
                    results.count = files.size();
                }else{

                    List<File> newFiles = new ArrayList<>();

                    int i = 0;
                    try {

                        while (i < files.size()) {
                            String A = files.get(i).getName().toLowerCase();
                            String B = charSequence.toString().toLowerCase();
                            if (A.contains(B)) {
                                newFiles.add(files.get(i));
                            }
                            i++;
                        }

                        results.values = newFiles;
                        results.count = newFiles.size();

                    }catch (Exception e){
                        //Handler.ShowSnack("Houve um erro",
                        // "MusicListAdapter.getFilter: " + e.getMessage(), activity, R_ID, true);
                    }
                }

                return results;
            }

            protected void publishResults(CharSequence charSequence, FilterResults filterResults)
            {
                filteredFiles = (List<File>) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    private void GetMusicInfo(String source, ImageView imageViewArt, TextView textViewArtistName, TextView textViewMusicName){
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(source);

        String path = Objects.requireNonNull(activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getPath();

        try {
            byte[] art = mediaMetadataRetriever.getEmbeddedPicture();
            assert art != null;
            Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
            imageViewArt.setImageBitmap(songImage);

            textViewArtistName.setText(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            textViewMusicName.setText(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        } catch (Exception e) {
            imageViewArt.setImageBitmap(null);
            String unknown = "Arquivo sem informações";
            String filename = source.replace(path,"").replace("/","").replace(".mp3","");
            textViewArtistName.setText(filename);
            textViewMusicName.setText(unknown);
        }
    }

}
