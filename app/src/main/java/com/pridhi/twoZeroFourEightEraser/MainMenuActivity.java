package com.pridhi.twoZeroFourEightEraser;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.databinding.DataBindingUtil;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.pridhi.twoZeroFourEightEraser.databinding.ActivityMainMenuBinding;
import com.pridhi.twoZeroFourEightEraser.databinding.MainHandlers;

public class MainMenuActivity extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener, MainHandlers {
    public static boolean mIsMainMenu = true;
    static final String playStoreUri = "http://play.google.com/store/apps";
    private static int mRows = 4;

    public static int getRows() {
        return mRows;
    }

    private final String BACKGROUND_COLOR_KEY = "BackgroundColor";
    public static int mBackgroundColor = 0;

    // Client used to sign in with Google APIs
    public GoogleSignInClient mGoogleSignInClient;

    // Client variables
    public AchievementsClient mAchievementsClient;
    public LeaderboardsClient mLeaderboardsClient;
    public EventsClient mEventsClient;
    public PlayersClient mPlayersClient;

    // request codes we use when invoking an external activity
    public static final int RC_UNUSED = 5001;
    public static final int RC_SIGN_IN = 9001;
    private static final int RC_ACHIEVEMENT_UI = 9003;

    private AdView mAdView;

    public ActivityMainMenuBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_menu);
        binding.setHandler(this);

        mIsMainMenu = true;

        Typeface ClearSans_Bold = Typeface.createFromAsset(getResources().getAssets(), "ClearSans-Bold.ttf");

        Button bt4x4 = findViewById(R.id.btn_start_4x4);
        Button bt5x5 = findViewById(R.id.btn_start_5x5);
        Button bt6x6 = findViewById(R.id.btn_start_6x6);

        bt4x4.setTypeface(ClearSans_Bold);
        bt5x5.setTypeface(ClearSans_Bold);
        bt6x6.setTypeface(ClearSans_Bold);

        MobileAds.initialize(this, initializationStatus -> {
        });

        mAdView = findViewById(R.id.adViewMenu);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Create the client used to sign in to Google services.
        mGoogleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build());

        if (isNotSignedIn())
            startSignInIntent();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_color_picker:
                mRows = 4;  // because of its GameView!
                startActivity(new Intent(MainMenuActivity.this, ColorPickerActivity.class));
                break;
            case R.id.settings_sign_out:
                signOut();
                break;
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsMainMenu = true;

        // Since the state of the signed in user can change when the activity is not active
        // it is recommended to try and sign in silently from when the app resumes.
        signInSilently();

        SaveColors();
        LoadColors();
    }

    private void SaveColors() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();

        if (mBackgroundColor < 0)
            editor.putInt(BACKGROUND_COLOR_KEY, mBackgroundColor);

        editor.apply();
    }

    private void LoadColors() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        if (settings.getInt(BACKGROUND_COLOR_KEY, mBackgroundColor) < 0)
            mBackgroundColor = settings.getInt(BACKGROUND_COLOR_KEY, mBackgroundColor);
        else
            mBackgroundColor = getResources().getColor(R.color.colorBackground);
    }

    private void StartGame(int rows) {
        mRows = rows;
        mIsMainMenu = false;
        startActivity(new Intent(MainMenuActivity.this, MainActivity.class));
    }

    public void signInSilently() {
        mGoogleSignInClient.silentSignIn().addOnCompleteListener(this, task -> {
            if (task.isSuccessful())
                onConnected(task.getResult());
            else
                onDisconnected();
        });
    }

    private void signOut() {
        if (isNotSignedIn())
            return;

        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            boolean successful = task.isSuccessful();

            onDisconnected();
        });
    }

    public void handleException(Exception e, String details) {
        int status = 0;

        if (e instanceof ApiException) {
            ApiException apiException = (ApiException) e;
            status = apiException.getStatusCode();
        }

        String message = getString(R.string.status_exception_error, details, status, e);
    }

    public void onConnected(GoogleSignInAccount googleSignInAccount) {
        mAchievementsClient = PlayGames.getAchievementsClient(this);
        mLeaderboardsClient = PlayGames.getLeaderboardsClient(this);
        mEventsClient = PlayGames.getEventsClient(this);
        mPlayersClient = PlayGames.getPlayersClient(this);

        // Set the greeting appropriately on main menu
        mPlayersClient.getCurrentPlayer().addOnCompleteListener(task -> {
            String displayName;
            if (task.isSuccessful())
                displayName = task.getResult().getDisplayName();
            else {
                Exception e = task.getException();
                handleException(e, getString(R.string.players_exception));
            }
        });
    }

    public void onDisconnected() {
        mAchievementsClient = null;
        mLeaderboardsClient = null;
        mPlayersClient = null;
    }

    private void startSignInIntent() {
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    private boolean isNotSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(this) == null;
    }

    public void onShowAchievementsRequested() {
        mAchievementsClient.getAchievementsIntent().addOnSuccessListener(intent -> startActivityForResult(intent, RC_ACHIEVEMENT_UI)).addOnFailureListener(e -> handleException(e, getString(R.string.achievements_exception)));
    }

    public void onShowLeaderboardsRequested() {
        mLeaderboardsClient.getAllLeaderboardsIntent().addOnSuccessListener(intent -> startActivityForResult(intent, RC_UNUSED)).addOnFailureListener(e -> handleException(e, getString(R.string.leaderboards_exception)));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                onConnected(account);
            } catch (ApiException apiException) {
                String message = apiException.getMessage();
                if (message == null || message.isEmpty())
                    message = getString(R.string.signin_other_error);

                onDisconnected();

                new AlertDialog.Builder(this).setMessage(message)
                        .setNeutralButton(android.R.string.ok, null).show();
            }
        }
    }

    @Override
    public void onButtonClick(View view) {
        switch (view.getId()) {
            case R.id.btn_start_4x4:
                StartGame(4);
                break;
            case R.id.btn_start_5x5:
                StartGame(5);
                break;
            case R.id.btn_start_6x6:
                StartGame(6);
                break;
            case R.id.btn_show_achievements:
                if (isNotSignedIn())
                    startSignInIntent();
                else {
                    try {
                        onShowAchievementsRequested();
                    } catch (Exception e) {
                        Toast.makeText(this, getString(R.string.try_again), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.btn_show_leaderboards:
                if (isNotSignedIn())
                    startSignInIntent();
                else {
                    try {
                        onShowLeaderboardsRequested();
                    } catch (Exception e) {
                        Toast.makeText(this, getString(R.string.try_again), Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            case R.id.btn_share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string._app_name));
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "Hey Friends Checkout This Interesting Game " + playStoreUri + "/details?id=co.amrit.bubbler");
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_title)));
                break;
            case R.id.btn_more_games:
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("http://play.google.com/store/search?q=pub:MolZol"));
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/search?q=pub:MolZol")));
                }
                break;
            case R.id.btn_rate:
                final Uri uri = Uri.parse(playStoreUri + "/details?id=" + "co.amrit.bubbler");
                Intent rateAppIntent = new Intent(Intent.ACTION_VIEW, uri);
                try {
                    startActivity(rateAppIntent);
                } catch (Exception e) // for activity not found exception
                {
                    e.printStackTrace();
                    Log.d("TAG", e.getMessage());
                }
                break;
            case R.id.btn_settings:
                PopupMenu popup = new PopupMenu(this, view);
                popup.setOnMenuItemClickListener(this);// to implement on click event on items of menu
                MenuInflater inflater = popup.getMenuInflater();
                inflater.inflate(R.menu.menus, popup.getMenu());
                popup.show();
                break;
        }
    }
}