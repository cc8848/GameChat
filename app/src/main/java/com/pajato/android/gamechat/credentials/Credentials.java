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

package com.pajato.android.gamechat.credentials;

/**
 * Provide a POJO to model an a User.
 *
 * @author Paul Michael Reilly
 */
public class Credentials {

    // Private instance variables.

    /** The User email address. */
    public String email;

    /** The email has been tested and is a known account (so we cannot set a username) */
    public boolean accountIsKnown;

    /** The identity provider. */
    public String provider;

    /** The identity secret. */
    public String secret;

    /** The identity token. */
    public String token;

    /** The user name */
    public String name;

    // Public constructors.

    /** Build an empty constructor. */
    public Credentials() {}


    /** Build an instance for e-mail login (used when creating a protected user) */
    public Credentials(final String email, final String name, final String password, final boolean isKnown) {
        this.email = email;
        this.name = name;
        this.secret = password;
        this.accountIsKnown = isKnown;
    }

    /** Build an instance */
    public Credentials(final String provider, final String email, final String secret,
                       final String token) {
        this.email = email;
        this.provider = provider;
        this.secret = secret;
        this.token = token;
    }

}
