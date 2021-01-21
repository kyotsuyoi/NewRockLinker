package com.rocklinker;

import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rocklinker.DAO.DataBaseCurrentList;
import com.rocklinker.Services.HeadphoneButtonService;
import com.rocklinker.Services.HeadphonePlugService;
import com.rocklinker.Services.PlayerService;
import com.rocklinker.UI.Player.PlayerFragment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private int R_ID = R.id.nav_view;
    public static final String PREFERENCES = "MYROCKLINKER_PREFERENCES";
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
                    R.id.navigation_player,
                    R.id.navigation_home,
                    R.id.navigation_notifications
            ).build();

            NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navView, navController);

            if(!PlayerService.isCreated()){
                //final Context context = this.getApplicationContext();
                //startForegroundService(new Intent(context, PlayerService.class));
                //stopService(new Intent(context, PlayerService.class));

                final Context applicationContext = this.getApplicationContext();
                Intent intent = new Intent(this, PlayerService.class);

                DataBaseCurrentList dataBaseCurrentList = new DataBaseCurrentList(getApplicationContext());
                dataBaseCurrentList.createTable();
                PlayerService.setCursor(dataBaseCurrentList.getData());

                applicationContext.bindService(intent, new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder binder) {
                        /*if (binder instanceof MusicBinder) {
                            MusicBinder musicBinder = (MusicBinder) binder;
                            PlayerService service = musicBinder.getService();
                            service.onCreate();
                        }
                        applicationContext.unbindService(this);*/

                        LoadPreferences();
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name){}
                }, Context.BIND_AUTO_CREATE);
            }

            HeadphonePlugService headphonePlugService = new HeadphonePlugService();
            registerReceiver(headphonePlugService, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
            //registerReceiver(headphonePlugService, new IntentFilter((Intent.ACTION_MEDIA_BUTTON)));

            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            ComponentName receiverComponent = new ComponentName(this, HeadphoneButtonService.class);
            audioManager.registerMediaButtonEventReceiver(receiverComponent);

        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.onCreate: " + e.getMessage(), this, R_ID);
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
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

            PlayerService.setMusic(jsonObject, false);
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.LoadPreferences: " + e.getMessage(), this, R_ID);
        }
    }

}