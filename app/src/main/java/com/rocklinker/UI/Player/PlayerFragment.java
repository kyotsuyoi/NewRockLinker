package com.rocklinker.UI.Player;

import android.annotation.SuppressLint;
import android.database.Cursor;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.rocklinker.Common.ApiClient;
import com.rocklinker.Common.PlayerInterface;
import com.rocklinker.DAO.DataBaseCurrentList;
import com.rocklinker.MainActivity;
import com.rocklinker.R;
import com.rocklinker.Services.PlayerService;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
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
    private Button buttonRepeat;
    private TextView textViewArtist, textViewTitle;
    private TextView textViewCurrentTime, textViewDuration;
    private SeekBar seekbar;
    private ImageView imageViewArt;

    public JsonObject musicInformation;

    private long currentTime = 0;
    private long duration = 0;
    private final android.os.Handler myHandler = new Handler();
    private Animation animationOutIn;

    private DataBaseCurrentList dataBaseCurrentList;
    private JsonArray currentList;

    private int currentPositionOnList = -1;

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

        if(PlayerService.isPlaying()){
            buttonPlay.setBackground(ResourcesCompat.getDrawable(
                    getResources(),
                    R.drawable.ic_pause_24,
                    getActivity().getTheme()
            ));
        }

        setButtons();
        setMusicInformation();
        loadCurrentList();
        setCurrentPositionOnList();

        myHandler.postDelayed(UpdateSongTime, 100);

        return root;
    }

    private void setButtons(){
        buttonPlay.setOnClickListener(v->{
            boolean isPlaying = PlayerService.play();
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
            if(currentPositionOnList >= currentList.size()-1){
                currentPositionOnList = 0;
            }else{
                currentPositionOnList++;
            }

            changeMusic();

            animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
            buttonNext.startAnimation(animationOutIn);
        });

        buttonPrevious.setOnClickListener(v -> {
            if(currentPositionOnList == 0){
                currentPositionOnList = currentList.size()-1;
            }else{
                currentPositionOnList--;
            }

            changeMusic();

            animationOutIn = AnimationUtils.loadAnimation(main.getApplicationContext(),R.anim.zoom_out_in);
            buttonPrevious.startAnimation(animationOutIn);
        });

        buttonRepeat.setOnClickListener(v -> {
            switch (PlayerService.getRepeat()){
                case "1":
                    PlayerService.setRepeat("A");
                    buttonRepeat.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_repeat_24, main.getTheme()));
                    buttonRepeat.setAlpha(1f);
                    break;
                case "A":
                    PlayerService.setRepeat("N");
                    //buttonRepeat.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_no_repeat, getTheme()));
                    buttonRepeat.setAlpha(0.5f);
                    break;
                case "N":
                    PlayerService.setRepeat("1");
                    buttonRepeat.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_repeat_one_24, main.getTheme()));
                    buttonRepeat.setAlpha(1f);
                    break;
            }
            main.SavePreferences();
        });

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentTime = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                PlayerService.setSeekTo(currentTime);
            }
        });
    }

    private void changeMusic(){
        boolean isPlaying = PlayerService.isPlaying();
        JsonObject jsonObject = currentList.get(currentPositionOnList).getAsJsonObject();
        PlayerService.setMusic(jsonObject);
        setMusicInformation();

        if(isPlaying){
            PlayerService.play();
        }

        main.SavePreferences();
    }

    private void getExternalMusicInfo(){
        try {
            imageViewArt.setImageDrawable(null);
            JsonObject jsonObject = PlayerService.getFileInformation();

            textViewArtist.setText(jsonObject.get("artist").getAsString());
            textViewTitle.setText(jsonObject.get("title").getAsString());

            musicInformation = new JsonObject();
            musicInformation.addProperty("artist", jsonObject.get("artist").getAsString());
            musicInformation.addProperty("title", jsonObject.get("title").getAsString());

            if(jsonObject.has("art")) {
                imageViewArt.setImageBitmap(Handler.ImageDecode(jsonObject.get("art").getAsString()));
                musicInformation.addProperty("art", jsonObject.get("art").getAsString());
            }

            getExternalMusicArt(jsonObject.get("filename").getAsString());
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","PlayerFragment.getExternalMusicInfo: " + e.getMessage(), main, R_ID);
        }
    }

    private void getExternalMusicArt(String filename){
        try {
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
                            musicInformation.addProperty("art", jsonArray.get(0).getAsJsonObject().get("art").getAsString());
                            //PlayerNotification.createNotification(main);
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
                //getMusicMeta();
                return;
            }

            getExternalMusicInfo();
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","PlayerFragment.setMusicInformation: " + e.getMessage(), main, R_ID);
        }
    }

    private JsonObject getMusicMeta(){
        return new JsonObject();
    }

    private void loadCurrentList(){
        dataBaseCurrentList = new DataBaseCurrentList(main);
        Cursor cursor = dataBaseCurrentList.getData();

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
    }

    private void setCurrentPositionOnList(){
        for (int i = 0; i < currentList.size(); i++) {
            String fileName = currentList.get(i).getAsJsonObject().get("filename").getAsString();
            if(fileName.equals(PlayerService.getFileName())){
                currentPositionOnList = i;
            }
        }
    }

    private final Runnable UpdateSongTime = new Runnable() {

        @SuppressLint("DefaultLocale")
        public void run() {
            if(PlayerService.getFileName() != null) {

                currentTime = PlayerService.getCurrentTime();
                duration = PlayerService.getDuration();

                String Minutes = String.valueOf(TimeUnit.MILLISECONDS.toMinutes((long) currentTime));
                String Seconds = String.valueOf(TimeUnit.MILLISECONDS.toSeconds((long) currentTime)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) currentTime)));
                if (Seconds.length() < 2) {
                    Seconds = "0" + Seconds;
                }
                textViewCurrentTime.setText(String.format("%s:%s", Minutes, Seconds));

                Minutes = String.valueOf(TimeUnit.MILLISECONDS.toMinutes((long) duration));
                Seconds = String.valueOf(TimeUnit.MILLISECONDS.toSeconds((long) duration)
                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long) duration)));
                if (Seconds.length() < 2) {
                    Seconds = "0" + Seconds;
                }
                textViewDuration.setText(String.format("%s:%s", Minutes, Seconds));

                seekbar.setMax((int) duration);
                seekbar.setProgress((int) currentTime);

                if (!PlayerService.isPlaying()) {
                    buttonPlay.setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play_arrow_24, main.getTheme()));
                }
            }

            myHandler.postDelayed(this, 100);
        }
    };
}