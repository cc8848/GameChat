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

package com.pajato.android.gamechat.database.handler;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.pajato.android.gamechat.account.Account;
import com.pajato.android.gamechat.chat.model.Room;
import com.pajato.android.gamechat.event.AppEventManager;
import com.pajato.android.gamechat.event.AccountChangeEvent;

/**
 * Provide a class to handle changes to an account by posting an app event.
 *
 * @author Paul Michael Reilly
 */
public class AccountChangeHandler extends DatabaseEventHandler implements ValueEventListener {

    // Public instance variables.

    /** The group for which this account is associated. */
    public String groupKey;

    // Private instance constants.

    /** The logcat TAG. */
    private final String TAG = AccountChangeHandler.class.getSimpleName();

    // Public constructors.

    /** Build a handler with the given name, path and key. */
    public AccountChangeHandler(final String name, final String path, final String key,
                                final String groupkey) {
        super(name, path, key);
        this.groupKey = groupKey;
    }

    /** Get the current generic profile. */
    @Override public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
        // Ensure that some data exists.
        if (dataSnapshot.exists()) {
            // There is data.  Publish the group profile to the app.
            Account account = dataSnapshot.getValue(Account.class);
            AppEventManager.instance.post(new AccountChangeEvent(key, groupKey, account));
        } else {
            Log.e(TAG, "Invalid key.  No value returned.");
        }
    }

    /** ... */
    @Override public void onCancelled(DatabaseError error) {
        // Failed to read value
        Log.w(TAG, "Failed to read value.", error.toException());
    }
}
