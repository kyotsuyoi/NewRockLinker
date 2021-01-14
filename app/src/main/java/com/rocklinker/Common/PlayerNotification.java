package com.rocklinker.Common;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Environment;
import android.support.v4.media.session.MediaSessionCompat;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.gson.JsonObject;
import com.rocklinker.DAO.DataBaseCurrentList;
import com.rocklinker.MainActivity;
import com.rocklinker.R;
import com.rocklinker.Services.NotificationActionService;
import com.rocklinker.Services.PlayerService;

import java.io.File;

public class PlayerNotification {

    private static final com.rocklinker.Common.Handler Handler =
            new com.rocklinker.Common.Handler();

    public static final String CHANNEL_ID = "channel";

    public static final String ACTION_PREVIOUS = "previous";
    public static final String ACTION_PLAY = "play_pause";
    public static final String ACTION_NEXT = "next";

    public static Notification notification;

    public static void createNotification(Context context){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            MediaSessionCompat mediaSessionCompat = new MediaSessionCompat(context, "tag");

            Bitmap art = null;
            JsonObject musicInfo = PlayerService.getFileInformation();

            if(musicInfo == null){
                musicInfo = new JsonObject();
                musicInfo.addProperty("title", "Desconhecido");
                musicInfo.addProperty("artist", "Desconhecido");
            }else{
                String path = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getPath();
                File file = new File(path, musicInfo.get("filename").getAsString());

                if(file.exists()){
                    art = getMusicMeta(context, musicInfo.get("filename").getAsString());
                }else{
                    DataBaseCurrentList dataBaseCurrentList = new DataBaseCurrentList(context);
                    dataBaseCurrentList.createTable();
                    String artString = dataBaseCurrentList.getArt(musicInfo.get("filename").getAsString());
                    if(!artString.equals("")) {
                        art = Handler.ImageDecode(artString);
                    }
                }
            }

            int play = R.drawable.ic_play_arrow_24;
            if(PlayerService.isPlaying()){
                play = R.drawable.ic_pause_24;
            }

            PendingIntent pendingIntentPrevious, pendingIntentPlay, pendingIntentNext, pendingIntent;

            Intent intentPrevious = new Intent(context, NotificationActionService.class)
                    .setAction(ACTION_PREVIOUS);
            pendingIntentPrevious = PendingIntent.getBroadcast(context, 0,
                    intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intentPlay = new Intent(context, NotificationActionService.class)
                    .setAction(ACTION_PLAY);
            pendingIntentPlay = PendingIntent.getBroadcast(context, 0,
                    intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent intentNext = new Intent(context, NotificationActionService.class)
                    .setAction(ACTION_NEXT);
            pendingIntentNext = PendingIntent.getBroadcast(context, 0,
                    intentNext, PendingIntent.FLAG_UPDATE_CURRENT);

            Intent notificationIntent = new Intent(context, MainActivity.class);
            pendingIntent = PendingIntent.getActivity(context, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(musicInfo.get("title").getAsString())
                    .setContentText(musicInfo.get("artist").getAsString())
                    .setContentIntent(pendingIntent)
                    .setLargeIcon(art)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(false)
                    .addAction(R.drawable.ic_skip_previous_24, "Anterior", pendingIntentPrevious)
                    .addAction(play, "Play ou Pause", pendingIntentPlay)
                    .addAction(R.drawable.ic_skip_next_24, "Próxima", pendingIntentNext)
                    .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                            .setShowActionsInCompactView(0,1,2)
                            .setMediaSession(mediaSessionCompat.getSessionToken())
                    )
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            notificationManagerCompat.notify(1, notification);
        }
    }

    //Refatorar
    //Metodo com caracteristica repetida em PlayerFragment e ExternalMusicListAdapter
    private static Bitmap getMusicMeta(Context context, String fileName){

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath() + "/" + fileName);

        try {
            byte[] art = mediaMetadataRetriever.getEmbeddedPicture();
            assert art != null;
            return BitmapFactory.decodeByteArray(art, 0, art.length);

        } catch (Exception e) {
            return null;
        }
    }

}
