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

import android.app.ProgressDialog;
import android.content.Context;
import android.support.annotation.NonNull;

/** Provide a singleton to manage showing and hiding the initial loading status. */
public enum ProgressManager {
    instance;

    // Private instance variables

    /** The view pager adapter used to manage paging on a smartphone layout. */
    private ProgressDialog mProgressDialog;

    // Public instance methods

    /** Show the initial loading dialog. */
    public void show(@NonNull final Context context) {
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setTitle("Loading...");
        mProgressDialog.setMessage("Please wait while the data is loaded...");
        mProgressDialog.show();
    }

    /** Dismiss the initial loading dialog. */
    public void hide() {
        mProgressDialog.dismiss();
        mProgressDialog = null;
    }
}
