package com.beagile.fastcontacts.contacts;

import android.content.BroadcastReceiver;
import android.util.Log;

import androidx.annotation.NonNull;

import com.beagile.fastcontacts.MainActivity;
import com.beagile.fastcontacts.tasks.PhoneContactsSynchronizerTask;

public class PhoneContacts {

    private MainActivity mActivity;
    private PhoneContactsSynchronizerTask mContactsSynchronizerService;
    private BroadcastReceiver mReceiver;
    private PhoneContactsCallback mCallback;

    public PhoneContacts(MainActivity activity, PhoneContactsCallback callback){
        this.mActivity = activity;
        this.mCallback = callback;
    }

    public void starSyncWithPermissionsCheck() {
        if(!ContactsPermissions.granted(this.mActivity, ContactsPermissions.Permission.READ_CONTACTS)) {
            ContactsPermissions.requestFor(this.mActivity);
        }else{
            this.mActivity.startPhoneContactsSynchronization();
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        ContactsPermissions.checkResult(this.mActivity, requestCode, grantResults);
    }

    public void sync() {
        new PhoneContactsSynchronizerTask(this.mActivity).execute(this.mCallback);
        Log.i("PhoneContacts", "Started application");
    }
}
