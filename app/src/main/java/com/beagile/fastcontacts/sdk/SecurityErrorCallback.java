package com.beagile.fastcontacts.sdk;

import com.beagile.fastcontacts.security.Security;

/**
 * Created by josh on 9/15/15.
 */
public interface SecurityErrorCallback {
    void handle(Security.NetworkErrorType type);
}
