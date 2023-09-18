package com.pridhi.twoZeroFourEightEraser;

import static com.pridhi.twoZeroFourEightEraser.R.id.adViewMenu;
import static com.pridhi.twoZeroFourEightEraser.R.id.btn_more_games;
import static com.pridhi.twoZeroFourEightEraser.R.id.btn_rate;
import static com.pridhi.twoZeroFourEightEraser.R.id.btn_share;
import static com.pridhi.twoZeroFourEightEraser.R.id.btn_show_achievements;
import static com.pridhi.twoZeroFourEightEraser.R.id.btn_show_leaderboards;
import static com.pridhi.twoZeroFourEightEraser.R.id.btn_settings;
import static com.pridhi.twoZeroFourEightEraser.R.id.btn_start_4x4;
import static com.pridhi.twoZeroFourEightEraser.R.id.btn_start_5x5;
import static com.pridhi.twoZeroFourEightEraser.R.id.btn_start_6x6;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.databinding.DataBindingUtil;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.PlayGames;
import com.pridhi.twoZeroFourEightEraser.databinding.ActivityMainMenuBinding;
import com.pridhi.twoZeroFourEightEraser.databinding.MainHandlers;

import java.util.Objects;

public class MainMenuActivity extends AppCompatActivity implements MainHandlers {
    public static boolean mIsMainMenu = true;
    static final String playStoreUri = "http://play.google.com/store/apps";
    private static int mRows = 4;

    public static int getRows() {
        return mRows;
    }

    public static int mBackgroundColor = 0;

    // Client used to sign in with Google APIs
    public GamesSignInClient mGamesSignInClient;

    public ActivityMainMenuBinding binding;

    private boolean isAuthenticated;

    ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_menu);
        binding.setHandler(this);

        mIsMainMenu = true;

        Typeface ClearSans_Bold = Typeface.createFromAsset(getResources().getAssets(), "ClearSans-Bold.ttf");

        Button bt4x4 = findViewById(btn_start_4x4);
        Button bt5x5 = findViewById(btn_start_5x5);
        Button bt6x6 = findViewById(btn_start_6x6);

        bt4x4.setTypeface(ClearSans_Bold);
        bt5x5.setTypeface(ClearSans_Bold);
        bt6x6.setTypeface(ClearSans_Bold);

        mBackgroundColor = getResources().getColor(R.color.colorBackground);

        MobileAds.initialize(this, initializationStatus -> {
        });

        AdView mAdView = findViewById(adViewMenu);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Create the client used to sign in to Google services.
        mGamesSignInClient = PlayGames.getGamesSignInClient(this);
        mGamesSignInClient.isAuthenticated().addOnCompleteListener(isAuthenticatedTask -> isAuthenticated =
                (isAuthenticatedTask.isSuccessful() &&
                        isAuthenticatedTask.getResult().isAuthenticated()));
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d("TAG", "Achievements/Leaderboard shown");
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsMainMenu = true;
    }

    private void StartGame(int rows) {
        mRows = rows;
        mIsMainMenu = false;
        startActivity(new Intent(MainMenuActivity.this, MainActivity.class));
    }

    public void onShowAchievementsRequested() {

        PlayGames.getAchievementsClient(this).getAchievementsIntent().addOnSuccessListener(intent -> activityResultLauncher.launch(intent));
    }

    public void onShowLeaderboardsRequested() {
        PlayGames.getLeaderboardsClient(this).getAllLeaderboardsIntent().addOnSuccessListener(intent -> activityResultLauncher.launch(intent));
    }

    @Override
    public void onButtonClick(View view) {
        if (view.getId() == btn_start_4x4) {
            StartGame(4);
        } else if (view.getId() == btn_start_5x5) {
            StartGame(5);
        } else if (view.getId() == btn_start_6x6) {
            StartGame(6);
        } else if (view.getId() == btn_show_achievements) {
            if (isAuthenticated) {
                onShowAchievementsRequested();
            }
        } else if (view.getId() == btn_show_leaderboards) {
            if (isAuthenticated) {
                onShowLeaderboardsRequested();
            }
        } else if (view.getId() == btn_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string._app_name));
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hey Friends Checkout This Interesting Game " + playStoreUri + "/details?id=com.pridhi.twoZeroFourEightEraser");
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)));

        } else if (view.getId() == btn_more_games) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(getString(R.string.play_store_uri)));
                startActivity(intent);
            } catch (Exception e) {
                Log.d("TAG", Objects.requireNonNull(e.getMessage()));
            }
        } else if (view.getId() == btn_settings) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(playStoreUri + "/details?id=" + "io.paridhi.slide.slideme"));
                startActivity(intent);
            } catch (Exception e) {
                Log.d("TAG", Objects.requireNonNull(e.getMessage()));
            }
        } else if (view.getId() == btn_rate) {
            final Uri uri = Uri.parse(playStoreUri + "/details?id=" + "com.pridhi.twoZeroFourEightEraser");
            Intent rateAppIntent = new Intent(Intent.ACTION_VIEW, uri);
            try {
                startActivity(rateAppIntent);
            } catch (Exception e) // for activity not found exception
            {
                Log.d("TAG", Objects.requireNonNull(e.getMessage()));
            }
        }
    }
}