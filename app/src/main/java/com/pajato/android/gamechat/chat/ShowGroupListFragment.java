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

package com.pajato.android.gamechat.chat;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import com.pajato.android.gamechat.R;
import com.pajato.android.gamechat.event.ClickEvent;
import com.pajato.android.gamechat.event.MessageListChangeEvent;

import org.greenrobot.eventbus.Subscribe;

import java.util.Locale;

/**
 * Provide a fragment to handle the display of the groups available to the current user.  This is
 * the top level view in the chat hierarchy.  It shows all the joined groups and allows for drilling
 * into rooms and chats within those rooms.
 *
 * @author Paul Michael Reilly
 */
public class ShowGroupListFragment extends BaseChatFragment {

    // Public instance methods.

    /** Process a given button click event looking for one on the chat fab button. */
    @Subscribe public void buttonClickHandler(final ClickEvent event) {
        // Assume that the button click is from a group view list item and give the base class a
        // chance to verify or refute the assumption.  If verified, the group view will drill into
        // the room view.
        processPayload(event.view);
    }

    /** Set the layout file. */
    @Override public int getLayout() {return R.layout.fragment_chat_groups;}

    /** Deal with the options menu by hiding the back button. */
    @Override public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        // Turn off the back option and turn on the search option.
        setOptionsMenu(menu, inflater, new int[] {R.id.search}, new int[] {R.id.back});
    }

    /** Handle the setup for the groups panel. */
    @Override public void onInitialize() {
        // Provide a loading indicator, enable the options menu, layout the fragment, set up the ad
        // view and the listeners for backend data changes.
        super.onInitialize();
        setTitles(null, null);
        mItemListType = ChatListManager.ChatListType.group;
        initAdView(mLayout);
        initList(mLayout, ChatListManager.instance.getList(mItemListType, mItem), false);
        FabManager.chat.init(this);
    }

    /** Manage the list UI every time a message change occurs. */
    @Subscribe public void onMessageListChange(final MessageListChangeEvent event) {
        // Log the event and update the list saving the result for a retry later.
        logEvent(String.format(Locale.US, "onMessageListChange with event {%s}", event));
        mUpdateOnResume = !updateAdapterList();
    }

    /** Deal with the fragment's lifecycle by managing the progress bar and the FAB. */
    @Override public void onResume() {
        // Turn on the FAB, shut down the progress bar (if it is showing), and force a recycle view
        // update.
        setTitles(null, null);
        FabManager.chat.setState(this, View.VISIBLE);
        mUpdateOnResume = true;
        super.onResume();
    }

}
