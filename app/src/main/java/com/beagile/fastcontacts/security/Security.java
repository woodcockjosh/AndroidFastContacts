package com.beagile.fastcontacts.security;

/**
 * Created by josh on 9/15/15.
 * <p>
 * Holds constants and enumerations for security
 */
public class Security {

    public static final String BROADCAST_NETWORK_SECURITY_ERROR_OCCURRED = "BROADCAST_NETWORK_SECURITY_ERROR_OCCURRED";
    public static final String BROADCAST_EXTRA_NETWORK_SECURITY_ERROR_TYPE = "BROADCAST_EXTRA_NETWORK_SECURITY_ERROR_TYPE";

    public enum NetworkErrorType {
        EXPIRED_CERTIFICATE, INVALID_CERTIFICATE, UNSUPPORTED_DEVICE
    }
}
