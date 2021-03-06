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

package com.pajato.android.gamechat.database;

import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.pajato.android.gamechat.database.handler.DatabaseEventHandler;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Provide a class to handle database listener/handler registrations.
 *
 * @author Paul Michael Reilly
 */
public enum DatabaseRegistrar {
    instance;

    // Private class constants.

    // The logcat tag.
    private static final String TAG = DatabaseRegistrar.class.getSimpleName();

    // Public instance variables.

    /** The Firebase value event listener map. */
    private Map<String, DatabaseEventHandler> mHandlerMap = new HashMap<>();

    // Public instance methods.

    /** Return TRUE iff the a handler with the given name is registered. */
    public boolean isRegistered(String name) {
        return mHandlerMap.containsKey(name);
    }

    /** Register a given value event listener. */
    public void registerHandler(final DatabaseEventHandler handler) {
        // Remove any previously registered handlers of the same name.
        String name = handler.name;
        Log.d(TAG, String.format(Locale.US, "Registering handler with name {%s}.", name));
        DatabaseReference database = FirebaseDatabase.getInstance().getReference(handler.path);
        DatabaseEventHandler registeredHandler = mHandlerMap.get(name);
        removeEventListener(database, registeredHandler);

        // Register the new listener both with the handler map and with Firebase.
        mHandlerMap.put(name, handler);
        ValueEventListener valueEventListener = handler instanceof ValueEventListener ?
            (ValueEventListener) handler : null;
        ChildEventListener childEventListener = handler instanceof ChildEventListener ?
            (ChildEventListener) handler : null;
        mHandlerMap.put(name, handler);
        if (valueEventListener != null) {
            database.addValueEventListener(valueEventListener);
        }
        if (childEventListener != null) {
            database.addChildEventListener(childEventListener);
        }
    }

    /** Unregister all listeners. */
    public void unregisterAll() {
        // Walk the set of registered handlers to remove them.  Leave them cached for possible reuse.
        DatabaseReference database;
        DatabaseEventHandler handler;
        Log.d(TAG, "Unregister all handlers.");
        for (String name : mHandlerMap.keySet()) {
            handler = mHandlerMap.get(name);
            database = FirebaseDatabase.getInstance().getReference(handler.path);
            removeEventListener(database, handler);
        }
        mHandlerMap.clear();
    }

    /** Unregister a named listener. */
    public void unregisterHandler(final String name) {
        // Determine if there is a handler registered by the given name.
        Log.d(TAG, String.format(Locale.US, "Unregister handler with name {%s}.", name));
        DatabaseEventHandler handler = mHandlerMap.get(name);
        if (handler != null) {
            // There is.  Remove it as a listener but keep it cached for possible reuse.
            DatabaseReference database = FirebaseDatabase.getInstance().getReference(handler.path);
            removeEventListener(database, handler);
            mHandlerMap.remove(name);
        }
    }

    // Private instance methods.

    /** Remove the database event listener, if any is found associated with the given handler. */
    private void removeEventListener(final DatabaseReference database,
                                     final DatabaseEventHandler handler) {
        ValueEventListener valueEventListener;
        ChildEventListener childEventListener;
        if (handler != null) {
            // There is a handler found.  Remove it.
            if (handler instanceof ValueEventListener) {
                valueEventListener = (ValueEventListener) handler;
                database.removeEventListener(valueEventListener);
            } else if (handler instanceof ChildEventListener) {
                childEventListener = (ChildEventListener) handler;
                database.removeEventListener(childEventListener);
            } else {
                // Remove the funky entry, after logging it.
                String format = "Removing an invalid event listener, %s/%s (name/type).";
                String name = handler.name;
                String type = handler.getClass().getSimpleName();
                Log.e(TAG, String.format(Locale.getDefault(), format, name, type));
            }
        }
    }

}
