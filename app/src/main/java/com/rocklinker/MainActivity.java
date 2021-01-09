package com.rocklinker;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rocklinker.Services.MusicBinder;
import com.rocklinker.Services.PlayerService;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private int R_ID = R.id.nav_view;
    private static final String PREFERENCES = "MYROCKLINKER_PREFERENCES";
    private final com.rocklinker.Common.Handler Handler = new com.rocklinker.Common.Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();

        try {

            BottomNavigationView navView = findViewById(R.id.nav_view);

            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.navigation_home,
                    R.id.navigation_player,
                    R.id.navigation_notifications
            ).build();

            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navView, navController);

            if(!PlayerService.isCreated()){
                final Context context = this.getApplicationContext();
                startService(new Intent(context, PlayerService.class));
                //stopService(new Intent(context, PlayerService.class));
                LoadPreferences();

                /*final Context applicationContext = this.getApplicationContext();
                Intent intent = new Intent(this, PlayerService.class);

                applicationContext.bindService(intent, new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder binder) {
                        if (binder instanceof MusicBinder) {
                            MusicBinder musicBinder = (MusicBinder) binder;
                            PlayerService service = musicBinder.getService();
                            service.onCreate();
                        }
                        applicationContext.unbindService(this);
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                    }
                }, Context.BIND_AUTO_CREATE);*/
            }

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.onCreate: " + e.getMessage(), this, R_ID);
        }
    }

    public void SavePreferences(){
        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean("shuffle", PlayerService.isShuffle());
        editor.putString("repeat", PlayerService.getRepeat());
        if(PlayerService.getFileInformation()!=null){
            editor.putString("fileInformation", PlayerService.getFileInformation().toString());
        }
        editor.apply();
    }

    private void LoadPreferences(){
        try {
            SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);

            PlayerService.setShuffle(settings.getBoolean("shuffle",false));
            PlayerService.setRepeat(settings.getString("repeat","N"));

            JsonParser parser = new JsonParser();
            String jsonString = settings.getString("fileInformation", "");
            if(jsonString.equals(""))return;
            JsonObject jsonObject = (JsonObject) parser.parse(jsonString);

            PlayerService.setFileInformation(jsonObject);
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.LoadPreferences: " + e.getMessage(), this, R_ID);
        }
    }

}