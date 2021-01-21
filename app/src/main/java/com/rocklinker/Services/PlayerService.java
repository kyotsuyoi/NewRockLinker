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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
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
import com.rocklinker.Common.ApiClient;
import com.rocklinker.Common.PlayerNotification;
import com.rocklinker.DAO.DataBaseCurrentList;
import com.rocklinker.MainActivity;
import com.rocklinker.R;
import com.rocklinker.UI.Player.PlayerFragment;

import java.io.File;
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

    private static Cursor cursor;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        musicBind.onBind(this);
        return musicBind;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return (START_STICKY);
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

    public static void setMusic(JsonObject fileInformation, boolean innerSet){
        try {
            PlayerService.fileInformation = fileInformation;
            if(fileInformation == null || fileInformation.equals(""))return;
            //String CompleteURI = fileInformation.get("uri").getAsString()+PlayerService.getFileName();
            String CompleteURI = ApiClient.BASE_URL+"songs/"+PlayerService.getFileName();

            String path = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getPath();
            File file = new File(path, PlayerService.getFileName());

            if(file.exists()){
                CompleteURI = path+"/"+PlayerService.getFileName();
            }else{
                if(!isOnline()){
                    return;
                }
            }

            if(!innerSet) {
                setCurrentPositionOnCursor();
            }

            PlayerNotification.createNotification(context);

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

    public static void setCursor(Cursor cursor){
        PlayerService.cursor = cursor;
    }

    public static void setCurrentPositionOnCursor(){
        if (cursor != null) {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String fileName = cursor.getString(2);

                if(fileName.equals(fileInformation.get("filename").getAsString())){
                    return;
                }

                cursor.moveToNext();
            }
        }
    }

    public void newNext(){
        if(cursor == null)return;
        cursor.moveToNext();
        if(cursor.isAfterLast()){
            cursor.moveToFirst();
        }
        changeMusic();
    }

    public void newPrevious(){
        if(cursor == null)return;
        cursor.moveToPrevious();
        if(cursor.isBeforeFirst()){
            cursor.moveToLast();
        }
        changeMusic();
    }

    private void changeMusic(){
        JsonObject jsonObject = new JsonObject();
        try {
            jsonObject.addProperty("id", cursor.getString(0));
            jsonObject.addProperty("uri", cursor.getString(1));
            jsonObject.addProperty("filename", cursor.getString(2));
            jsonObject.addProperty("artist", cursor.getString(3));
            jsonObject.addProperty("title", cursor.getString(4));

            boolean isPlaying = mediaPlayer.isPlaying();
            setMusic(jsonObject, true);
            if (isPlaying) {
                mediaPlayer.start();
            }

            PlayerService.updatePlayerFragment = true;
            updateListFragment = true;

            //Save current music
            String PREFERENCES = "MYROCKLINKER_PREFERENCES";
            SharedPreferences settings = context.getSharedPreferences(PREFERENCES, 0);
            SharedPreferences.Editor editor = settings.edit();
            if (PlayerService.getFileInformation() != null) {
                editor.putString("fileInformation", PlayerService.getFileInformation().toString());
            }
            editor.apply();
        }catch (Exception e){
            Log.e("Error on PlayerService.changeMusic", e.getMessage());
        }
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
                                    Log.i("MediaPlayer", ">>>>> Trying NEXT <<<<<");
                                    newNext();
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

            Log.i("PlayerService",">>>>> IS RUNNING <<<<<");

            myHandler.postDelayed(this, 100);
        }
    };

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
                    newPrevious();
                    updatePlayerFragment = true;
                    break;
                case "next":
                    newNext();
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

    public static boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

}