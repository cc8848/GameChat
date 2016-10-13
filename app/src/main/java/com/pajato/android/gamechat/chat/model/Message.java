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

    // Public class constants.

    /** The message types. */
    public final static int SYSTEM = 0;
    public final static int STANDARD = 1;
    // TODO: add this real soon: public final static int PROTECTED = 2;

    /** The member account identifer who posted the message. */
    public String owner;

    /** The message push key. */
    public String key;

    /** The poster's display name. */
    public String name;

    /** The poster's url. */
    public String url;

    /** The creation timestamp. */
    public long createTime;

    /** The last modification timestamp. */
    private long modTime;

    /** The message text. */
    public String text;

    /** The message type. */
    public int type;

    /** A list of users (by account identifier) in the room, that have not yet read the message. */
    public List<String> unreadList;

    // Public constructors.

    /** Build an empty args constructor for the database. */
    public Message() {}

    /** Build a default Message using all the parameters. */
    public Message(final String key, final String owner, final String name, final String url,
                   final long createTime, final String text, final int type,
                   final List<String> unreadList) {
        this.createTime = createTime;
        this.key = key;
        this.modTime = 0;
        this.name = name;
        this.owner = owner;
        this.text = text;
        this.type = type;
        this.url = url;
        this.unreadList = unreadList;
    }

    /** Provide a default map for a Firebase create/update. */
    @Exclude public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("createTime", createTime);
        result.put("key", key);
        result.put("modTime", modTime);
        result.put("name", name);
        result.put("owner", owner);
        result.put("text", text);
        result.put("type", type);
        result.put("url", url);
        result.put("unreadList", unreadList);

        return result;
    }
}
