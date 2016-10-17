/*
 * Copyright (C) 2016 Pajato Technologies LLC.
 *
 * This file is part of Pajato GameChat.

 * GameChat is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.

 * GameChat is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.

 * You should have received a copy of the GNU General Public License along with GameChat.  If not,
 * see http://www.gnu.org/licenses
 */

package com.pajato.android.gamechat.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.pajato.android.gamechat.R;
import com.pajato.android.gamechat.account.Account;
import com.pajato.android.gamechat.account.AccountManager;
import com.pajato.android.gamechat.database.DatabaseListManager;
import com.pajato.android.gamechat.database.DatabaseManager;
import com.pajato.android.gamechat.database.DatabaseRegistrar;
import com.pajato.android.gamechat.event.AccountStateChangeEvent;
import com.pajato.android.gamechat.event.AppEventManager;
import com.pajato.android.gamechat.event.BackPressEvent;
import com.pajato.android.gamechat.event.ClickEvent;
import com.pajato.android.gamechat.event.NavDrawerOpenEvent;
import com.pajato.android.gamechat.game.GameManager;
import com.pajato.android.gamechat.intro.IntroActivity;

import org.greenrobot.eventbus.Subscribe;

import java.util.Locale;

import static com.pajato.android.gamechat.account.AccountManager.ACCOUNT_AVAILABLE_KEY;

/**
 * Provide a main activity to display the chat and game fragments.
 *
 * @author Paul Michael Reilly
 */
public class MainActivity extends BaseActivity
    implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {

    // Public class constants.

    /** The intent extras key conveying the desire to skip the intro activity. */
    public static final String SKIP_INTRO_ACTIVITY_KEY = "skipIntroActivityKey";

    // Private class constants.

    /** The logcat tag constant. */
    private static final String TAG = MainActivity.class.getSimpleName();

    /** The preferences file name. */
    private static final String PREFS = "GameChatPrefs";

    /** The Intro activity request code. */
    private static final int RC_INTRO = 1;

    // Public instance methods

    /** Handle an account state change by updating the navigation drawer header. */
    @Subscribe public void accountStateChanged(final AccountStateChangeEvent event) {
        // Due to a "bug" in Android, using XML to configure the navigation header current profile
        // click handler does not work.  Instead we do it here programmatically.
        Account account = event != null ? event.account : null;
        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        View header = navView.getHeaderView(0) != null
            ? navView.getHeaderView(0)
            : navView.inflateHeaderView(R.layout.nav_header_main);
        View layout = header.findViewById(R.id.currentProfile);
        if (layout != null) layout.setOnClickListener(this);

        // If there is an account, set up the navigation drawer header accordingly.
        if (account != null) {
            // There is an account.  Set it up in the header.
            NavigationManager.instance.setAccount(account, header);
        } else {
            // There is no current user yet.  Provide the sign in button in the header.
            NavigationManager.instance.setNoAccount(header);
        }
    }

    /** Handle a back button press event posted by the app event manager. */
    @Subscribe public void onBackPressed(final BackPressEvent event) {
        // No other subscriber has handled the back press.  Let the system deal with it.
        super.onBackPressed();
    }

    /** Handle a back button press event delivered by the system. */
    @Override public void onBackPressed() {
        // If the navigation drawer is open, close it, otherwise let the system deal with it.
        AppEventManager.instance.post(new BackPressEvent(this));
    }

    /** Process a given button click event handling the nav drawer closing. */
    @Subscribe public void onClick(final ClickEvent event) {
        // Log all button clicks and rocess the sign in and sign out button clicks.
        View view = event.view;
        String format = "Button click event on view: {%s}.";
        Log.v(TAG, String.format(Locale.US, format, view.getClass().getSimpleName()));
        switch (view.getId()) {
            case R.id.currentProfile:
            case R.id.signIn:
            case R.id.signOut:
                // On a sign in or sign out event, make sure the navigation drawer gets closed.
                AppEventManager.instance.post(new NavDrawerOpenEvent(this));
                break;
            default:
                // Ignore everything else.
                break;
        }
    }

    /** Process a click on a given view by posting a button click event. */
    public void onClick(final View view) {
        // Use the Event bus to post the click event.
        AppEventManager.instance.post(new ClickEvent(view));
    }

    /** Process a navigation menu item click by posting a click event. */
    @Override public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        // Handle navigation view item clicks here by posting a click event and closing the drawer.
        switch (item.getItemId()) {
            case R.id.nav_manage_accounts:
            case R.id.nav_settings:
            case R.id.nav_feedback:
            case R.id.nav_learn:
                // Todo: add menu button handling as a future feature.
                break;
        }
        AppEventManager.instance.post(new NavDrawerOpenEvent(this));
        return true;
    }

    /** Setup the standard set of activty menu items. */
    @Override public boolean onCreateOptionsMenu(final Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    // Protected instance methods

    /** Process the result from the Intro activity. */
    @Override protected void onActivityResult(final int requestCode, final int resultCode,
                                              final Intent intent) {
        // Ensure that the request code and the result are valid.
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == RC_INTRO && resultCode == RESULT_OK) {
            // The request code is valid and the result is good.  Update the account available flag
            // based on the result from the intro activity intent data.
            SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String uid = intent.getStringExtra(Intent.EXTRA_TEXT);
            String key = ACCOUNT_AVAILABLE_KEY;
            editor.putBoolean(key, intent.getBooleanExtra(key, uid != null));
            editor.apply();
        }
    }

    /**
     * Set up the app per the characteristics of the running device.
     *
     * @see android.app.Activity#onCreate(Bundle)
     */
    @Override protected void onCreate(Bundle savedInstanceState) {
        // Allow superclasses to initialize using the saved state and determine if there has been a
        // fresh install on this device and proceed accordingly.
        super.onCreate(savedInstanceState);

        // Determine if the calling intent wants to skip the intro activity.
        Intent intent = getIntent();
        if (!intent.getBooleanExtra(SKIP_INTRO_ACTIVITY_KEY, false)) {
            // Do not skip running the intro screen activity.
            SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (!prefs.getBoolean(ACCOUNT_AVAILABLE_KEY, false)) {
                // This is a fresh installation of the app.  Present the intro activity to get things
                // started, which will introduce the user to the app and provide a chnance to sign in or
                // register an account.
                Intent introIntent = new Intent(this, IntroActivity.class);
                startActivityForResult(introIntent, RC_INTRO);
            }
        }

        // Deal with the main activity creation.  The account manager must be initialized last so
        // that all other managers are initialized prior to the first authentication event.  If a
        // manager misses an authentication event then the app will not behave as intended.
        setContentView(R.layout.activity_main);
        init();
    }

    /** Handle the imminent destruction of the app by shutting down the app event manager. */
    @Override protected void onDestroy() {
        super.onDestroy();
        AppEventManager.instance.unregisterAll();
    }

    /** Respect the lifecycle and ensure that the event bus shuts down. */
    @Override protected void onPause() {
        // Unregister the components directly used by the main activity which will unregister
        // sub-components in turn.
        super.onPause();

        // If and how this should be ordered is ill understood. :-()
        AccountManager.instance.unregister();
        DatabaseRegistrar.instance.unregisterAll();
    }

    /** Respect the lifecycle and ensure that the event bus spins up. */
    @Override protected void onResume() {
        // Register the components carefully as there are order sensitivities betwee the account and
        // database managers.
        super.onResume();
        AppEventManager.instance.register(this);
        AppEventManager.instance.register(DatabaseListManager.instance);
        AccountManager.instance.register();
    }

    // Private instance methods.

    /** Initialize the main activity and all of it's subsystems. */
    private void init() {
        // Set up the toolbar and the app managers: navigation, chat and game panes, accounts and
        // database, chat and chat list.
        ProgressManager.instance.show(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DatabaseManager.instance.init(this);
        NavigationManager.instance.init(this, toolbar);
        NetworkManager.instance.init(this);
        PaneManager.instance.init(this);
        GameManager.instance.init();
    }

}
