package com.rocklinker.Common;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.exifinterface.media.ExifInterface;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rocklinker.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Objects;

import retrofit2.Response;

public class Handler {

    public boolean isRequestError(Response<JsonObject> response, Activity activity, int R_ID){
        try {
            JsonObject jsonError;
            int code = response.code();
            switch (code) {
                case 200:
                    return isMessageError(response.body(), activity, R_ID);
                case 400:
                case 401:
                case 403:
                    jsonError = new JsonParser().parse(Objects.requireNonNull(response.errorBody()).string()).getAsJsonObject();
                    return  isMessageError(jsonError, activity, R_ID);
                case 404:
                    ShowSnack("Caminho não encontrado",response.raw().toString(), activity, R_ID);
                    return true;
                default:
                    ShowSnack(response.message(),response.raw().toString(), activity, R_ID);
                    return true;
            }
        }catch (Exception e){
            ShowSnack("Houve um erro","Handler.isRequestError: \n"+e.getMessage(), activity, R_ID);
            return true;
        }
    }

    public boolean isMessageError(JsonObject jsonObject, Activity activity, int R_ID){
        try {
            if(jsonObject.get("error").getAsBoolean()) {
                String message = jsonObject.get("message").getAsString();

                if (message.contains("No data")){
                    ShowSnack("Nada encontrado", null, activity, R_ID);
                }else if(message.contains("Error on delete data")) {
                    ShowSnack("Esta tarefa não pode ser removida",
                            "Esta tarefa contém informações vinculadas como por exemplo mensagens, usuários, listas, etc\n\n" + message,
                            activity,
                            R_ID
                    );
                }else if (message.contains("This user is blocked")){
                    ShowSnack("Usuário bloqueado", null, activity, R_ID);
                }else if (message.contains("User or password error")){
                    ShowSnack("Usuário e/ou senha incoretos", null, activity, R_ID);
                }else if (message.contains("This user does not have access to this application")) {
                    ShowSnack("Este usuário não tem acesso nesta aplicação", null, activity, R_ID);
                }else if (message.contains("blocked state_id")){
                    ShowSnack("Você não pode fazer isso", "Não é possível fazer isso por causa do estado atual desta tarefa", activity, R_ID);
                }else if (message.contains("Only the task creator or administrator can do this")){
                    ShowSnack("Você não pode fazer isso", "Apenas o criador da tarefa ou um administrador pode executar essa ação", activity, R_ID);
                }else if (message.contains("Authorization denied")){
                    ShowSnack("Acesso negado", "Sua sessão expirou.\nTalvez tenha sido conectado em outro dispositivo", activity, R_ID);
                }else {
                    ShowSnack("Houve um erro", jsonObject.get("message").getAsString(), activity, R_ID);
                }
            }else{
                return false;
            }
        }catch (Exception e){
            ShowSnack("Houve um erro","Handler.isMessageError: \n"+e.getMessage(), activity, R_ID);
        }
        return true;
    }

    public void ShowSnack(String message, String fullMessage, Activity activity, int R_ID){
        try {
            if (fullMessage != null){
                View.OnClickListener mOnClickListener;
                mOnClickListener = v -> {

                    final Dialog dialog = new Dialog(activity);
                    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    dialog.setContentView(R.layout.dialog_full_message);
                    TextView textView = dialog.findViewById(R.id.dialogFullMessage_textView);
                    Button button = dialog.findViewById(R.id.dialogFullMessage_button);

                    button.setOnClickListener(view -> {
                        dialog.cancel();
                    });

                    textView.setText(fullMessage);
                    dialog.show();
                };
                Snackbar.make(activity.findViewById(R_ID),message,5000).setAction("Ver",mOnClickListener).setActionTextColor(Color.RED).show();
            }else{
                Snackbar.make(activity.findViewById(R_ID), message, 5000).show();
            }
        }catch (Exception e){
            Toast.makeText(activity.getApplicationContext(), "Handler.ShowSnack: \n"+e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public Bitmap ImageDecode(String code) {
        byte[] decodeString = Base64.decode(code, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodeString, 0, decodeString.length);
    }

    public String ImageEncode(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] bytes = stream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    public Bitmap ImageOrientation(Bitmap bitmap, File file){
        try {
            ExifInterface ei = new ExifInterface(file.getAbsolutePath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            Bitmap rotatedBitmap = null;
            switch (orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = RotateImage(bitmap, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = RotateImage(bitmap, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = RotateImage(bitmap, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotatedBitmap = bitmap;
            }
            return rotatedBitmap;
        }catch (Exception ignored){}
        return null;
    }

    private static Bitmap RotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

}
