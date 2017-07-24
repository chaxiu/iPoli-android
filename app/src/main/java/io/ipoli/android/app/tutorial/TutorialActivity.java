package io.ipoli.android.app.tutorial;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.squareup.otto.Bus;

import javax.inject.Inject;

import io.ipoli.android.Constants;
import io.ipoli.android.R;
import io.ipoli.android.app.App;
import io.ipoli.android.app.events.FinishTutorialActivityEvent;
import io.ipoli.android.app.tutorial.events.RequestOverlayPermissionEvent;
import io.ipoli.android.app.tutorial.fragments.TutorialAddQuestFragment;
import io.ipoli.android.app.tutorial.fragments.TutorialCalendarFragment;
import io.ipoli.android.app.tutorial.fragments.TutorialIntroFragment;
import io.ipoli.android.app.tutorial.fragments.TutorialNamePromptFragment;
import io.ipoli.android.app.tutorial.fragments.TutorialOutroFragment;
import io.ipoli.android.app.utils.LocalStorage;
import io.ipoli.android.quest.data.Category;

/**
 * Created by Venelin Valkov <venelin@curiousily.com>
 * on 5/31/17.
 */
public class TutorialActivity extends AppCompatActivity {

    public static final int OVERLAY_PERMISSION_REQUEST_CODE = 42;
    @Inject
    LocalStorage localStorage;

    @Inject
    Bus eventBus;

    private String playerName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        App.getAppComponent(this).inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.root_container, new TutorialIntroFragment())
                .commit();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enterImmersiveMode();
        }
    }

    public void onIntroDone() {
        replaceFragment(new TutorialNamePromptFragment());
    }

    public void onNamePromptDone(String playerName) {
        this.playerName = playerName;
        replaceFragment(new TutorialAddQuestFragment());
    }

    public void onAddQuestDone(String name, Category category) {
        exitImmersiveMode();
        TutorialCalendarFragment calendarFragment = new TutorialCalendarFragment();
        calendarFragment.setQuestInfo(name, category);
        replaceFragment(calendarFragment);
    }

    public void exitImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }

    public void onCalendarDone() {
        enterImmersiveMode();
        TutorialOutroFragment outroFragment = new TutorialOutroFragment();
        outroFragment.setPlayerName(playerName);
        replaceFragment(outroFragment);
    }

    public void enterImmersiveMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private void replaceFragment(Fragment newFragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                .replace(R.id.root_container, newFragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        // intentional
    }

    public void onTutorialSkipped() {
        playerName = "";
        onTutorialDone();
    }

    public void onTutorialDone() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            eventBus.post(new RequestOverlayPermissionEvent(Build.VERSION.SDK_INT));
            Toast.makeText(getApplicationContext(), R.string.overlay_permission_reason, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
            return;
        }

        finishTutorial();
    }

    protected void finishTutorial() {
        localStorage.saveBool(Constants.KEY_SHOULD_SHOW_TUTORIAL, false);
        eventBus.post(new FinishTutorialActivityEvent(playerName));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            finishTutorial();
        }
    }
}