package com.pridhi.twoZeroFourEightEraser;

import android.app.Activity;
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
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.GamesSignInClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.PlayGames;

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

    public InterstitialAd mInterstitialAd;
    private RewardedAd mRewardedAd;

    private static long mHighScore4x4;
    private static long mHighScore5x5;
    private static long mHighScore6x6;

    private boolean mAchievement8192At4x4;
    private boolean mAchievement4096At4x4;
    private boolean mAchievement2048At4x4;
    private boolean mAchievement1024At4x4;
    private boolean mAchievement8192At5x5;
    private boolean mAchievement4096At5x5;
    private boolean mAchievement2048At5x5;
    private boolean mAchievement2048At6x6;

    public GamesSignInClient mGamesSignInClient;
    private boolean isAuthenticated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        FrameLayout frameLayout = findViewById(R.id.game_frame_layout);
        view = new MainView(this, this);

        if (savedInstanceState != null)
            if (savedInstanceState.getBoolean("hasState"))
                load();

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(params);

        frameLayout.addView(view);

        MobileAds.initialize(this, initializationStatus -> {
        });

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        loadInterstitial();
        loadRewarded();
        mGamesSignInClient = PlayGames.getGamesSignInClient(this);
        mGamesSignInClient.isAuthenticated().addOnCompleteListener(isAuthenticatedTask -> isAuthenticated =
                (isAuthenticatedTask.isSuccessful() &&
                        isAuthenticatedTask.getResult().isAuthenticated()));
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

        pushAccomplishments();
        updateLeaderboards();
    }

    protected void onResume() {
        super.onResume();
        load();

        pushAccomplishments();
        updateLeaderboards();
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

    public void pushAccomplishments() {
        if (!isAuthenticated) {
            return;
        }
        try {
            AchievementsClient achievementsClient = PlayGames.getAchievementsClient(this);
            if (mAchievement8192At4x4) {
                achievementsClient.unlock(getString(R.string.achievement_8192_at_4x4));
                mAchievement8192At4x4 = false;
            }

            if (mAchievement4096At4x4) {
                achievementsClient.unlock(getString(R.string.achievement_4096_at_4x4));
                mAchievement4096At4x4 = false;
            }

            if (mAchievement2048At4x4) {
                achievementsClient.unlock(getString(R.string.achievement_2048_at_4x4));
                mAchievement2048At4x4 = false;
            }

            if (mAchievement1024At4x4) {
                achievementsClient.unlock(getString(R.string.achievement_1024_at_4x4));
                mAchievement1024At4x4 = false;
            }

            if (mAchievement8192At5x5) {
                achievementsClient.unlock(getString(R.string.achievement_8192_at_5x5));
                mAchievement8192At5x5 = false;
            }

            if (mAchievement4096At5x5) {
                achievementsClient.unlock(getString(R.string.achievement_4096_at_5x5));
                mAchievement4096At5x5 = false;
            }

            if (mAchievement2048At5x5) {
                achievementsClient.unlock(getString(R.string.achievement_2048_at_5x5));
                mAchievement2048At5x5 = false;
            }

            if (mAchievement2048At6x6) {
                achievementsClient.unlock(getString(R.string.achievement_2048_at_6x6));
                mAchievement2048At6x6 = false;
            }
        } catch (Exception e) {
            Log.e("PushAccomplishments", Arrays.toString(e.getStackTrace()));
        }
    }

    private void updateLeaderboards() {
        if (!isAuthenticated) {
            return;
        }
        try {
            LeaderboardsClient leaderboardsClient = PlayGames.getLeaderboardsClient(this);
            if (mHighScore4x4 >= 0) {
                leaderboardsClient.submitScore(getString(R.string.leaderboard_4x4), mHighScore4x4);
                mHighScore4x4 = -1;
            }

            if (mHighScore5x5 >= 0) {
                leaderboardsClient.submitScore(getString(R.string.leaderboard_5x5), mHighScore5x5);
                mHighScore5x5 = -1;
            }

            if (mHighScore6x6 >= 0) {
                leaderboardsClient.submitScore(getString(R.string.leaderboard_6x6), mHighScore6x6);
                mHighScore6x6 = -1;
            }
        } catch (Exception e) {
            Log.e("updateLeaderboards", Arrays.toString(e.getStackTrace()));
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
        if (isAuthenticated) {
            PlayGames.getAchievementsClient(this).increment(getString(R.string.achievement_senior), 1);
            PlayGames.getAchievementsClient(this).increment(getString(R.string.achievement_fresher), 1);
        }
    }

}