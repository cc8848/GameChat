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
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.appinvite.AppInvite;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.appinvite.AppInviteInvitationResult;
import com.google.android.gms.appinvite.AppInviteReferral;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.firebase.database.FirebaseDatabase;
import com.pajato.android.gamechat.BuildConfig;
import com.pajato.android.gamechat.R;
import com.pajato.android.gamechat.chat.fragment.ChatEnvelopeFragment;
import com.pajato.android.gamechat.common.InvitationManager;
import com.pajato.android.gamechat.common.model.Account;
import com.pajato.android.gamechat.credentials.CredentialsManager;
import com.pajato.android.gamechat.database.AccountManager;
import com.pajato.android.gamechat.database.DBUtils;
import com.pajato.android.gamechat.event.AppEventManager;
import com.pajato.android.gamechat.event.AuthenticationChangeEvent;
import com.pajato.android.gamechat.event.ClickEvent;
import com.pajato.android.gamechat.event.GroupJoinedEvent;
import com.pajato.android.gamechat.event.MenuItemEvent;
import com.pajato.android.gamechat.event.NavDrawerOpenEvent;
import com.pajato.android.gamechat.exp.fragment.ExpEnvelopeFragment;
import com.pajato.android.gamechat.intro.IntroActivity;

import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.pajato.android.gamechat.database.AccountManager.ACCOUNT_AVAILABLE_KEY;
import static java.security.AccessController.getContext;

/**
 * Provide a main activity to display the chat and game fragments.
 *
 * @author Paul Michael Reilly
 */
public class MainActivity extends BaseActivity
    implements View.OnClickListener, NavigationView.OnNavigationItemSelectedListener, GoogleApiClient.OnConnectionFailedListener {

    // Public class constants.

    /** ... */
    public static final String SKIP_INTRO_ACTIVITY_KEY = "skipIntroActivityKey";

    /** The test user name key. */
    public static final String TEST_USER_KEY = "testUserKey";

    /** The invite activity request code. */
    public static final int RC_INVITE = 2;

    // Private class constants.

    /** The logcat tag constant. */
    private static final String TAG = MainActivity.class.getSimpleName();

    /** The preferences file name. */
    private static final String PREFS = "GameChatPrefs";

    /** The Intro activity request code. */
    private static final int RC_INTRO = 1;

    // Public instance methods

    /** Handle an account state change by updating the navigation drawer header. */
    @Subscribe public void onAuthenticationChange(final AuthenticationChangeEvent event) {
        // Due to a "bug" in Android, using XML to configure the navigation header current profile
        // click handler does not work.  Instead we do it here programmatically.
        Account account = event != null ? event.account : null;
        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        View header = navView.getHeaderView(0) != null
            ? navView.getHeaderView(0)
            : navView.inflateHeaderView(R.layout.nav_header_main);
        View layout = header.findViewById(R.id.currentProfile);
        if (layout != null) layout.setOnClickListener(this);
        NavigationManager.instance.setAccount(account, header);
    }

    /** Handle group joined event */
    @Subscribe public void onGroupJoined(final GroupJoinedEvent event) {
        if (event.groupNames.size() > 1) {
            String msg = getString(R.string.JoinedMultiGroupsMessage);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        } else {
            String format = getString(R.string.JoinedGroupsMessage);
            String message = String.format(Locale.US, format, event.groupNames.get(0));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    /** Handle a back button press event delivered by the system. */
    @Override public void onBackPressed() {
        if (NavigationManager.instance.closeDrawerIfOpen(this)) return;
        super.onBackPressed();
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
                AppEventManager.instance.post(new NavDrawerOpenEvent(this, null));
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
        String format =  "Navigation Item Selected on view: {%s}";
        Log.v(TAG, String.format(Locale.US, format, item.getClass().getSimpleName()));
        AppEventManager.instance.post(new NavDrawerOpenEvent(this, item));
        return true;
    }

    /** Setup the standard set of activty menu items. */
    @Override public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the main options menu and enable the join developer groups item in debug builds.
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        if (BuildConfig.DEBUG) {
            MenuItem item = menu.findItem(R.id.joinDeveloperGroups);
            if (item != null) item.setVisible(true);
        }
        return true;
    }

    /** Handle a menu item click by providing a last ditch chance to do something. */
    @Subscribe public void onMenuItem(final MenuItemEvent event) {
        // Case on the menu id to handle the item.
        switch (event.item.getItemId()) {
            case R.id.helpAndFeedback:
                SupportManager.instance.sendFeedback(this, "GameChat Feedback");
                AppEventManager.instance.cancel(event);
                break;
            case R.id.fileBugReport:
                handleBugReport(event);
                break;
            default:
                // Handle all other events by logging a message for now.
                final String format = "Default handling for menu item with title: {%s}";
                Log.d(TAG, String.format(Locale.US, format, event.item.getTitle()));
                break;
        }
    }

    /** Post the menu item click to the app. */
    @Override public boolean onOptionsItemSelected(final MenuItem item) {
        AppEventManager.instance.post(new MenuItemEvent(item));
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
        } else if (requestCode == RC_INVITE && resultCode == RESULT_OK) {
            // For now, just log
            Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
            // Get the invitation IDs of all sent messages
            String[] ids = AppInviteInvitation.getInvitationIds(resultCode, intent);
            for (String id : ids) {
                Log.d(TAG, "onActivityResult: sent invitation " + id);
            }
        }
    }

    public void onConnectionFailed (@NonNull ConnectionResult result) {
        Log.i(TAG, "connection failed: " + result.toString());
    }

    /** Set up the app per the characteristics of the running device. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Deal with signin, set up the main layout, and initialize the app.
        super.onCreate(savedInstanceState);

        signIn();
        setContentView(R.layout.activity_main);
        init();

        // Build GoogleApiClient with AppInvite API for receiving deep links
        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(AppInvite.API)
                .build();

        // Check if this app was launched from a deep link. Setting autoLaunchDeepLink to true
        // would automatically launch the deep link if one is found.
        final boolean autoLaunchDeepLink = false;
        AppInvite.AppInviteApi.getInvitation(mGoogleApiClient, this, autoLaunchDeepLink)
                .setResultCallback(InvitationManager.instance);
//                        new ResultCallback<AppInviteInvitationResult>() {
//                            @Override
//                            public void onResult(@NonNull AppInviteInvitationResult result) {
//                                Log.i(TAG, "getInvitation with autoLaunchDeepLink=" + autoLaunchDeepLink + " -- onResult:" + result.getStatus());
//                                Log.i(TAG, "getInvitation intent=" + result.getInvitationIntent());
//                                Intent i = result.getInvitationIntent();
//                                if(i != null) {
//                                    Log.i(TAG, "extras: " + i.getExtras().toString());
//                                }
//                                if (result.getStatus().isSuccess()) {
//                                    // Extract deep link from Intent
//                                    Intent intent = result.getInvitationIntent();
//                                    String deepLink = AppInviteReferral.getDeepLink(intent);
//                                    Log.i(TAG, "getInvitation with deepLink: " + deepLink);
//                                    String invitationId = AppInviteReferral.getInvitationId(intent);
//                                    Log.i(TAG, "getInvitation: invitationId=" + invitationId);
//                                    boolean hasRef = AppInviteReferral.hasReferral(intent);
//                                    Log.i(TAG, "getInvitation: invitation has a referral=" + hasRef);
//                                    boolean isFromPlayStore = AppInviteReferral.isOpenedFromPlayStore(intent);
//                                    Log.i(TAG, "getInvitation: launched after install from play store=" + isFromPlayStore);
//                                    // If we have a deep link, try to find the groupKey.
//                                    if(deepLink != null && !deepLink.equals("")) {
//                                        Uri dlUri = Uri.parse(deepLink);
//                                        String firebaseLink = dlUri.getQueryParameter("link");
//                                        if(firebaseLink != null && !firebaseLink.equals("")) {
//                                            Uri fbUri = Uri.parse(firebaseLink);
//                                            List<String> parts = fbUri.getPathSegments();
//                                            // Get the last value which should be the group key
//                                            String groupKey = parts.get(parts.size() - 1);
//                                            Log.i(TAG, "getInvitation: groupKey=" + groupKey);
////                                            Account currAccount = AccountManager.instance.getCurrentAccount();
//                                            groupKeysToJoin.add(groupKey);
////                                            DBUtils.instance.updateChildren(
////                                                    AccountManager.instance.getAccountPath(currAccount.id),
////                                                    currAccount.toMap());
//                                        } else {
//                                            Log.i(TAG, "getInvitation: can't get group key - firebaseLink is not set");
//                                        }
//                                    } else {
//                                        Log.i(TAG, "getInvitation: can't get group key - deepLink is not set");
//                                    }
//
//                                } else {
//                                    Log.i(TAG, "getInvitation: no deep link found.");
//                                }
//                            }
//                        });

    }

    // Private instance methods.

    /** Return "about" information: a string describing the app and it's version information. */
    private String getAbout() {
        final String format = "GameChat %s-%d Bug Report";
        final String name = BuildConfig.VERSION_NAME;
        final int code = BuildConfig.VERSION_CODE;
        return String.format(Locale.US, format, name, code);
    }

    /** Return null if the given bitmap cannot be saved or the file path it has been saved to. */
    private String getBitmapPath(final Bitmap bitmap) {
        // Create the image file on internal storage.  Abort if the subdirectories cannot be
        // created.
        FileOutputStream outputStream;
        File dir = new File(getFilesDir(), "images");
        if (!dir.exists() && !dir.mkdirs()) return null;

        // Flush the bitmap to the image file as a stream and return the result.
        File imageFile = new File(dir, "screenshot.png");
        Log.d(TAG, String.format(Locale.US, "Image file path is {%s}", imageFile.getPath()));
        try {
            outputStream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
        } catch (IOException exc) {
            Log.e(TAG, exc.getMessage(), exc);
            return null;
        }

        return imageFile.getPath();
    }

    /** Handle a bug report by performing a screen capture, grabbing logcat and sending email. */
    private void handleBugReport(final MenuItemEvent event) {
        // Capture the screen (with any luck, sans menu.), send the message and cancel event
        // propagation.
        View rootView = getWindow().getDecorView().getRootView();
        rootView.setDrawingCacheEnabled(true);
        List<String> attachments = new ArrayList<>();
        String path = getBitmapPath(rootView.getDrawingCache());
        if (path != null) attachments.add(path);
        path = getLogcatPath();
        if (path != null) attachments.add(path);
        SupportManager.instance.sendFeedback(this, getAbout(), "Extra information: ", attachments);
        AppEventManager.instance.cancel(event);
    }

    /** Initialize the main activity and all of it's subsystems. */
    private void init() {
        // Set up the account manager with a list of class names.  These classes must be registered
        // with the app event manager before Firebase can be enabled.  And when any of them are
        // unregistered, Firebase will be turned off.
        List<String> list = new ArrayList<>();
        list.add(this.getClass().getName());
        list.add(ChatEnvelopeFragment.class.getName());
        list.add(ExpEnvelopeFragment.class.getName());
        AccountManager.instance.init(list);
        CredentialsManager.instance.init(getSharedPreferences(PREFS, Context.MODE_PRIVATE));

        // Finish initializing the important manager modules.
        DBUtils.instance.init(this);
        NetworkManager.instance.init(this);
        PaneManager.instance.init(this);
        NavigationManager.instance.init(this, (Toolbar) findViewById(R.id.toolbar));
    }

    /** Return the file where logcat data has been placed, null if no data is available. */
    private String getLogcatPath() {
        // Capture the current state of the logcat file.
        File dir = new File(getFilesDir(), "logcat");
        if (!dir.exists() && !dir.mkdirs()) return null;

        File outputFile = new File(dir, "logcat.txt");
        try {
            Runtime.getRuntime().exec("logcat -f " + outputFile.getAbsolutePath());
        } catch (IOException exc) {
            Log.e(TAG, exc.getMessage(), exc);
            return null;
        }

        Log.d(TAG, String.format("File size is %d.", outputFile.length()));
        Log.d(TAG, String.format("File path is {%s}.", outputFile.getPath()));

        return outputFile.getPath();
    }

//    /** Extend an invitation to join GameChat using AppInviteInvitation Intent */
//    public void extendAppInvitation(String groupKey) {
//        Log.i(TAG, "extendAppInvitation with groupKey=" + groupKey);
//        String firebaseUrl = FirebaseDatabase.getInstance().getReference().toString();
//        firebaseUrl += "/groups/";
//        if(groupKey == null || groupKey.equals("")) {
//            firebaseUrl += AccountManager.instance.getMeGroup();
//            Log.i(TAG, "extendAppInvitation: " + firebaseUrl);
//        } else {
//            firebaseUrl += groupKey;
//        }
//
//        String APP_CODE = "aq5ca";
//        String PLAY_STORE_LINK = "https://play.google.com/apps/testing/com.pajato.android.gamechat";
//        String APP_PACKAGE_NAME = "com.pajato.android.gamechat";
//        String WEB_LINK = "https://github.com/pajato/GameChat";
//
//        String dynamicLink = new Uri.Builder()
//                .scheme("https")
//                .authority(APP_CODE + ".app.goo.gl")
//                .path("/")
//                .appendQueryParameter("link", firebaseUrl)
//                .appendQueryParameter("apn", APP_PACKAGE_NAME)
//                .appendQueryParameter("afl", PLAY_STORE_LINK)
//                .appendQueryParameter("ifl", WEB_LINK).toString();
//
//        Log.i(TAG, "dynamicLink=" + dynamicLink);
//        Intent intent = new AppInviteInvitation.IntentBuilder(getString(R.string.InviteTitle))
//                .setMessage(getString(R.string.InviteMessage))
//                .setDeepLink(Uri.parse(dynamicLink))
//                .build();
//        startActivityForResult(intent, RC_INVITE);
//    }

    /** Handle sign in by processing the invoking intent and checking for an existing account. */
    private void signIn() {
        // Determine if the calling intent prefers not to run the intro activity (for example
        // during a connected test run) or if there is an available account.  Abort in either case.
        Intent intent = getIntent();
        boolean skipIntro = intent.hasExtra(SKIP_INTRO_ACTIVITY_KEY);
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        boolean hasAccount = prefs.getBoolean(ACCOUNT_AVAILABLE_KEY, false);
        if (skipIntro || hasAccount) return;

        // This is a fresh installation of the app.  Present the intro activity to get
        // things started, which will introduce the user to the app and provide a chance to
        // sign in.
        Intent introIntent = new Intent(this, IntroActivity.class);
        startActivityForResult(introIntent, RC_INTRO);
    }
}
