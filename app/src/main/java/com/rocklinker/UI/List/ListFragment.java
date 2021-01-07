package com.rocklinker.UI.List;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rocklinker.Adapters.ExternalArtistListAdapter;
import com.rocklinker.Adapters.ExternalMusicListAdapter;
import com.rocklinker.Adapters.InternalMusicListAdapter;
import com.rocklinker.Common.ApiClient;
import com.rocklinker.Common.PlayerInterface;
import com.rocklinker.Common.RecyclerItemClickListener;
import com.rocklinker.DAO.DataBaseCurrentList;
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

    private ExternalArtistListAdapter externalArtistListAdapter;
    private ExternalMusicListAdapter externalMusicListAdapter;
    private InternalMusicListAdapter internalMusicListAdapter;

    private int listType = 4;
    //Type 1 to internal music list
    //Type 2 to internal music artist
    //Type 3 to external music list
    //Type 4 to external music artist

    private String path;
    private String URI = ApiClient.BASE_URL+"songs/";

    private DataBaseCurrentList dataBaseCurrentList;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_list, container, false);
        recyclerView = root.findViewById(R.id.fragmentList_RecyclerView);

        try {
            main = (MainActivity) getActivity();
            assert main != null;
            path = Objects.requireNonNull(main.getExternalFilesDir(Environment.DIRECTORY_MUSIC)).getPath();
            getInternalMusicList();
            setRecyclerView();
            getExternalArtistList();

            dataBaseCurrentList = new DataBaseCurrentList(main);
            dataBaseCurrentList.createTable();
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","ListFragment.onCreateView: " + e.getMessage(), main, R_ID);
        }

        return root;
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

    private void setRecyclerView(){
        try {
            recyclerView.addOnItemTouchListener(new RecyclerItemClickListener(
                    main.getBaseContext(), recyclerView, new RecyclerItemClickListener.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    try {
                        switch (listType){
                            case 3:
                                JsonObject jsonObject = externalMusicListAdapter.getItem(position);
                                jsonObject.addProperty("uri",URI);
                                PlayerService.setMusic(jsonObject);
                                PlayerService.play();
                                main.SavePreferences();

                                dataBaseCurrentList.dropTable();
                                dataBaseCurrentList.createTable();
                                insertCurrentList(URI);

                                externalMusicListAdapter.notifyDataSetChanged();
                                break;
                            case 4:
                                listType = 3;
                                if (externalArtistListAdapter == null) return;
                                getExternalMusicList(externalArtistListAdapter.getArtistName(position));
                                break;
                        }
                    }catch (Exception e){
                        Handler.ShowSnack("Houve um erro","ListFragment.setRecyclerView.onItemClick: " + e.getMessage(), main, R_ID);
                    }
                }

                @Override
                public boolean onLongItemClick(View view, int position) {
                    return true;
                }
            }));
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","ListFragment.setRecyclerView: " + e.getMessage(), main, R_ID);
        }
    }

    private void insertCurrentList(String URI){
        for(JsonElement jsonElement : externalMusicListAdapter.getItems()){
            String fileName = jsonElement.getAsJsonObject().get("filename").getAsString();
            String artist = jsonElement.getAsJsonObject().get("artist").getAsString();
            String title = jsonElement.getAsJsonObject().get("title").getAsString();
            dataBaseCurrentList.insert(URI,fileName,artist,title);
        }
    }

}