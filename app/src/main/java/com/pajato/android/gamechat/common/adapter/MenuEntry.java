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

package com.pajato.android.gamechat.common.adapter;

import com.pajato.android.gamechat.common.FragmentType;

import java.util.Locale;

/**
 * Provide a POJO to encapsulate a recycler view list item: a FAB menu item.
 *
 * @author Paul Michael Reilly
 */
public class MenuEntry {

    // Type constants.
    public final static int MENU_ITEM_NO_TINT_TYPE = 0;
    public final static int MENU_ITEM_TINT_TYPE = 1;
    final static int MENU_HEADER_TYPE = 2;

    // Public and package private instance variables.

    /** A description of the item. */
    private String mDescription;

    /** The fragment type associated with the menu item, if any. */
    public FragmentType fragmentType;

    /** The menu item icon resource id. */
    int iconResId;

    /** The list of rooms or groups with messages to show, or the text of a message. */
    public int titleResId;

    /** The entry type, provided by the item. */
    public int type;

    /** The URL for the item, possibly null, used for icons with contacts and chat list items. */
    public String url;

    // Public constructors.

    /** Build an instance for a given menu item. */
    public MenuEntry(final MenuItemEntry entry) {
        type = entry.type;
        iconResId = entry.iconResId;
        titleResId = entry.titleResId;
        fragmentType = entry.fragmentType;
        url = entry.url;
        String format = "Menu item with title resource id {%s} and url {%s}.";
        mDescription = String.format(Locale.US, format, titleResId, url);
    }

    /** Build an instance for a given menu item. */
    public MenuEntry(final MenuHeaderEntry entry) {
        type = MENU_HEADER_TYPE;
        titleResId = entry.titleResId;
        String format = "Menu header with title resource id {%s}.";
        mDescription = String.format(Locale.US, format, titleResId);
    }

    // Public instance methods.

    /** Return a description of the object. */
    @Override public String toString() {
        return mDescription;
    }

}
