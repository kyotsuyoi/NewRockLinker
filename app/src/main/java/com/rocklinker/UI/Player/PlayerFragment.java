package com.rocklinker.UI.Player;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.rocklinker.Adapters.ExternalMusicListAdapter;
import com.rocklinker.Common.ApiClient;
import com.rocklinker.Common.PlayerInterface;
import com.rocklinker.DAO.DataBase;
import com.rocklinker.DAO.DataBaseCurrentList;
import com.rocklinker.DAO.DataBaseFavorite;
import com.rocklinker.MainActivity;
import com.rocklinker.R;
import com.rocklinker.Services.PlayerService;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlayerFragment extends Fragment {

    private MainActivity main;
    private int R_ID = R.id.nav_view;
    private final com.rocklinker.Common.Handler Handler = new com.rocklinker.Common.Handler();

    private final PlayerInterface playerInterface = ApiClient.getApiClient().create(PlayerInterface.class);

    private Button buttonPlay, buttonPrevious, buttonNext;
    private Button buttonRepeat, buttonShuffle, buttonFavorite;
    private TextView textViewArtist, textViewTitle;
    private TextView textViewCurrentTime, textViewDuration;
    private SeekBar seekbar;
    private ImageView imageViewArt;

    private long currentTime = 0;
    private long duration = 0;
    private final android.os.Handler myHandler = new Handler();
    private Animation animationOutIn;

    private DataBaseCurrentList dataBaseCurrentList;
    private DataBaseFavorite dataBaseFavorite;
    private JsonArray currentList;

    private int currentPositionOnList = -1;

    private boolean isTrackingTouch = false;
    private int currentProgressTouch = 0;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_player, container, false);
        main = (MainActivity) getActivity();

        textViewArtist = root.findViewById(R.id.fragmentPlayer_TextView_ArtistName);
        textViewTitle = root.findViewById(R.id.fragmentPlayer_TextView_MusicName);
        imageViewArt = root.findViewById(R.id.fragmentPlayer_ImageView_Art);

        textViewCurrentTime = root.findViewById(R.id.fragmentPlayer_TextView_CurrentTime);
        textViewDuration = root.findViewById(R.id.fragmentPlayer_TextView_Duration);

        seekbar = root.findViewById(R.id.fragmentPlayer_SeekBar);

        buttonPlay = root.findViewById(R.id.fragmentPlayer_Button_Play);
        buttonPrevious = root.findViewById(R.id.fragmentPlayer_Button_Previous);
        buttonNext = root.findViewById(R.id.fragmentPlayer_Button_Next);

        buttonRepeat =  root.findViewById(R.id.fragmentPlayer_Button_Repeat);
        buttonShuffle =  root.findViewById(R.id.fragmentPlayer_Button_Shuffle);
        buttonFavorite =  root.findViewById(R.id.fragmentPlayer_Button_Favorite);

        if(PlayerService.isPlaying()){
            buttonPlay.setBackground(ResourcesCompat.getDrawable(
                    getResources(),
                    R.drawable.ic_pause_24,
                    getActivity().getTheme()
            ));
        }

        dataBaseFavorite = new DataBaseFavorite(main);
        dataBaseFavorite.createTable();

        setMusicInformation();
        loadCurrentList();
        setCurrentPositionOnList();

        if(!PlayerService.isSetMusic()) {
            PlayerService.setMusic(PlayerService.getFileInformation(), false);
        }

        setButtons();

        //myHandler.postDelayed(UpdateSongTime, 100);

        //main.registerReceiver(broadcastReceiver, new IntentFilter("broadcastAction"));

        return root;
    }

    @Override
    public void onDestroy() {
        //main.unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        myHandler.postDelayed(UpdateSongTime, 100);
        super.onResume();
    }

    @Override
    public void onPause() {
        myHandler.removeCallbacks(UpdateSongTime);
        super.onPause();
    }

    private void setButtons(){

        switch (PlayerService.getRepeat()){
            case "N":
                //buttonRepeat.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_no_repeat, main.getTheme()));
                buttonRepeat.setAlpha(0.5f);
                break;
            case "1":
                buttonRepeat.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_repeat_one_24, main.getTheme()));
                buttonRepeat.setAlpha(1f);
                break;
            case "A":
                buttonRepeat.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_repeat_24, main.getTheme()));
                buttonRepeat.setAlpha(1f);
                break;
        }

        if(PlayerService.isShuffle()){
            buttonShuffle.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_transform_24, main.getTheme()));
            buttonShuffle.setAlpha(1f);
            shuffleList();
        }else{
            //buttonShuffle.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_no_transform, getTheme()));
            buttonShuffle.setAlpha(0.5f);
        }

        setFavorite();

        buttonPlay.setOnClickListener(v->{

            PlayerService playerService = new PlayerService();
            boolean isPlaying = playerService.play();

            if(!isPlaying){
                buttonPlay.setBackground(ResourcesCompat.getDrawable(
                        getResources(),
                        R.drawable.ic_play_arrow_24,
                        getActivity().getTheme()
                ));
            }else{
                buttonPlay.setBackground(ResourcesCompat.getDrawable(
                        getResources(),
                        R.drawable.ic_pause_24,
                        getActivity().getTheme()
                ));
            }

            animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
            buttonPlay.startAnimation(animationOutIn);
        });

        buttonNext.setOnClickListener(v->{
            animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
            buttonNext.startAnimation(animationOutIn);

            /*if(currentPositionOnList >= currentList.size()-1){
                currentPositionOnList = 0;
            }else{
                currentPositionOnList++;
            }*/
            PlayerService playerService = new PlayerService();
            playerService.newNext();
            //changeMusic();
        });

        buttonPrevious.setOnClickListener(v -> {

            animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
            buttonPrevious.startAnimation(animationOutIn);

            if (PlayerService.getCurrentTime() > 5000) {
                PlayerService.setSeekTo(0);
                return;
            }

            /*if (currentPositionOnList == 0) {
                currentPositionOnList = currentList.size() - 1;
            } else {
                currentPositionOnList--;
            }*/
            PlayerService playerService = new PlayerService();
            playerService.newPrevious();
            //changeMusic();
        });

        buttonRepeat.setOnClickListener(v -> {
            switch (PlayerService.getRepeat()){
                case "1":
                    PlayerService.setRepeat("A");
                    buttonRepeat.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_repeat_24, main.getTheme()));
                    buttonRepeat.setAlpha(1f);

                    animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.rotate);
                    buttonRepeat.startAnimation(animationOutIn);
                    break;
                case "A":
                    PlayerService.setRepeat("N");
                    //buttonRepeat.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_no_repeat, getTheme()));
                    buttonRepeat.setAlpha(0.5f);

                    animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
                    buttonRepeat.startAnimation(animationOutIn);
                    break;
                case "N":
                    PlayerService.setRepeat("1");
                    buttonRepeat.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_repeat_one_24, main.getTheme()));
                    buttonRepeat.setAlpha(1f);

                    animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
                    buttonRepeat.startAnimation(animationOutIn);
                    break;
            }
            main.SavePreferences();
        });

        buttonShuffle.setOnClickListener(v -> {

            animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
            buttonShuffle.startAnimation(animationOutIn);

            if(PlayerService.isShuffle()){
                PlayerService.setShuffle(false);
                buttonShuffle.setAlpha(0.5f);
                return;
            }
            PlayerService.setShuffle(true);
            buttonShuffle.setAlpha(1f);
            shuffleList();
        });

        buttonFavorite.setOnClickListener(v -> {

            JsonObject jsonObject = PlayerService.getFileInformation();

            if(PlayerService.getFileInformation()==null)return;

            String URI = jsonObject.get("uri").getAsString();
            String filename = jsonObject.get("filename").getAsString();
            String artist = jsonObject.get("artist").getAsString();
            String title = jsonObject.get("title").getAsString();
            String art = "";
            if(jsonObject.has("art")) {
                art = jsonObject.get("art").getAsString();
            }

            int ID = dataBaseFavorite.getID(filename);
            if(ID != 0){
                dataBaseFavorite.delete(ID);
                //Toast.makeText(getContext(),"Removida das favoritas!",Toast.LENGTH_LONG).show();
                buttonFavorite.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_favorite_border_24, main.getTheme()));

                animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
                buttonFavorite.startAnimation(animationOutIn);
                return;
            }

            dataBaseFavorite.insert(URI, filename, artist, title, art);
            //Toast.makeText(getContext(),"Salva nas favoritas!",Toast.LENGTH_LONG).show();
            buttonFavorite.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_favorite_24, main.getTheme()));

            animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.heart_beat);
            buttonFavorite.startAnimation(animationOutIn);
        });

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(PlayerService.getFileInformation()==null){
                    seekBar.setProgress(0);
                    return;
                }
                currentProgressTouch = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTrackingTouch=true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTrackingTouch=false;
                PlayerService.setSeekTo(currentProgressTouch);
                currentProgressTouch=0;
            }
        });
    }

    /*private void changeMusic(){
        boolean isPlaying = PlayerService.isPlaying();
        if(currentList.size()==0)return;
        JsonObject jsonObject = currentList.get(currentPositionOnList).getAsJsonObject();
        PlayerService.setMusic(jsonObject,false);
        setMusicInformation();

        if(isPlaying){
            PlayerService playerService = new PlayerService();
            playerService.play();
        }

        setFavorite();
        main.SavePreferences();
    }*/

    private void getExternalMusicInfo(){
        try {
            imageViewArt.setImageDrawable(null);
            JsonObject jsonObject = PlayerService.getFileInformation();

            textViewArtist.setText(jsonObject.get("artist").getAsString());
            textViewTitle.setText(jsonObject.get("title").getAsString());

            /*musicInformation = new JsonObject();
            musicInformation.addProperty("artist", jsonObject.get("artist").getAsString());
            musicInformation.addProperty("title", jsonObject.get("title").getAsString());*/

            /*if(jsonObject.has("art")) {
                imageViewArt.setImageBitmap(Handler.ImageDecode(jsonObject.get("art").getAsString()));
                musicInformation.addProperty("art", jsonObject.get("art").getAsString());
            }*/

            dataBaseCurrentList = new DataBaseCurrentList(main);
            dataBaseCurrentList.createTable();
            String art = dataBaseCurrentList.getArt(jsonObject.get("filename").getAsString());
            imageViewArt.setImageBitmap(Handler.ImageDecode(art));
            //musicInformation.addProperty("art", art);

            //getExternalMusicArt(jsonObject.get("filename").getAsString());
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","PlayerFragment.getExternalMusicInfo: " + e.getMessage(), main, R_ID);
        }
    }

    private void getExternalMusicArt(String filename){
        try {

            if(PlayerService.getFileInformation().has("art")
                    && PlayerService.getFileInformation().get("art") != JsonNull.INSTANCE){
                imageViewArt.setImageBitmap(Handler.ImageDecode(PlayerService.getFileInformation().get("art").getAsString()));
                return;
            }

            Call<JsonObject> call = playerInterface.GetFullMusicArt(filename,true);
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    try {
                        if (!Handler.isRequestError(response, main, R_ID)){
                            JsonObject jsonObject = response.body();
                            assert jsonObject != null;
                            JsonArray jsonArray = jsonObject.get("data").getAsJsonArray();

                            imageViewArt.setImageBitmap(Handler.ImageDecode(jsonArray.get(0).getAsJsonObject().get("art").getAsString()));
                            //musicInformation.addProperty("art", jsonArray.get(0).getAsJsonObject().get("art").getAsString());
                            PlayerService.setFileInformationArt(jsonArray.get(0).getAsJsonObject().get("art").getAsString());
                        }else{
                            imageViewArt.setImageBitmap(null);
                        }
                    }catch (Exception e){
                        Handler.ShowSnack("Houve um erro","PlayerFragment.getExternalMusicArt.onResponse: " + e.getMessage(), main, R_ID);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                    Handler.ShowSnack("Houve um erro","PlayerFragment.getExternalMusicArt.onFailure: " + t.toString(), main, R_ID);
                }
            });

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","PlayerFragment.getExternalMusicArt: " + e.getMessage(), main, R_ID);
        }
    }

    private void setMusicInformation(){
        try {
            if (PlayerService.getFileInformation() == null) return;

            File file = new File(
                    main.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath(),
                    PlayerService.getFileName()
            );

            if (file.exists()) {
                getMusicMeta();
                return;
            }

            getExternalMusicInfo();
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","PlayerFragment.setMusicInformation: " + e.getMessage(), main, R_ID);
        }
    }

    //Refatorar
    //Metodo repetido em ExternalMusicListAdapter e Player Notification
    private void getMusicMeta(){

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(main.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/" + PlayerService.getFileName());

        try {
            byte[] art = mediaMetadataRetriever.getEmbeddedPicture();
            assert art != null;
            Bitmap songImage = BitmapFactory.decodeByteArray(art, 0, art.length);
            imageViewArt.setImageBitmap(songImage);

            textViewArtist.setText(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            textViewTitle.setText(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));
        } catch (Exception e) {
            imageViewArt.setImageBitmap(null);
            String unknown = "Arquivo sem informações";
            String filename = PlayerService.getFileName().replace("/", "").replace(".mp3", "");
            textViewArtist.setText(filename);
            textViewTitle.setText(unknown);
        }
    }

    private void loadCurrentList(){
        try {
            dataBaseCurrentList = new DataBaseCurrentList(main);
            dataBaseCurrentList.createTable();
            Cursor cursor = dataBaseCurrentList.getData();

            currentList = new JsonArray();
            if (cursor != null) {
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
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","PlayerFragment.loadCurrentList: " + e.getMessage(), main, R_ID);
        }
    }

    private void setCurrentPositionOnList(){
        for (int i = 0; i < currentList.size(); i++) {
            String fileName = currentList.get(i).getAsJsonObject().get("filename").getAsString();
            if(fileName.equals(PlayerService.getFileName())){
                currentPositionOnList = i;
            }
        }
    }

    private void setFavorite(){
        if(PlayerService.getFileInformation() == null) return;
        int fID = dataBaseFavorite.getID(PlayerService.getFileInformation().get("filename").getAsString());
        if(fID == 0){
            buttonFavorite.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_favorite_border_24, main.getTheme()));
        }else{
            buttonFavorite.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_favorite_24, main.getTheme()));
            animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.heart_beat);
            buttonFavorite.startAnimation(animationOutIn);
        }
    }

    private void shuffleList (){
        try {
            Random random = new Random();

            JsonArray oldArray = currentList;
            JsonArray newArray = new JsonArray();

            for (int i = oldArray.size(); i > 0; i--) {
                int randomPosition = random.nextInt(oldArray.size());

                JsonElement element = oldArray.get(randomPosition);
                oldArray.remove(randomPosition);
                newArray.add(element);
            }

            currentList = newArray;

            dataBaseCurrentList.dropTable();
            dataBaseCurrentList.createTable();

            for(JsonElement jsonElement : currentList){
                String URI = jsonElement.getAsJsonObject().get("uri").getAsString();
                String fileName = jsonElement.getAsJsonObject().get("filename").getAsString();
                String artist = jsonElement.getAsJsonObject().get("artist").getAsString();
                String title = jsonElement.getAsJsonObject().get("title").getAsString();
                String art = jsonElement.getAsJsonObject().get("title").getAsString();
                dataBaseCurrentList.insert(URI, fileName, artist, title, art);
            }

            setCurrentPositionOnList();

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","PlayerFragment.shuffleList: " + e.getMessage(), main, R_ID);
        }
    }

    private final Runnable UpdateSongTime = new Runnable() {

        @SuppressLint("DefaultLocale")
        public void run() {
            String error = PlayerService.getError();
            if(!error.equals("")){
                Handler.ShowSnack(error,null, main, R_ID);
            }

            if(PlayerService.getFileName() != null && !PlayerService.getFileName().equals("")) {

                if(PlayerService.isUpdatePlayerFragment()){
                    setMusicInformation();

                    if (!PlayerService.isPlaying()) {
                        buttonPlay.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play_arrow_24, main.getTheme()));
                    }else{
                        buttonPlay.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause_24, main.getTheme()));
                    }
                    animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
                    buttonPlay.startAnimation(animationOutIn);
                }

                currentTime = PlayerService.getCurrentTime();
                duration = PlayerService.getDuration();

                String Minutes = String.valueOf(TimeUnit.MILLISECONDS.toMinutes(currentTime));
                String Seconds = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(currentTime)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(currentTime)));
                if (Seconds.length() < 2) {
                    Seconds = "0" + Seconds;
                }
                textViewCurrentTime.setText(String.format("%s:%s", Minutes, Seconds));

                Minutes = String.valueOf(TimeUnit.MILLISECONDS.toMinutes(duration));
                Seconds = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(duration)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
                if (Seconds.length() < 2) {
                    Seconds = "0" + Seconds;
                }
                textViewDuration.setText(String.format("%s:%s", Minutes, Seconds));

                seekbar.setMax((int) duration);
                if(!isTrackingTouch) {
                    seekbar.setProgress((int) currentTime);
                }
            }

            myHandler.postDelayed(this, 100);
        }
    };

}