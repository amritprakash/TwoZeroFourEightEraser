package com.pridhi.twoZeroFourEightEraser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.EventBuffer;
import com.google.android.gms.tasks.Task;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    public static int mRewardDeletes = 2;

    private final String TAG = "MainActivity";

    // delete selection:
    public static int mRewardDeletingSelectionAmounts = 3;

    private static final String REWARD_DELETES = "reward chances";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    private static final String SCORE = "score";
    private static final String HIGH_SCORE = "high score temp";
    private static final String UNDO_SCORE = "undo score";
    private static final String CAN_UNDO = "can undo";
    private static final String UNDO_GRID = "undo";
    private static final String GAME_STATE = "game state";
    private static final String UNDO_GAME_STATE = "undo game state";
    private static final String REWARD_DELETE_SELECTION = "reward delete selection amounts";


    private MainView view;

    private AdView mAdView;
    public InterstitialAd mInterstitialAd;
    private RewardedAd mRewardedAd;


    // Google Play Games Services:
    public GoogleSignInClient mGoogleSignInClient;  // Client used to sign in with Google APIs

    // Client variables
    public AchievementsClient mAchievementsClient;
    public LeaderboardsClient mLeaderboardsClient;
    public EventsClient mEventsClient;
    public PlayersClient mPlayersClient;

    // request codes we use when invoking an external activity
    public static final int RC_SIGN_IN = 9001;

    // achievements and scores we're pending to push to the cloud
    // (waiting for the user to sign in, for instance)
    private static long mHighScore4x4;
    private static long mHighScore5x5;
    private static long mHighScore6x6;

    private boolean mAchievementSenior;
    private boolean mAchievementFresher;
    private boolean mAchievement8192At4x4;
    private boolean mAchievement4096At4x4;
    private boolean mAchievement2048At4x4;
    private boolean mAchievement1024At4x4;
    private boolean mAchievement8192At5x5;
    private boolean mAchievement4096At5x5;
    private boolean mAchievement2048At5x5;
    private boolean mAchievement2048At6x6;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        FrameLayout frameLayout = findViewById(R.id.game_frame_layout);
        view = new MainView(this, this);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        view.hasSaveState = settings.getBoolean("save_state", false);

        if (savedInstanceState != null)
            if (savedInstanceState.getBoolean("hasState"))
                load();

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(params);

        frameLayout.addView(view);

        MobileAds.initialize(this, initializationStatus -> {
        });

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        loadInterstitial();
        loadRewarded();
        mGoogleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build());

        if (!isSignedIn())
            startSignInIntent();
    }

    private void loadInterstitial() {
        AdRequest interstitialAdRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", interstitialAdRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        MainActivity.this.mInterstitialAd = interstitialAd;
                        Log.i(TAG, "onAdLoaded");
                        interstitialAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        loadInterstitial();
                                        Log.d("TAG", "The ad was dismissed.");
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                        MainActivity.this.mInterstitialAd = null;
                                        Log.d("TAG", "The ad failed to show." + adError.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, loadAdError.toString());
                        mInterstitialAd = null;
                    }
                });
    }

    private void loadRewarded() {
        AdRequest rewardedAdRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", rewardedAdRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        MainActivity.this.mRewardedAd = rewardedAd;
                        Log.d(TAG, "onAdLoaded");
                        rewardedAd.setFullScreenContentCallback(
                                new FullScreenContentCallback() {
                                    @Override
                                    public void onAdDismissedFullScreenContent() {
                                        loadRewarded();
                                        Log.d("TAG", "The ad was dismissed.");
                                    }

                                    @Override
                                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                        MainActivity.this.mRewardedAd = null;
                                        Log.d("TAG", "The ad failed to show." + adError.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.d(TAG, loadAdError.toString());
                        mRewardedAd = null;
                    }
                });
    }

    public void showInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(this);
        } else {
            loadInterstitial();
        }
    }

    public void showRewarded() {
        if (mRewardedAd != null) {
            Activity activityContext = MainActivity.this;
            mRewardedAd.show(activityContext, rewardItem -> {
                // Handle the reward.
                Log.d(TAG, "The user earned the reward.");
                int rewardAmount = rewardItem.getAmount();
                String rewardType = rewardItem.getType();
                Log.d(TAG, "RewardAmount : " + rewardAmount + ", RewardType : " + rewardType);
            });
        } else {
            loadRewarded();
            Log.d(TAG, "The rewarded ad wasn't ready yet.");
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU)
            return true;
        else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            view.game.move(2);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            view.game.move(0);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            view.game.move(3);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            view.game.move(1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("hasState", true);
        save();
        super.onSaveInstanceState(savedInstanceState);
    }

    protected void onPause() {
        super.onPause();
        save();

        if (isSignedIn()) {
            pushAccomplishments();
            updateLeaderboards();
        }
    }

    protected void onResume() {
        super.onResume();
        load();

        // Since the state of the signed in user can change when the activity is not active
        // it is recommended to try and sign in silently from when the app resumes.
        signInSilently();

        if (isSignedIn()) {
            pushAccomplishments();
            updateLeaderboards();
        }
    }

    private void save() {
        final int rows = MainMenuActivity.getRows();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        Tile[][] field = view.game.grid.field;
        Tile[][] undoField = view.game.grid.undoField;
        editor.putInt(WIDTH + rows, field.length);
        editor.putInt(HEIGHT + rows, field.length);

        for (int xx = 0; xx < field.length; xx++) {
            for (int yy = 0; yy < field[0].length; yy++) {
                if (field[xx][yy] != null)
                    editor.putInt(rows + " " + xx + " " + yy, field[xx][yy].getValue());
                else
                    editor.putInt(rows + " " + xx + " " + yy, 0);

                if (undoField[xx][yy] != null)
                    editor.putInt(UNDO_GRID + rows + " " + xx + " " + yy, undoField[xx][yy].getValue());
                else
                    editor.putInt(UNDO_GRID + rows + " " + xx + " " + yy, 0);
            }
        }

        // reward deletions:
        editor.putInt(REWARD_DELETES + rows, mRewardDeletes);
        editor.putInt(REWARD_DELETE_SELECTION + rows, mRewardDeletingSelectionAmounts);

        // game values:
        editor.putLong(SCORE + rows, view.game.score);
        editor.putLong(HIGH_SCORE + rows, view.game.highScore);
        editor.putLong(UNDO_SCORE + rows, view.game.lastScore);
        editor.putBoolean(CAN_UNDO + rows, view.game.canUndo);
        editor.putInt(GAME_STATE + rows, view.game.gameState);
        editor.putInt(UNDO_GAME_STATE + rows, view.game.lastGameState);
        editor.apply();

        // my reason for writing this operation here: i want take effect after save()
        switch (MainMenuActivity.getRows()) {
            case 4:
                mHighScore4x4 = view.game.highScore;
                break;
            case 5:
                mHighScore5x5 = view.game.highScore;
                break;
            case 6:
                mHighScore6x6 = view.game.highScore;
                break;
        }
    }

    private void load() {
        final int rows = MainMenuActivity.getRows();

        //Stopping all animations
        view.game.aGrid.cancelAnimations();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        for (int xx = 0; xx < view.game.grid.field.length; xx++) {
            for (int yy = 0; yy < view.game.grid.field[0].length; yy++) {
                int value = settings.getInt(rows + " " + xx + " " + yy, -1);
                if (value > 0)
                    view.game.grid.field[xx][yy] = new Tile(xx, yy, value);
                else if (value == 0)
                    view.game.grid.field[xx][yy] = null;

                int undoValue = settings.getInt(UNDO_GRID + rows + " " + xx + " " + yy, -1);
                if (undoValue > 0)
                    view.game.grid.undoField[xx][yy] = new Tile(xx, yy, undoValue);
                else if (value == 0)
                    view.game.grid.undoField[xx][yy] = null;
            }
        }

        mRewardDeletes = settings.getInt(REWARD_DELETES + rows, 2);
        mRewardDeletingSelectionAmounts = settings.getInt(REWARD_DELETE_SELECTION + rows, 3);

        view.game.score = settings.getLong(SCORE + rows, view.game.score);
        view.game.highScore = settings.getLong(HIGH_SCORE + rows, view.game.highScore);
        view.game.lastScore = settings.getLong(UNDO_SCORE + rows, view.game.lastScore);
        view.game.canUndo = settings.getBoolean(CAN_UNDO + rows, view.game.canUndo);
        view.game.gameState = settings.getInt(GAME_STATE + rows, view.game.gameState);
        view.game.lastGameState = settings.getInt(UNDO_GAME_STATE + rows, view.game.lastGameState);
    }

    public void loadAndPrintEvents() {
        mEventsClient.load(true).addOnSuccessListener(eventBufferAnnotatedData -> {
                    EventBuffer eventBuffer = eventBufferAnnotatedData.get();

                    int count = 0;
                    if (eventBuffer != null)
                        count = eventBuffer.getCount();

                    for (int i = 0; i < count; i++) {
                        Event event = eventBuffer.get(i);
                        Log.i(TAG, "event: "
                                + event.getName()
                                + " -> "
                                + event.getValue());
                    }
                })
                .addOnFailureListener(e -> handleException(e, getString(R.string.achievements_exception)));
    }

    public void signInSilently() {
        mGoogleSignInClient.silentSignIn().addOnCompleteListener(this, task -> {
            if (task.isSuccessful())
                onConnected(task.getResult());
            else
                onDisconnected();
        });
    }

    public void handleException(Exception e, String details) {
        Log.d("TAG", e.getMessage() + details);

    }

    public void pushAccomplishments() {
        if (!isSignedIn())
            return; // can't push to the cloud, try again later

        try {

            if (mAchievement8192At4x4) {
                mAchievementsClient.unlock(getString(R.string.achievement_8192_at_4x4));
                mAchievement8192At4x4 = false;
            }

            if (mAchievement4096At4x4) {
                mAchievementsClient.unlock(getString(R.string.achievement_4096_at_4x4));
                mAchievement4096At4x4 = false;
            }

            if (mAchievement2048At4x4) {
                mAchievementsClient.unlock(getString(R.string.achievement_2048_at_4x4));
                mAchievement2048At4x4 = false;
            }

            if (mAchievement1024At4x4) {
                mAchievementsClient.unlock(getString(R.string.achievement_1024_at_4x4));
                mAchievement1024At4x4 = false;
            }

            if (mAchievement8192At5x5) {
                mAchievementsClient.unlock(getString(R.string.achievement_8192_at_5x5));
                mAchievement8192At5x5 = false;
            }

            if (mAchievement4096At5x5) {
                mAchievementsClient.unlock(getString(R.string.achievement_4096_at_5x5));
                mAchievement4096At5x5 = false;
            }

            if (mAchievement2048At5x5) {
                mAchievementsClient.unlock(getString(R.string.achievement_2048_at_5x5));
                mAchievement2048At5x5 = false;
            }

            if (mAchievement2048At6x6) {
                mAchievementsClient.unlock(getString(R.string.achievement_2048_at_6x6));
                mAchievement2048At6x6 = false;
            }
        } catch (Exception e) {
            Log.e("PushAccomplishments", Arrays.toString(e.getStackTrace()));
        }
    }

    private void updateLeaderboards() {
        if (!isSignedIn())
            return; // can't push to the cloud, try again later

        try {
            if (mHighScore4x4 >= 0) {
                mLeaderboardsClient.submitScore(getString(R.string.leaderboard_4x4), mHighScore4x4);
                mHighScore4x4 = -1;
            }

            if (mHighScore5x5 >= 0) {
                mLeaderboardsClient.submitScore(getString(R.string.leaderboard_5x5), mHighScore5x5);
                mHighScore5x5 = -1;
            }

            if (mHighScore6x6 >= 0) {
                mLeaderboardsClient.submitScore(getString(R.string.leaderboard_6x6), mHighScore6x6);
                mHighScore6x6 = -1;
            }
        } catch (Exception e) {
            Log.e("updateLeaderboards", Arrays.toString(e.getStackTrace()));
        }
    }

    public void onConnected(GoogleSignInAccount googleSignInAccount) {
        mAchievementsClient = PlayGames.getAchievementsClient(this);
        mLeaderboardsClient = PlayGames.getLeaderboardsClient(this);
        mEventsClient = PlayGames.getEventsClient(this);
        mPlayersClient = PlayGames.getPlayersClient(this);

        mPlayersClient.getCurrentPlayer().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Exception e = task.getException();
                if (e != null) {
                    handleException(e, getString(R.string.players_exception));
                }
            }
        });

        // if we have accomplishments to push, push them
        if (!isEmptyAchievementsOrLeaderboards()) {
            pushAccomplishments();
            updateLeaderboards();
        }
        loadAndPrintEvents();
    }

    public void onDisconnected() {
        mAchievementsClient = null;
        mLeaderboardsClient = null;
        mPlayersClient = null;
    }

    private void startSignInIntent() {
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    private boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(this) != null;
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

    public void unlockAchievement(int requestedTile, int rows) {
        // Check if each condition is met; if so, unlock the corresponding achievement.
        if (rows == 4) {
            if (requestedTile == 1024) mAchievement1024At4x4 = true;
            if (requestedTile == 2048) mAchievement2048At4x4 = true;
            if (requestedTile == 4096) mAchievement4096At4x4 = true;
            if (requestedTile == 8192) mAchievement8192At4x4 = true;
        } else if (rows == 5) {
            if (requestedTile == 2048) mAchievement2048At5x5 = true;
            if (requestedTile == 4096) mAchievement4096At5x5 = true;
            if (requestedTile == 8192) mAchievement8192At5x5 = true;
        } else if (rows == 6) {
            if (requestedTile == 2048) mAchievement2048At6x6 = true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        incrementGameCountAchievements();
    }

    public void incrementGameCountAchievements() {
        if (!isSignedIn()) {
            return;
        }
        mAchievementsClient.increment(getString(R.string.achievement_senior), 1);
        mAchievementsClient.increment(getString(R.string.achievement_fresher), 1);
    }

    private boolean isEmptyAchievementsOrLeaderboards() {
        return !mAchievement1024At4x4 || !mAchievement2048At4x4
                || !mAchievement4096At4x4 || !mAchievement8192At4x4 || !mAchievement2048At5x5 || !mAchievement4096At5x5
                || !mAchievement8192At5x5 || mAchievement2048At6x6 || mHighScore4x4 < 0 || mHighScore5x5 < 0 || mHighScore6x6 < 0;
    }
}