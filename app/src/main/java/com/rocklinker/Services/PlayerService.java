package com.rocklinker.Services;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.res.ResourcesCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.rocklinker.Common.PlayerNotification;
import com.rocklinker.DAO.DataBaseCurrentList;
import com.rocklinker.MainActivity;
import com.rocklinker.R;
import com.rocklinker.UI.Player.PlayerFragment;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PlayerService extends Service {

    private static MediaPlayer mediaPlayer;
    private static JsonObject fileInformation;
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    private final Handler myHandler = new Handler();
    private static String repeat = "N";
    private static boolean shuffle = false;
    private static boolean updatePlayerFragment = false;
    private static boolean updateListFragment = false;
    private static boolean created = false;
    private static boolean setMusic = false;

    private static long cur = 0;//To refresh 'currentTime' during 'play()';

    private final MusicBinder musicBind = new MusicBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        musicBind.onBind(this);
        return musicBind;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return (START_NOT_STICKY);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        created=true;
        myHandler.postDelayed(UpdateSongTime, 100);
        registerReceiver(broadcastReceiver, new IntentFilter("broadcastAction"));
        createChannel();
        context = getApplicationContext();
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
            cur=0;
            mediaPlayer.setDataSource(CompleteURI);
            mediaPlayer.prepare();
            //mediaPlayer.start();

            setMusic=true;
        }catch (Exception e){
            //mediaPlayer.release();
            Log.e("Error on PlayerService.setMusic",e.getMessage());
        }
    }

    public boolean play(){
        boolean isPLaying = false;
        if(mediaPlayer == null){
            return false;
        }
        try {
            if (mediaPlayer.isPlaying()) {
                cur = mediaPlayer.getCurrentPosition();
                mediaPlayer.pause();
            } else {
                mediaPlayer.seekTo((int)cur);
                mediaPlayer.start();
            }
            isPLaying = mediaPlayer.isPlaying();

            updateListFragment = true;
            PlayerNotification.createNotification(context);

            return isPLaying;
        }catch (Exception e){
            Log.e("ERROR (PlayerService.play())",e.getMessage());
            return isPLaying;
        }
    }

    public void forcePause(){
        if(mediaPlayer == null){
            return;
        }
        try {

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
            }
            PlayerNotification.createNotification(context);
        }catch (Exception e){
            Log.e("ERROR (PlayerNotification.forcePause())",e.getMessage());
        }
    }

    public static String getFileName(){
        String fileName = "";
        if(PlayerService.fileInformation == null){
            return fileName;
        }
        fileName = fileInformation.get("filename").getAsString();
        return fileName;
    }

    public static JsonObject getFileInformation(){
        return fileInformation;
    }

    public static void setFileInformationArt(String artString){
        PlayerService.fileInformation.addProperty("art", artString);
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

    public static boolean isSetMusic(){
        return setMusic;
    }

    private JsonArray getCurrentPositionOnList(){

        //Precisa de uma forma que seja carregado quando houver mudança na lista
        //Por enquanto carrega sempre que troca de música
        DataBaseCurrentList dataBaseCurrentList;

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

        return currentList;
    }

    private final Runnable UpdateSongTime = new Runnable() {

        @SuppressLint("DefaultLocale")
        public void run() {

            try {
                if(PlayerService.getFileName() != null && created && mediaPlayer != null) {

                    if (mediaPlayer.isPlaying()) {
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
                                    next();
                                }
                                break;
                        }
                    }
                }

            } catch (Exception e) {
                //Existe um erro em
                //if (mediaPlayer.getCurrentPosition() >= mediaPlayer.getDuration() - 1000) {
                //por algum motivo em alguns momentos não consegue ler o estado do mediaPlayer
                Log.e("ERROR (PlayerService.Runnable.run())",e.getMessage());
            }

            myHandler.postDelayed(this, 100);
        }
    };

    //Requer refatoração de previous e next (verificar também com next de PlayerFragment)
    private void next(){

        //Precisa de uma forma que seja carregado quando houver mudança na lista
        //Por enquanto carrega sempre que troca de música
        JsonArray currentList = getCurrentPositionOnList();

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
        play();

        PlayerService.updatePlayerFragment = true;

        //Save current music
        String PREFERENCES = "MYROCKLINKER_PREFERENCES";
        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        SharedPreferences.Editor editor = settings.edit();
        if(PlayerService.getFileInformation()!=null){
            editor.putString("fileInformation", PlayerService.getFileInformation().toString());
        }
        editor.apply();

        updateListFragment = true;
    }

    //Requer refatoração de previous e next (verificar também com previous de PlayerFragment)
    private void previous(){

        //Precisa de uma forma que seja carregado quando houver mudança na lista
        //Por enquanto carrega sempre que troca de música
        JsonArray currentList = getCurrentPositionOnList();

        int currentPositionOnList = 0;

        for (int i = 0; i < currentList.size(); i++) {
            String fileName = currentList.get(i).getAsJsonObject().get("filename").getAsString();
            if (fileName.equals(PlayerService.getFileName())) {
                currentPositionOnList = i;
            }
        }

        if (currentPositionOnList == 0) {
            currentPositionOnList = currentList.size() - 1;
        } else {
            currentPositionOnList--;
        }

        PlayerService.setMusic(currentList.get(currentPositionOnList).getAsJsonObject());
        play();

        PlayerService.updatePlayerFragment = true;

        //Save current music
        String PREFERENCES = "MYROCKLINKER_PREFERENCES";
        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        SharedPreferences.Editor editor = settings.edit();
        if(PlayerService.getFileInformation()!=null){
            editor.putString("fileInformation", PlayerService.getFileInformation().toString());
        }
        editor.apply();

        updateListFragment = true;
    }

    public static boolean isUpdateListFragment(){
        if(updateListFragment){
            updateListFragment = false;
            return true;
        }
        return false;
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = Objects.requireNonNull(intent.getExtras()).getString("actionName");
            switch (Objects.requireNonNull(action)){
                case "play_pause":
                    play();
                    updatePlayerFragment = true;
                    break;
                case "pause":
                    forcePause();
                    updatePlayerFragment = true;
                    break;
                case "previous":
                    previous();
                    updatePlayerFragment = true;
                    break;
                case "next":
                    next();
                    updatePlayerFragment = true;
                    break;
            }
        }
    };

    private void createChannel(){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    PlayerNotification.CHANNEL_ID,
                    "Test",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if(notificationManager != null){
                notificationManager.createNotificationChannel(channel);
            }

            registerReceiver(broadcastReceiver, new IntentFilter("broadcastAction"));
            startService(new Intent(getBaseContext(), OnClearFormRecentService.class));
        }
    }

}
