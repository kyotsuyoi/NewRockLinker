package com.rocklinker.SplashScreen;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.rocklinker.MainActivity;
import com.rocklinker.R;
import com.rocklinker.Services.PlayerService;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        TextView textViewTitle = findViewById(R.id.activitySplash_TextView_Title);
        TextView textViewMy = findViewById(R.id.activitySplash_TextView_My);
        CardView cardView = findViewById(R.id.activitySplash_CardView);

        Animation animationFadeIn = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fade_in);
        Animation animationZoomInOut = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.zoom_in_out);
        animationZoomInOut.setStartOffset(1000);

        textViewMy.startAnimation(animationZoomInOut);
        textViewTitle.startAnimation(animationFadeIn);
        cardView.startAnimation(animationFadeIn);

        Handler handle = new Handler();
        handle.postDelayed(this::openMain, 3000);
    }

    private void openMain(){
        Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
