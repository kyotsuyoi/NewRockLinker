package com.rocklinker.Services;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.rocklinker.DAO.DataBaseCurrentList;
import com.rocklinker.MainActivity;
import com.rocklinker.R;
import com.rocklinker.UI.Player.PlayerFragment;

import java.util.concurrent.TimeUnit;

public class PlayerService extends Service {

    private static MediaPlayer mediaPlayer;
    private static JsonObject fileInformation;
    private final Handler myHandler = new Handler();
    private static String repeat = "N";
    private static boolean shuffle = false;
    private static boolean updatePlayerFragment = false;
    private static boolean created = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {return null;}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return (START_REDELIVER_INTENT);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        created=true;
        myHandler.postDelayed(UpdateSongTime, 100);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
    }

    public static void setMusic(JsonObject fileInformation){
        try {
            PlayerService.fileInformation = fileInformation;
            String CompleteURI = fileInformation.get("uri").getAsString()+PlayerService.getFileName();
            if(mediaPlayer!=null){
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(CompleteURI);
            mediaPlayer.prepare();
        }catch (Exception e){
            mediaPlayer.release();
            //Log.e("Error on PlayerService.setMusic",e.getMessage());
        }
    }

    public static boolean play(){
        if(mediaPlayer == null){
            return false;
        }
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.start();
            }
            return mediaPlayer.isPlaying();
        }catch (Exception e){
            return false;
        }
    }

    public static String getFileName(){
        if(PlayerService.fileInformation == null){
            return null;
        }
        return PlayerService.fileInformation.get("filename").getAsString();
    }

    public static JsonObject getFileInformation(){
        return fileInformation;
    }

    public static void setFileInformation(JsonObject fileInformation){
        PlayerService.fileInformation = fileInformation;
        setMusic(fileInformation);
    }

    public static boolean isPlaying(){
        try{
            return mediaPlayer.isPlaying();
        }catch (Exception e){
            return false;
        }
    }

    public static long getCurrentTime(){
        try{
            return mediaPlayer.getCurrentPosition();
        }catch (Exception e){
            return 0;
        }
    }

    public static long getDuration(){
        try{
            return mediaPlayer.getDuration();
        }catch (Exception e){
            return 0;
        }
    }

    public static void setSeekTo(long time){
        try{
            mediaPlayer.seekTo(time,MediaPlayer.SEEK_CLOSEST_SYNC);
        }catch (Exception ignored){}
    }

    public static void setRepeat(String repeat){
        PlayerService.repeat = repeat;
    }

    public static String getRepeat(){
        return PlayerService.repeat;
    }

    public static void setShuffle(boolean shuffle){
        PlayerService.shuffle = shuffle;
    }

    public static boolean isShuffle(){
        return shuffle;
    }

    public static boolean isUpdatePlayerFragment(){
        if(updatePlayerFragment){
            updatePlayerFragment = false;
            return true;
        }
        return false;
    }

    public static boolean isCreated(){
        return created;
    }

    private final Runnable UpdateSongTime = new Runnable() {

        DataBaseCurrentList dataBaseCurrentList;

        @SuppressLint("DefaultLocale")
        public void run() {
            if(PlayerService.getFileName() != null && created) {

                try{
                    switch (repeat) {
                        case "N":
                            if (mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration() - 1000) {
                                mediaPlayer.seekTo(0);
                                mediaPlayer.pause();
                            }
                            break;
                        case "1":
                            if (mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration() - 1000) {
                                mediaPlayer.seekTo(0);
                            }
                            break;
                        case "A":

                            if (mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration() - 1000) {

                                dataBaseCurrentList = new DataBaseCurrentList(getApplicationContext());
                                Cursor cursor = dataBaseCurrentList.getData();

                                JsonArray currentList = new JsonArray();
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

                                int currentPositionOnList = 0;

                                for (int i = 0; i < currentList.size(); i++) {
                                    String fileName = currentList.get(i).getAsJsonObject().get("filename").getAsString();
                                    if (fileName.equals(PlayerService.getFileName())) {
                                        currentPositionOnList = i;
                                    }
                                }

                                if (currentPositionOnList >= currentList.size() - 1) {
                                    currentPositionOnList = 0;
                                } else {
                                    currentPositionOnList++;
                                }

                                PlayerService.setMusic(currentList.get(currentPositionOnList).getAsJsonObject());
                                PlayerService.play();

                                PlayerService.updatePlayerFragment = true;

                            }
                            break;
                    }

                }catch (Exception e){
                    //Existe um erro em
                    //if (mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration() - 1000) {
                    //por algum motivo em alguns momentos não consegue ler o estado do mediaPlayer
                }
            }

            myHandler.postDelayed(this, 100);
        }
    };

}
