package com.beagile.fastcontacts.contacts;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import com.beagile.fastcontacts.MainActivity;

import java.util.ArrayList;
import java.util.List;

import jonathanfinerty.once.Once;

public class ContactsPermissions {

    public static final String PERMISSION_GRANTED = "PERMISSION_GRANTED";
    public static final String PERMISSION_DENIED = "PERMISSION_DENIED";
    public static final String PERMISSION_BLOCKED = "PERMISSION_BLOCKED";

    private static final int REQUEST_CODE = 123;
    private static List<String> _permissions = new ArrayList<>();

    public static boolean granted(Context context, Permission permission) {
        if (context.checkSelfPermission(permission.toString()) != PackageManager.PERMISSION_GRANTED) {
            _permissions.add(permission.toString());
            return false;
        } else {
            Once.markDone(PERMISSION_GRANTED);
            Once.clearDone(PERMISSION_BLOCKED);
        }
        return true;
    }

    public static void requestFor(Activity activity) {
        String[] permissionsArray = _permissions.toArray(new String[]{});
        activity.requestPermissions(permissionsArray, REQUEST_CODE);
        _permissions.clear();
    }

    public static void checkResult(MainActivity activity, int requestCode, int[] grantResults) {
        if (requestCode == ContactsPermissions.REQUEST_CODE) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    boolean showRationale = activity.shouldShowRequestPermissionRationale(Permission.READ_CONTACTS.toString());
                    if (!showRationale) {
                        //Permission was blocked by user ("never ask again")...
                        Once.markDone(PERMISSION_BLOCKED);
                    } else {
                        //Permission was denied by user
                        Once.markDone(PERMISSION_DENIED);
                    }
                } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Once.markDone(PERMISSION_GRANTED);
                    Once.clearDone(PERMISSION_BLOCKED);
                    activity.startPhoneContactsSynchronization();
                }
            }
        }
    }

    public enum Permission {
        READ_CONTACTS {
            @Override
            public String toString() {
                return Manifest.permission.READ_CONTACTS;
            }
        };

        public abstract String toString();
    }
}