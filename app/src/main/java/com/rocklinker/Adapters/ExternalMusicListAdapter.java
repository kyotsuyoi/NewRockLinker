package com.rocklinker.Adapters;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.rocklinker.Common.ApiClient;
import com.rocklinker.Common.PlayerInterface;
import com.rocklinker.DAO.DataBaseFavorite;
import com.rocklinker.R;
import com.rocklinker.Services.PlayerService;

import java.io.File;
import java.util.List;

import pl.droidsonroids.gif.GifImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExternalMusicListAdapter extends RecyclerView.Adapter <ExternalMusicListAdapter.ViewHolder> {

    private final Activity activity;
    private final com.rocklinker.Common.Handler Handler = new com.rocklinker.Common.Handler();
    private final int R_ID;
    private final PlayerInterface musicListInterface = ApiClient.getApiClient().create(PlayerInterface.class);
    private boolean isBindViewHolderError;

    private final JsonArray jsonArray;
    private JsonArray filteredJsonArray;

    private DataBaseFavorite dataBaseFavorite;

    public ExternalMusicListAdapter(JsonArray jsonArray, Activity activity, int R_ID) {
        this.jsonArray = jsonArray;
        this.filteredJsonArray = jsonArray;
        this.activity = activity;
        this.R_ID = R_ID;
    }

    @NonNull
    public ViewHolder onCreateViewHolder(ViewGroup parent, int ViewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_external_music_list,parent,false);
        dataBaseFavorite = new DataBaseFavorite(activity);
        dataBaseFavorite.createTable();
        return new ViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        try {
            viewHolder.imageViewArt.setImageDrawable(null);
            viewHolder.textViewMusicName.setText("");
            viewHolder.textViewArtistName.setText("");
            viewHolder.buttonViewDownload.setVisibility(View.VISIBLE);

            String fileName = filteredJsonArray.get(position).getAsJsonObject().get("filename").getAsString();
            String title = null;
            String artist = null;

            if(filteredJsonArray.get(position).getAsJsonObject().get("title") != JsonNull.INSTANCE){
                title = filteredJsonArray.get(position).getAsJsonObject().get("title").getAsString();
            }

            if(filteredJsonArray.get(position).getAsJsonObject().get("artist") != JsonNull.INSTANCE){
                artist = filteredJsonArray.get(position).getAsJsonObject().get("artist").getAsString();
            }

            if(title == null && artist == null){
                title = "Arquivo sem informações";
                artist = fileName;
            }

            //>>>Refatorar<<<
            //Algoritimo que deve ser feito somente uma vez e
            //ir marcando as musicas encontradas
            /*if(files!=null) {
                for (int i = 0; i < files.size(); i++) {
                    String Name = files.get(i).getName();
                    if (Name.equalsIgnoreCase(filename)) {
                        viewHolder.buttonViewDownload.setVisibility(View.INVISIBLE);
                        i = files.size();
                    }
                }
            }*/

            int fID = dataBaseFavorite.getID(fileName);
            if(fID == 0){
                //viewHolder.buttonViewFavorite.setVisibility(View.INVISIBLE);
                viewHolder.buttonViewFavorite.setBackground(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.ic_favorite_border_24, activity.getTheme()));
            }else{
                //viewHolder.buttonViewFavorite.setVisibility(View.VISIBLE);
                viewHolder.buttonViewFavorite.setBackground(ResourcesCompat.getDrawable(activity.getResources(), R.drawable.ic_favorite_24, activity.getTheme()));
            }

            File file = new File(
                    activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath(),
                    filteredJsonArray.get(position).getAsJsonObject().get("filename").getAsString()
            );

            viewHolder.gifImageView.setVisibility(View.INVISIBLE);

            if(PlayerService.getFileName() != null){
                if(PlayerService.getFileName().equals(fileName) && PlayerService.isPlaying()){
                    viewHolder.gifImageView.setVisibility(View.VISIBLE);
                }
            }

            if (file.exists()) {
                getMusicMeta(
                        viewHolder.imageViewArt,
                        viewHolder.textViewMusicName,
                        viewHolder.textViewArtistName,
                        filteredJsonArray.get(position).getAsJsonObject().get("filename").getAsString()
                );
                viewHolder.buttonViewDownload.setVisibility(View.INVISIBLE);
                return;
            }

            viewHolder.textViewArtistName.setText(artist);
            viewHolder.textViewMusicName.setText(title);

            getMusicArt(viewHolder.imageViewArt, fileName, position);

        }catch (Exception e){
            if(!isBindViewHolderError) {
                Handler.ShowSnack("Houve um erro", "ExternalMusicListAdapter.onBindViewHolder: " + e.getMessage(), activity, R_ID);
                isBindViewHolderError=true;
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        ImageView imageViewArt;
        Button buttonViewDownload, buttonViewFavorite;
        TextView textViewArtistName, textViewMusicName;
        GifImageView gifImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            imageViewArt = itemView.findViewById(R.id.itemExternalMusicList_ImageView_Art);
            textViewArtistName = itemView.findViewById(R.id.itemExternalMusicList_TextView_ArtistName);
            textViewMusicName = itemView.findViewById(R.id.itemExternalMusicList_TextView_MusicName);
            buttonViewDownload = itemView.findViewById(R.id.itemExternalMusicList_Button_Download);
            gifImageView = itemView.findViewById(R.id.itemExternalMusicList_ImageView_Gif);

            buttonViewFavorite = itemView.findViewById(R.id.itemExternalMusicList_Button_Favorite);
        }
    }

    public int getItemCount() {
        if(filteredJsonArray==null) {
            return jsonArray.size();
        }
        return filteredJsonArray.size();
    }

    public JsonObject getItem(int position){
        return filteredJsonArray.get(position).getAsJsonObject();
    }

    public JsonArray getItems(){
        return jsonArray;
    }

    private void getMusicArt(ImageView imageView, String filename, int position){
        try {

            int pos = -1;
            for (int i = 0; i < jsonArray.size(); i++) {
                String thisFilename = jsonArray.get(i).getAsJsonObject().get("filename").getAsString();
                if(thisFilename.equals(filename)){
                    pos = i;
                    i = jsonArray.size();
                }
            }

            if(jsonArray.get(pos).getAsJsonObject().has("art")
                    && jsonArray.get(pos).getAsJsonObject().get("art") != JsonNull.INSTANCE){

                imageView.setImageBitmap(Handler.ImageDecode(
                        jsonArray.get(pos).getAsJsonObject().get("art").getAsString()
                ));
                return;
            }

            Call<JsonObject> call = musicListInterface.GetFullMusicArt(filename, true);
            int finalPos = pos;
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    try {
                        if (!Handler.isRequestError(response, activity, R_ID)){
                            JsonObject jsonObject = response.body();
                            assert jsonObject != null;
                            JsonArray localJsonArray = jsonObject.get("data").getAsJsonArray();

                            if(localJsonArray.get(0).getAsJsonObject().get("art") == JsonNull.INSTANCE)return;

                            String artString = localJsonArray.get(0).getAsJsonObject().get("art").getAsString();

                            imageView.setImageBitmap(Handler.ImageDecode(artString));

                            JsonObject newJsonObject = filteredJsonArray.get(position).getAsJsonObject();
                            newJsonObject.addProperty("art",localJsonArray.get(0).getAsJsonObject().get("art").getAsString());
                            jsonArray.set(finalPos, newJsonObject);

                            jsonArray.get(finalPos).getAsJsonObject().addProperty("art",localJsonArray.get(0).getAsJsonObject().get("art").getAsString());
                        }else{
                            imageView.setImageBitmap(null);
                        }
                    }catch (Exception e){
                        Handler.ShowSnack("Houve um erro","ExternalMusicListAdapter.GetMusicArt.onResponse: " + e.getMessage(), activity, R_ID);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    Handler.ShowSnack("Houve um erro","ExternalMusicListAdapter.getMusicArt.onFailure: " + t.toString(), activity, R_ID);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","ExternalMusicListAdapter.getMusicArt: " + e.getMessage(), activity, R_ID);
        }
    }

    public Filter getFilter() {
        return new Filter()
        {
            protected FilterResults performFiltering(CharSequence charSequence)
            {
                FilterResults results = new FilterResults();

                if(charSequence == null || charSequence.length() == 0){
                    results.values = jsonArray;
                    results.count = jsonArray.size();
                }else{

                    JsonArray jsonArray = new JsonArray();

                    int i = 0;
                    try {

                        while (i < jsonArray.size()) {
                            String A = jsonArray.get(i).getAsJsonObject().get("filename").toString().toLowerCase();
                            String B = charSequence.toString().toLowerCase();
                            if (A.contains(B)) {
                                jsonArray.add(jsonArray.get(i));
                            }
                            i++;
                        }

                        results.values = jsonArray;
                        results.count = jsonArray.size();

                    }catch (Exception e){
                        Toast.makeText(activity,e.getMessage(),Toast.LENGTH_LONG).show();
                        results.values = jsonArray;
                        results.count = jsonArray.size();
                    }
                }

                return results;
            }

            protected void publishResults(CharSequence charSequence, FilterResults filterResults)
            {
                filteredJsonArray = (JsonArray) filterResults.values;
                notifyDataSetChanged();
            }
        };
    }

    public void clearFilter(){
        filteredJsonArray = jsonArray;
    }

    //Refatorar
    //Metodo repetido em PlayerFragment e PlayerNotification
    private void getMusicMeta(ImageView imageView, TextView textViewArtist, TextView textViewTitle, String fileName){

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(activity.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/" + fileName);

        try {
            byte[] art = mediaMetadataRetriever.getEmbeddedPicture();
            assert art != null;
            Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
            imageView.setImageBitmap(songImage);

            textViewArtist.setText(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            textViewTitle.setText(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        } catch (Exception e) {
            imageView.setImageBitmap(null);
            String unknown = "Arquivo sem informações";
            String filename = PlayerService.getFileName().replace("/", "").replace(".mp3", "");
            textViewArtist.setText(filename);
            textViewTitle.setText(unknown);
        }
    }

}
