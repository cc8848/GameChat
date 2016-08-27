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

package com.pajato.android.gamechat.chat.model;


import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Provide a Firebase model class repesenting a chat message, an icon, a name and text. */
@IgnoreExtraProperties public class Message {

    /** The group owner/creator. */
    private String owner;

    /** The group name. */
    public String name;

    /** The creation timestamp. */
    public long createTime;

    /** The last modification timestamp. */
    private long modTime;

    /** The message text. */
    public String text;

    /** A list of users (by account identifier) in the room, that have not yet read the message. */
    public List<String> unreadList;

    // Public constructors.

    /** Build an empty args constructor for the database. */
    public Message() {}

    /** Build a default Message. */
    public Message(final String owner, final String name, final long createTime, final long modTime,
                   final String text, final List<String> unreadList) {
        this.owner = owner;
        this.name = name;
        this.createTime = createTime;
        this.modTime = modTime;
        this.text = text;
        this.unreadList = unreadList;
    }

    /** Provide a default map for a Firebase create/update. */
    @Exclude public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("owner", owner);
        result.put("name", name);
        result.put("createTime", createTime);
        result.put("modTime", modTime);
        result.put("text", text);
        result.put("unreadList", unreadList);

        return result;
    }
}
