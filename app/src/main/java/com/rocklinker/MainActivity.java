package com.rocklinker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rocklinker.DAO.DataBase;
import com.rocklinker.DAO.DataBaseCurrentList;
import com.rocklinker.Services.PlayerService;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private int R_ID = R.id.nav_view;
    private static final String PREFERENCES = "MYROCKLINKER_PREFERENCES";
    private final com.rocklinker.Common.Handler Handler = new com.rocklinker.Common.Handler();
    //public String repeat = "N";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide();

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

        startService(new Intent(MainActivity.this, PlayerService.class));
        LoadPreferences();

        /*dataBaseCurrentList = new DataBaseCurrentList(this);
        dataBaseCurrentList.createTable();*/
    }

    public void SavePreferences(){
        SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);
        SharedPreferences.Editor editor = settings.edit();

        //editor.putBoolean("shuffle", isShuffle);
        editor.putString("repeat", PlayerService.getRepeat());
        editor.putString("fileInformation", PlayerService.getFileInformation().toString());
        editor.apply();
    }

    private void LoadPreferences(){
        try {
            SharedPreferences settings = getSharedPreferences(PREFERENCES, 0);

            //isShuffle = settings.getBoolean("shuffle",false);
            //repeat = settings.getString("repeat","N");
            PlayerService.setRepeat(settings.getString("repeat","N"));

        /*if(isShuffle){
            buttonShuffle.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_transform_purple, getTheme()));
        }else{
            buttonShuffle.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_no_transform, getTheme()));
        }*/

            JsonParser parser = new JsonParser();
            String jsonString = settings.getString("fileInformation", "");
            if(jsonString.equals(""))return;
            JsonObject jsonObject = (JsonObject) parser.parse(jsonString);

            PlayerService.setFileInformation(jsonObject);
        }catch (Exception e){
            Handler.ShowSnack("Houve um erro","MainActivity.LoadPreferences: " + e.getMessage(), this, R_ID);
        }
    }

    /*public DataBaseCurrentList getDataBaseCurrentList(){
        return dataBaseCurrentList;
    }*/

}