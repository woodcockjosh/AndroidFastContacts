package com.beagile.fastcontacts.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Patterns;

public class StringUtil {

    public static String concat(char separator, String... strings) {
        StringBuilder stringBuilder = new StringBuilder();

        for (String string : strings) {
            if (string != null && string.trim().length() > 0) {
                stringBuilder.append(string.trim());
                stringBuilder.append(separator);
            }
        }
        if (stringBuilder.length() > 0) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        return stringBuilder.toString();
    }

    /**
     * Checks email for being non-empty and properly formatted.
     *
     * @param email Email for validation.
     * @return Boolean whether email valid ot not.
     */
    public static Boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }

        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Create string with capitalized first character and lowercase the others.
     *
     * @param text Source string.
     * @return Formatted string.
     */
    public static String ucFirst(String text) {
        if (TextUtils.isEmpty(text)) {
            return text;
        }

        StringBuilder builder = new StringBuilder(text.toLowerCase());
        builder.setCharAt(0, Character.toUpperCase(text.charAt(0)));
        return builder.toString();
    }

    public static String getStringResourceByName(Context context, String aString) {
        String packageName = context.getPackageName();
        int resId = context.getResources().getIdentifier(aString, "string", packageName);
        return context.getString(resId);
    }
}
