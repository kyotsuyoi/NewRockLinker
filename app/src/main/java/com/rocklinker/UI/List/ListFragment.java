package com.rocklinker.UI.List;

import android.app.Dialog;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.rocklinker.Adapters.CurrentMusicListAdapter;
import com.rocklinker.Adapters.ExternalArtistListAdapter;
import com.rocklinker.Adapters.ExternalMusicListAdapter;
import com.rocklinker.Adapters.InternalMusicListAdapter;
import com.rocklinker.Common.ApiClient;
import com.rocklinker.Common.PlayerInterface;
import com.rocklinker.Common.RecyclerItemClickListener;
import com.rocklinker.DAO.DataBaseCurrentList;
import com.rocklinker.DAO.DataBaseFavorite;
import com.rocklinker.MainActivity;
import com.rocklinker.R;
import com.rocklinker.Services.PlayerService;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ListFragment extends Fragment {

    private MainActivity main;
    private int R_ID = R.id.nav_view;
    private final com.rocklinker.Common.Handler Handler = new com.rocklinker.Common.Handler();

    private final PlayerInterface musicListInterface = ApiClient.getApiClient().create(PlayerInterface.class);
    private RecyclerView recyclerView;

    private Button buttonExternalArtist, buttonCurrentList, buttonFavorites;

    private ExternalArtistListAdapter externalArtistListAdapter;
    private ExternalMusicListAdapter externalMusicListAdapter;
    private InternalMusicListAdapter internalMusicListAdapter;
    private CurrentMusicListAdapter currentMusicListAdapter;

    private int listType = 5;
    //Type 1 to internal music list
    //Type 2 to internal music artist
    //Type 3 to external music list
    //Type 4 to external music artist
    //Type 5 to current music list

    private String path;
    private String URI = ApiClient.BASE_URL+"songs/";

    private DataBaseCurrentList dataBaseCurrentList;
    private DataBaseFavorite dataBaseFavorite;
    private JsonArray currentList;

    private Animation animationOutIn;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_list, container, false);
        recyclerView = root.findViewById(R.id.fragmentList_RecyclerView);

        buttonExternalArtist = root.findViewById(R.id.fragmentList_Button_Artist);
        buttonCurrentList = root.findViewById(R.id.fragmentList_Button_CurrentList);
        buttonFavorites = root.findViewById(R.id.fragmentList_Button_Favorites);

        buttonExternalArtist.setOnClickListener(v -> {
            getExternalArtistList();
            listType = 4;

            animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
            buttonExternalArtist.startAnimation(animationOutIn);
        });

        buttonCurrentList.setOnClickListener(v -> {
            loadCurrentList(false);
            listType = 5;

            animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
            buttonCurrentList.startAnimation(animationOutIn);
        });

        buttonFavorites.setOnClickListener(v -> {
            loadCurrentList(true);
            listType = 5;

            animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
            buttonFavorites.startAnimation(animationOutIn);
        });

        try {
            main = (MainActivity) getActivity();
            assert main != null;
            path = Objects.requireNonNull(main.getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getPath();
            getInternalMusicList();
            setRecyclerView();
            //getExternalArtistList();

            dataBaseCurrentList = new DataBaseCurrentList(main);
            dataBaseCurrentList.createTable();
            loadCurrentList(false);

            dataBaseFavorite = new DataBaseFavorite(main);
            dataBaseFavorite.createTable();
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","ListFragment.onCreateView: " + e.getMessage(), main, R_ID);
        }

        return root;
    }

    private void setRecyclerView(){
        try {
            recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                    main.getBaseContext(), recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    try {
                        JsonObject jsonObject;
                        switch (listType){
                            case 3:
                                if(externalMusicListAdapter == null) return;
                                jsonObject = externalMusicListAdapter.getItem(position);
                                jsonObject.addProperty("uri",URI);
                                PlayerService.setMusic(jsonObject);
                                PlayerService.play();
                                main.SavePreferences();

                                dataBaseCurrentList.dropTable();
                                dataBaseCurrentList.createTable();
                                insertCurrentList(URI, externalMusicListAdapter.getItems());

                                externalMusicListAdapter.notifyDataSetChanged();
                                break;
                            case 4:
                                listType = 3;
                                if (externalArtistListAdapter == null) return;
                                getExternalMusicList(externalArtistListAdapter.getArtistName(position));
                                break;
                            case 5:
                                if (currentMusicListAdapter == null) return;
                                jsonObject = currentMusicListAdapter.getItem(position);
                                PlayerService.setMusic(jsonObject);
                                PlayerService.play();
                                main.SavePreferences();

                                dataBaseCurrentList.dropTable();
                                dataBaseCurrentList.createTable();
                                insertCurrentList(URI, currentMusicListAdapter.getItems());

                                currentMusicListAdapter.notifyDataSetChanged();
                                break;
                        }
                    }catch (Exception e){
                        Handler.ShowSnack("Houve um erro","ListFragment.setRecyclerView.onItemClick: " + e.getMessage(), main, R_ID);
                    }
                }

                @Override
                public boolean onLongItemClick(View view, int position) {
                    switch (listType){
                        case 3:
                            if(externalMusicListAdapter == null) return false;
                            DialogMusicMenu(externalMusicListAdapter.getItem(position));
                            break;
                        case 5:
                            if (currentMusicListAdapter == null) return false;
                            DialogMusicMenu(currentMusicListAdapter.getItem(position));
                            break;
                    }
                    return true;
                }
            }));
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","ListFragment.setRecyclerView: " + e.getMessage(), main, R_ID);
        }
    }

    private void getInternalMusicList(){
        try {
            File dir = new File(path);
            File[] fileList = dir.listFiles();

            List<File> files = new ArrayList<>();

            assert fileList != null;
            Collections.addAll(files, fileList);

            internalMusicListAdapter = new InternalMusicListAdapter(files, main, R_ID);

            Collections.sort(files);
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","ListFragment.getInternalMusicList: " + e.getMessage(), main, R_ID);
        }
    }

    private void getExternalArtistList(){
        try {

            Call<JsonObject> call = musicListInterface.GetArtistList();
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    try {
                        if (!Handler.isRequestError(response, main, R_ID)){
                            JsonObject jsonObject = response.body();
                            assert jsonObject != null;
                            JsonArray data = jsonObject.get("data").getAsJsonArray();

                            RecyclerView.LayoutManager layoutManager;
                            recyclerView.setHasFixedSize(true);
                            layoutManager = new LinearLayoutManager(main);
                            recyclerView.setLayoutManager(layoutManager);

                            externalArtistListAdapter = new ExternalArtistListAdapter(data, main, R_ID);
                            recyclerView.setAdapter(externalArtistListAdapter);
                        }
                    }catch (Exception e){
                        Handler.ShowSnack("Houve um erro","ListFragment.getExternalArtistList.onResponse: " + e.getMessage(), main, R_ID);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    Handler.ShowSnack("Houve um erro","ListFragment.getExternalArtistList.onFailure: " + t.toString(), main, R_ID);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","ListFragment.getExternalArtistList: " + e.getMessage(), main, R_ID);
        }
    }

    private void getExternalMusicList(String artist){
        try {
            Call<JsonObject> call = musicListInterface.GetMusicList(artist);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    try {
                        if (!Handler.isRequestError(response, main, R_ID)){
                            JsonObject jsonObject = response.body();
                            assert jsonObject != null;
                            JsonArray data = jsonObject.get("data").getAsJsonArray();

                            externalMusicListAdapter = new ExternalMusicListAdapter(
                                    internalMusicListAdapter.getFiles(), data, main, R_ID);
                            recyclerView.setAdapter(externalMusicListAdapter);
                        }
                    }catch (Exception e){
                        Handler.ShowSnack("Houve um erro","ListFragment.getExternalMusicList.onResponse: " + e.getMessage(), main, R_ID);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    Handler.ShowSnack("Houve um erro","ListFragment.getExternalMusicList.onFailure: " + t.toString(), main, R_ID);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","ListFragment.getExternalMusicList: " + e.getMessage(), main, R_ID);
        }
    }

    private void loadCurrentList(boolean isFavorite){
        Cursor cursor;
        if(isFavorite){
            cursor = dataBaseFavorite.getData();
        }else {
            dataBaseCurrentList = new DataBaseCurrentList(main);
            dataBaseCurrentList.createTable();
            cursor = dataBaseCurrentList.getData();
        }

        currentList = new JsonArray();
        if(cursor!=null ) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("id", cursor.getString(0));
                jsonObject.addProperty("uri", cursor.getString(1));
                jsonObject.addProperty("filename", cursor.getString(2));
                jsonObject.addProperty("artist", cursor.getString(3));
                jsonObject.addProperty("title", cursor.getString(4));

                currentList.add(jsonObject);
                cursor.moveToNext();
            }
        }

        RecyclerView.LayoutManager layoutManager;
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(main);
        recyclerView.setLayoutManager(layoutManager);

        currentMusicListAdapter = new CurrentMusicListAdapter(currentList, main, R_ID);
        recyclerView.setAdapter(currentMusicListAdapter);

    }

    private void insertCurrentList(String URI, JsonArray jsonArray){

        try {
            for (JsonElement jsonElement : jsonArray) {
                String fileName = jsonElement.getAsJsonObject().get("filename").getAsString();
                String artist = jsonElement.getAsJsonObject().get("artist").getAsString();
                String title = jsonElement.getAsJsonObject().get("title").getAsString();
                dataBaseCurrentList.insert(URI, fileName, artist, title);
            }
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","ListFragment.insertCurrentList: " + e.getMessage(), main, R_ID);
        }
    }

    private void DialogMusicMenu(JsonObject jsonObject){
        try {
            Dialog dialog = new Dialog(main);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setContentView(R.layout.dialog_music_menu);

            TextView textViewTitle = dialog.findViewById(R.id.dialogMusicMenu_TextView_Title);
            TextView textViewArtist = dialog.findViewById(R.id.dialogMusicMenu_TextView_Artist);
            TextView textViewYear = dialog.findViewById(R.id.dialogMusicMenu_TextView_Year);
            ImageView imageView = dialog.findViewById(R.id.dialogMusicMenu_ImageView);
            Button buttonOK = dialog.findViewById(R.id.dialogMusicMenu_Button_OK);
            Button buttonFavorite = dialog.findViewById(R.id.dialogMusicMenu_Button_Favorite);
            Button buttonDownload = dialog.findViewById(R.id.dialogMusicMenu_Button_Download);

            textViewTitle.setText(jsonObject.get("title").getAsString());
            textViewArtist.setText(jsonObject.get("artist").getAsString());

            String year = "Ano desconhecido";
            textViewYear.setText(year);
            if(jsonObject.has("year") && jsonObject.get("year") != JsonNull.INSTANCE){
                textViewYear.setText(jsonObject.get("year").getAsString());
            }

            if(jsonObject.has("art") && jsonObject.get("art") != JsonNull.INSTANCE) {
                imageView.setImageBitmap(Handler.ImageDecode(jsonObject.get("art").getAsString()));
            }

            File file = new File(Objects.requireNonNull(main.getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getAbsolutePath(), jsonObject.get("filename").getAsString());

            if(file.exists()){
                buttonDownload.setEnabled(false);
                buttonDownload.setVisibility(View.INVISIBLE);
            }else{
                buttonDownload.setOnClickListener(v->{
                    Handler.ShowSnack("Indisponível",null, main, R_ID);
                    /*beginDownload(jsonObject.get("filename").getAsString());
                    buttonDownload.setEnabled(false);
                    buttonDownload.setVisibility(View.INVISIBLE);*/
                });
            }

            int fID = dataBaseFavorite.getID(jsonObject.get("filename").getAsString());
            if(fID == 0){
                buttonFavorite.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_favorite_border_24, main.getTheme()));
            }else{
                buttonFavorite.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_favorite_24, main.getTheme()));
                animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.heart_beat);
                buttonFavorite.startAnimation(animationOutIn);
            }

            buttonFavorite.setOnClickListener(v -> {

                //Precisa guardar a URI
                //String URI = jsonObject.get("uri").getAsString();
                String URI = ApiClient.BASE_URL+"songs/";

                String filename = jsonObject.get("filename").getAsString();
                String artist = jsonObject.get("artist").getAsString();
                String title = jsonObject.get("title").getAsString();

                int ID = dataBaseFavorite.getID(filename);
                if(ID != 0){
                    dataBaseFavorite.delete(ID);
                    //Toast.makeText(getContext(),"Removida das favoritas!",Toast.LENGTH_LONG).show();
                    buttonFavorite.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_favorite_border_24, main.getTheme()));

                    animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
                    buttonFavorite.startAnimation(animationOutIn);
                    return;
                }

                dataBaseFavorite.insert(URI,filename,artist,title);
                //Toast.makeText(getContext(),"Salva nas favoritas!",Toast.LENGTH_LONG).show();
                buttonFavorite.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_favorite_24, main.getTheme()));

                animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.heart_beat);
                buttonFavorite.startAnimation(animationOutIn);
            });

            buttonOK.setOnClickListener(v-> dialog.cancel());

            dialog.create();
            dialog.show();

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","ListFragment.DialogMusicMenu: " + e.getMessage(), main, R_ID);
        }
    }

}