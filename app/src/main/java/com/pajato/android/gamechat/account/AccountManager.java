/*
 * Copyright (C) 2016 Pajato Technologies, Inc.
 *
 * This file is part of Pajato GameChat.

 * GameChat is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * GameChat is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.

 * You should have received a copy of the GNU General Public License along with GameChat. If not,
 * see <http://www.gnu.org/licenses/>.
 */

package com.pajato.android.gamechat.account;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.pajato.android.gamechat.R;
import com.pajato.android.gamechat.database.handler.DatabaseEventHandler;
import com.pajato.android.gamechat.database.DatabaseManager;
import com.pajato.android.gamechat.database.DatabaseRegistrar;
import com.pajato.android.gamechat.event.AccountStateChangeEvent;
import com.pajato.android.gamechat.event.AccountStateChangeHandled;
import com.pajato.android.gamechat.event.AppEventManager;
import com.pajato.android.gamechat.event.ClickEvent;
import com.pajato.android.gamechat.event.RegistrationChangeEvent;
import com.pajato.android.gamechat.signin.SignInActivity;

import org.greenrobot.eventbus.Subscribe;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.pajato.android.gamechat.account.Account.STANDARD;
import static com.pajato.android.gamechat.event.RegistrationChangeEvent.REGISTERED;

/**
 * Manages the account related aspects of the GameChat application.  These include setting up the
 * first time sign-in result, creating and persisting a profile on this device and switching
 * accounts.
 *
 * @author Paul Michael Reilly
 */
public enum AccountManager implements FirebaseAuth.AuthStateListener {
    instance;

    // Public class constants.

    /** A key used to access account available data. */
    public static final String ACCOUNT_AVAILABLE_KEY = "accountAvailable";

    /** The sentinel value to use for indicating an offline cached database object owner. */
    public static final String SIGNED_OUT_OWNER_ID = "signedOutOwnerId";

    /** The sentinel value to use for indicating a signed out experience key. */
    public static final String SIGNED_OUT_EXPERIENCE_KEY = "signedOutExperienceKey";

    // Private class constants.

    /** The logcat tag. */
    private static final String TAG = AccountManager.class.getSimpleName();

    // Private instance variables

    /** The account repository associating mulitple account id strings with the cloud account. */
    private Map<String, Account> mAccountMap = new HashMap<>();

    /** The current account key, null if there is no current account. */
    private String mCurrentAccountKey;

    /** A flag indicating that Firebase is enabled (registered) or not. */
    private boolean mIsFirebaseEnabled;

    /** A map tracking registrations from the key classed that are needed to enable Firebase. */
    private Map<String, Boolean> mRegistrationClassNameMap = new HashMap<>();

    // Public instance methods

    /** Retrun the account for the current User, null if there is no signed in User. */
    public Account getCurrentAccount() {
        return mCurrentAccountKey == null ? null : mAccountMap.get(mCurrentAccountKey);
    }

    /** Retrun the account for the current User, null if there is no signed in User. */
    public Account getCurrentAccount(final Context context) {
        // Determine if there is a logged in account.  If so, return it.
        if (mCurrentAccountKey != null) return mAccountMap.get(mCurrentAccountKey);

        // The User is not signed in.  Prompt them to do so now.
        String text = "Not logged in!  Please sign in.";
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
        return null;
    }

    /** Return the current account id, null if there is no curent signed in User. */
    public String getCurrentAccountId() {
        return mCurrentAccountKey;
    }

    /** Return a joined room entry, well formed (space separated) group key and room key pair. */
    public String getJoinedRoomEntry(final String groupKey, final String roomKey) {
        return String.format(Locale.US, "%s %s", groupKey, roomKey);
    }

    /** Obtain a suitable Uri to use for the User's icon. */
    public String getPhotoUrl(FirebaseUser user) {
        // TODO: figure out how to handle a generated icon ala Inbox, Gmail and Hangouts.
        Uri icon = user.getPhotoUrl();
        return icon != null ? icon.toString() : null;
    }

    /** Return TRUE iff there is a signed in User. */
    public boolean hasAccount() {
        return getCurrentAccount() != null;
    }

    /** Handle initialization by setting up the Firebase required list of registered classes. */
    public void init(final List<String> classNameList) {
        // Setup the class map that drives enabling and disabling Firebase. Each of these classes
        // must be registered with the app event manager before Firebase can be enabled.  If any are
        // deregistered, Firebase will be disabled.
        for (String name : classNameList) {
            mRegistrationClassNameMap.put(name, false);
        }
    }

    /** Deal with authentication backend changes: sign in and sign out */
    @Override public void onAuthStateChanged(@NonNull final FirebaseAuth auth) {
        // Determine if this state represents a User signing in or signing out.
        String name = "accountStateChangeHandler";
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            // A User has signed in. Set up a database listener for the associated account.  That
            // listener will post an account change event with the account information to the app.
            String path = String.format("/accounts/%s", user.getUid());
            if (!(DatabaseRegistrar.instance.isRegistered(name))) {
                DatabaseRegistrar.instance.registerHandler(new AccountChangeHandler(name, path));
            }
        } else {
            // The User is signed out.  Clear the current account key and notify the app of the sign
            // out event.
            mCurrentAccountKey = null;
            if (DatabaseRegistrar.instance.isRegistered(name)) {
                DatabaseRegistrar.instance.unregisterHandler(name);
                AppEventManager.instance.post(new AccountStateChangeEvent(null));
                AppEventManager.instance.post(new AccountStateChangeHandled(null));
            }
        }
    }

    /** Handle a sign in or sign out button click. */
    @Subscribe public void onClick(final ClickEvent event) {
        // Determine if there was a button click on a view.  If not, abort (since it is a menu item
        // that is not interesting here.)
        View view = event.view;
        if (view == null) return;

        // Handle the button click if it is either a sign-in or a sign-out.
        switch (view.getId()) {
            case R.id.signIn:
            case R.id.expSignIn:
                // Invoke the sign in activity to kick off a Firebase auth event.
                Context context = view.getContext();
                Intent intent = new Intent(context, SignInActivity.class);
                intent.putExtra("signin", true);
                context.startActivity(intent);
                break;
            case R.id.signOut:
                // Have Firebase log out the user.
                FirebaseAuth.getInstance().signOut();
                break;
            default:
                break;
        }
    }

    /** Handle a registration event by enabling and/or disabling Firebase, as necessary. */
    @Subscribe public void onRegistrationChange(final RegistrationChangeEvent event) {
        // Determin if this is a relevant registration event.
        if (!mRegistrationClassNameMap.containsKey(event.name)) return;

        // The event is of interest. Update the map and determine if Firebase needs to be enabled or disabled.
        mRegistrationClassNameMap.put(event.name, event.changeType == REGISTERED);
        boolean enable = true;
        for (Boolean value : mRegistrationClassNameMap.values()) {
            if (!value) {
                enable = false;
                break;
            }
        }

        // Determine if Firebase should be disabled or enabled.
        if (enable)
            register();
        else
            unregister();
    }

    // Private instance methods.

    /** Register the account manager with the database. */
    private void register() {
        if (!mIsFirebaseEnabled) {
            Log.d(TAG, "Registering the account manager with Firebase.");
            FirebaseAuth.getInstance().addAuthStateListener(this);
            mIsFirebaseEnabled = true;
        }
    }

    /** Handle a sign in with the given credentials. */
    public void signIn(final Activity activity, final String login, final String pass) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        SignInCompletionHandler handler = new SignInCompletionHandler(activity, login);
        auth.signInWithEmailAndPassword(login, pass).addOnCompleteListener(activity, handler);
    }

    /** Unregister the component during lifecycle pause events. */
    private void unregister() {
        if (mIsFirebaseEnabled) {
            Log.d(TAG, "Unregistering the AccountManager from Firebase.");
            FirebaseAuth.getInstance().removeAuthStateListener(this);
            mIsFirebaseEnabled = false;
        }
    }

    // Private classes

    /** Provide a class to handle account changes. */
    private class AccountChangeHandler extends DatabaseEventHandler implements ValueEventListener {

        // Private instance constants.

        /** The logcat TAG. */
        private final String TAG = this.getClass().getSimpleName();

        // Public constructors.

        /** Build a handler with a given top level layout view. */
        AccountChangeHandler(final String name, final String path) {
            super(name, path);
        }

        /** Get the current account using a list of account identifiers. */
        @Override public void onDataChange(final DataSnapshot dataSnapshot) {
            // Determine if the account exists.
            Account account;
            if (dataSnapshot.exists()) {
                // It does.  Register it and notify the app that this is the new account of record.
                account = dataSnapshot.getValue(Account.class);
                mAccountMap.put(account.id, account);
                mCurrentAccountKey = account.id;
            } else {
                // The account does not exist.  Create it now, ensuring there really is a User.
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                account = getAccount(user);
                if (user != null) DatabaseManager.instance.createAccount(account);
            }
            AppEventManager.instance.post(new AccountStateChangeEvent(account));
            AppEventManager.instance.post(new AccountStateChangeHandled(account));
        }

        /** ... */
        @Override public void onCancelled(DatabaseError error) {
            // Failed to read value
            Log.w(TAG, "Failed to read value.", error.toException());
        }

        // Private instance methods.

        /** Return a partially populated account. The database manager will finish the job. */
        private Account getAccount(final FirebaseUser user) {
            long tstamp = new Date().getTime();
            Account account = new Account();
            account.id = user.getUid();
            account.email = user.getEmail();
            account.displayName = user.getDisplayName();
            account.url = getPhotoUrl(user);
            account.providerId = user.getProviderId();
            account.type = STANDARD;
            account.createTime = tstamp;

            return account;
        }
    }

    private class SignInCompletionHandler implements OnCompleteListener<AuthResult> {

        Activity mActivity;
        String mLogin;

        SignInCompletionHandler(final Activity activity, final String login) {
            mActivity = activity;
            mLogin = login;
        }

        @Override public void onComplete(@NonNull Task<AuthResult> task) {
            if (!task.isSuccessful()) {
                // Log and report a signin error.
                String format = mActivity.getString(R.string.SignInFailedFormat);
                String message = String.format(Locale.US, format, mLogin);
                Log.d(TAG, message);
                Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
            }
        }
    }
}
