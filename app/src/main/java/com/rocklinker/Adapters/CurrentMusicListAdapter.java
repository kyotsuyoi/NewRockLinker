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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.rocklinker.Common.ApiClient;
import com.rocklinker.Common.PlayerInterface;
import com.rocklinker.R;
import com.rocklinker.Services.PlayerService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import pl.droidsonroids.gif.GifImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CurrentMusicListAdapter extends RecyclerView.Adapter <CurrentMusicListAdapter.ViewHolder> {

    private JsonArray jsonArray;
    private JsonArray filteredJsonArray;
    private final Activity activity;
    private final com.rocklinker.Common.Handler Handler = new com.rocklinker.Common.Handler();
    private final PlayerInterface musicListInterface = ApiClient.getApiClient().create(PlayerInterface.class);
    private final int R_ID;
    private boolean isBindViewHolderError;

    public CurrentMusicListAdapter(JsonArray jsonArray, Activity activity, int R_ID) {
        this.jsonArray = jsonArray;
        this.filteredJsonArray = jsonArray;
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

            viewHolder.buttonPlaying.setVisibility(View.INVISIBLE);
            viewHolder.gifImageView.setVisibility(View.INVISIBLE);

            GetMusicInfo(
                    viewHolder.imageViewArt,
                    viewHolder.textViewArtistName,
                    viewHolder.textViewMusicName,
                    position
            );

            String fileName = filteredJsonArray.get(position).getAsJsonObject().get("filename").getAsString();

            if(PlayerService.getFileInformation() == null){
                return;
            }

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
            }

        }catch (Exception e){
            if(!isBindViewHolderError) {
                Handler.ShowSnack(
                        "Houve um erro",
                        "CurrentMusicListAdapter.onBindViewHolder: " + e.getMessage(),
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
        return filteredJsonArray.size();
    }

    public JsonObject getItem(int position){
        return filteredJsonArray.get(position).getAsJsonObject();
    }

    public JsonArray getItems(){
        return jsonArray;
    }

    private void GetMusicInfo(ImageView imageViewArt, TextView textViewArtistName, TextView textViewMusicName, int position){

        String fileName = filteredJsonArray.get(position).getAsJsonObject().get("filename").getAsString();
        String artist = filteredJsonArray.get(position).getAsJsonObject().get("artist").getAsString();
        String title = filteredJsonArray.get(position).getAsJsonObject().get("title").getAsString();

        String path = activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getPath();
        File file = new File(path,fileName);

        if(!file.exists()){
            imageViewArt.setImageBitmap(null);
            textViewArtistName.setText(artist);
            textViewMusicName.setText(title);

            if(filteredJsonArray.get(position).getAsJsonObject().has("art")
                    && filteredJsonArray.get(position).getAsJsonObject().get("art") != null) {
                String art = filteredJsonArray.get(position).getAsJsonObject().get("art").getAsString();
                imageViewArt.setImageBitmap(Handler.ImageDecode(art));
                return;
            }

            getMusicArt(imageViewArt, fileName, position);
            return;
        }

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(fileName);

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
            String filename = fileName.replace("/","").replace(".mp3","");
            textViewArtistName.setText(filename);
            textViewMusicName.setText(unknown);
        }
    }

    private void getMusicArt(ImageView imageView, String filename, int position){
        try {

            Call<JsonObject> call = musicListInterface.GetFullMusicArt(filename, true);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    try {
                        if (!Handler.isRequestError(response, activity, R_ID)){
                            JsonObject jsonObject = response.body();
                            assert jsonObject != null;
                            JsonArray jsonArray = jsonObject.get("data").getAsJsonArray();

                            String art = jsonArray.get(0).getAsJsonObject().get("art").getAsString();
                            imageView.setImageBitmap(Handler.ImageDecode(art));

                            filteredJsonArray.get(position).getAsJsonObject().addProperty("art", art);
                        }else{
                            imageView.setImageBitmap(null);
                        }
                    }catch (Exception e){
                        Handler.ShowSnack("Houve um erro","ExternalMusicListAdapter.GetMusicArt.onResponse: " + e.getMessage(), activity, R_ID);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    Handler.ShowSnack("Houve um erro","ExternalMusicListAdapter.GetMusicArt.onFailure: " + t.toString(), activity, R_ID);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","ExternalMusicListAdapter.GetMusicArt: " + e.getMessage(), activity, R_ID);
        }
    }

}
