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

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

/**
 * Provides an account data model class.
 *
 * @author Paul Michael Reilly
 */
@Data
@Builder(toBuilder=true)
public final class Account {

    // Private instance variables

    /** The account id, usually an email address or a phone number. */
    private String accountId;

    /** The account display name, usually something like "Fred C. Jones". */
    private String displayName;

    /** The account icon, a URL. */
    private String accountUrl;

    /** The account token, an access key supplied by the provider. */
    private String token;

    /** The account provider name, a string like "Facebook". */
    private String providerName;

    /** The account provider id, a string like "google.com". */
    private String providerId;

    /** The account avatars. The key is the name, the value is a URL for the image. */
    @Singular
    private Map<String, URL> avatars = new ConcurrentHashMap<>();

    // Public class methods

    /** Load an account for the given account and provider ids. */
    public static void load(final String accountId, final String providerId) {
        // TODO: figure out how to deal with this via Firebase.
    }

}
