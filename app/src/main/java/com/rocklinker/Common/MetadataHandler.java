package com.rocklinker.Common;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

import com.google.gson.JsonObject;

public class MetadataHandler {

    /*public JsonObject getMusicInformation(String source){
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(source);

        JsonObject jsonObject = new JsonObject();
        try {
            jsonObject.addProperty("artist", mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST));
            jsonObject.addProperty("title", mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE));

            byte[] art = mediaMetadataRetriever.getEmbeddedPicture();
            assert art != null;
            Bitmap artImage = BitmapFactory.decodeByteArray(art, 0, art.length);
            String artString = Handler.ImageEncode(artImage);
            jsonObject.addProperty("art", artString);

        } catch (Exception e) {
            String unknown = "Arquivo sem informações";
            String filename = source.replace(path,"").replace("/","").replace(".mp3","").replace(" - ","\n");

            jsonObject.addProperty("artist", filename);
            jsonObject.addProperty("title", unknown);
            //jsonObject.addProperty("art", Arrays.toString(art));
        }
        return jsonObject;
    }*/
}
