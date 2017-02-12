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

package com.pajato.android.gamechat.chat.fragment;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.inputmethod.InputMethodManager;

import com.pajato.android.gamechat.R;
import com.pajato.android.gamechat.chat.BaseCreateFragment;
import com.pajato.android.gamechat.chat.model.Group;
import com.pajato.android.gamechat.chat.model.Room;
import com.pajato.android.gamechat.chat.model.Room.RoomType;
import com.pajato.android.gamechat.common.ToolbarManager;
import com.pajato.android.gamechat.common.model.Account;
import com.pajato.android.gamechat.database.AccountManager;
import com.pajato.android.gamechat.database.GroupManager;
import com.pajato.android.gamechat.database.MemberManager;
import com.pajato.android.gamechat.database.MessageManager;
import com.pajato.android.gamechat.database.RoomManager;
import com.pajato.android.gamechat.exp.NotificationManager;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.pajato.android.gamechat.chat.model.Message.STANDARD;
import static com.pajato.android.gamechat.common.ToolbarManager.MenuItemType.helpAndFeedback;
import static com.pajato.android.gamechat.common.ToolbarManager.MenuItemType.settings;

/**
 * Create a room ...
 *
 * Validate that the associated group and member both exist even as early as possible.
 */
public class CreateRoomFragment extends BaseCreateFragment {

    // Private instance variables.

    /** The group containing the new room. */
    private Group mGroup;

    /** The member associated with teh account creating the new room. */
    private Account mMember;

    /** The room being created. */
    private Room mRoom;

    // Public instance methods.

    /** Establish the create time state. */
    @Override public void onStart() {
        // Ensure that there is a group to use in creating the new room.
        super.onStart();
        mGroup = GroupManager.instance.getGroupProfile(mItem.groupKey);
        Account account = AccountManager.instance.getCurrentAccount();
        mMember = MemberManager.instance.getMember(mGroup.key, account.id);
        if (mGroup == null || mMember == null) {
            // Return to the previous fragment using a back press.
            getActivity().onBackPressed();
            // Probably good to put a toast or snackbar here.
            return;
        }

        // Establish the list type and setup the toolbar.
        mCreateType = CreateType.room;
        int titleResId = R.string.CreateRoomMenuTitle;
        ToolbarManager.instance.init(this, titleResId, helpAndFeedback, settings);

        // Set up the room profile.
        mRoom = new Room();
        mRoom.owner = account.id;
        mRoom.name = getDefaultName();
        mRoom.groupKey = mGroup.key;
        mRoom.owner = mMember.id;
        mRoom.type = RoomType.PUBLIC;
    }

    // Protected instance methods.

    /** Save the room being created to the Firebase realtime database. */
    @Override protected void save(@NonNull Account account) {
        // Persist the configured room.
        mRoom.key = RoomManager.instance.getRoomKey(mGroup.key);
        mRoom.addMember(account.id);
        RoomManager.instance.createRoomProfile(mRoom);

        // Update and persist the group adding the new room to it's room list.
        mGroup.roomList.add(mRoom.key);
        GroupManager.instance.updateGroupProfile(mGroup);

        // Update and persist the member adding the new room to it's join list.
        mMember.joinMap.put(mRoom.key, true);
        MemberManager.instance.updateMember(mMember);

        // Post a welcome message to the new room from the owner.
        String text = "Welcome to my new room!";
        MessageManager.instance.createMessage(text, STANDARD, account, mRoom);

        // Dismiss the Keyboard and return to the previous fragment.
        Activity activity = getActivity();
        InputMethodManager manager;
        manager = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);
        if (manager.isAcceptingText() && activity.getCurrentFocus() != null)
            manager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);

        // Give the user a snackbar message offering to join friends to the room.
        NotificationManager.instance.notifyRoomCreate(this, mRoom.key, mRoom.name);
    }

    /** Set the room name conditionally to the given value. */
    @Override protected void setName(final String value) {if (mRoom != null) mRoom.name = value;}

    /** Implement the set type. */
    @Override protected void setType(final RoomType type) {
        mRoom.type = type;
    }
}
