package com.beagile.fastcontacts.utils;

import android.text.TextUtils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class PhoneFormatUtil {

    private static final String DEFAULT_FORMAT = "+### ### #########";

    public static boolean isSearchable(String phone) {
        if (TextUtils.isEmpty(phone)) {
            return false;
        }

        PhoneNumberUtil utils = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber numberProto = utils.parse(phone, _getCurrentCountry());
            return utils.isPossibleNumber(numberProto);
        } catch (NumberParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Formats the phone with the FastContact standard international phone format +### ### #########
     * If the phone number is not a valid phone number, the phone will not be formatted.
     *
     * @param phone The phone number for formatting.
     * @return A formatted phone for valid phone numbers.
     */
    public static String format(String phone) {
        return format(phone, _getCurrentCountry());
    }

    public static String format(String phone, String countryCode) {
        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

        if (isSearchable(phone)) {
            try {
                Phonenumber.PhoneNumber phoneNumberObject = phoneNumberUtil.parse(phone, countryCode);
                String defaultFormattedNumber = phoneNumberUtil.format(phoneNumberObject,
                        PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);

                defaultFormattedNumber = defaultFormattedNumber.replaceFirst("-", " ");
                defaultFormattedNumber = defaultFormattedNumber.replace("-", "");
                defaultFormattedNumber = _removeRedundantSpaces(defaultFormattedNumber);
                return defaultFormattedNumber;
            } catch (NumberParseException e) {
                e.printStackTrace();
                return phone;
            }
        } else {
            return phone;
        }
    }

    @NotNull
    private static String _getCurrentCountry() {
        return Locale.getDefault().getCountry();
    }

    private static String _removeRedundantSpaces(@NotNull String phone) {
        String[] parts = phone.split(" ");

        // Remove all spaces starting with 3rd
        if (parts.length < 4) {
            return phone;
        } else {
            StringBuilder result = new StringBuilder(parts[0] + " " + parts[1] + " " + parts[2]);
            for (int i = 3; i < parts.length; i++) {
                result.append(parts[i]);
            }
            return result.toString();
        }
    }

    public static boolean isValid(String phone) {

        PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

        try {
            Phonenumber.PhoneNumber phoneNumberObject = phoneNumberUtil.parse(phone, _getCurrentCountry());

            return phoneNumberUtil.isValidNumber(phoneNumberObject);

        } catch (Exception e) {
            return false;
        }
    }
}
