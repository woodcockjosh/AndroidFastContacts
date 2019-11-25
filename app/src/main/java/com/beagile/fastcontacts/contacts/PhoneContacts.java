package com.beagile.fastcontacts.contacts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.beagile.fastcontacts.MainActivity;
import com.beagile.fastcontacts.tasks.PhoneContactsSynchronizerTask;

public class PhoneContacts {

    private MainActivity mActivity;
    private PhoneContactsSynchronizerTask mContactsSynchronizerService;
    private BroadcastReceiver mReceiver;

    public PhoneContacts(MainActivity activity, PhoneContactsCallback callback){
        this.mActivity = activity;
        subscribeToUpdates(callback);
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
        new PhoneContactsSynchronizerTask(this.mActivity).execute();
        Log.i("PhoneContacts", "Started application");
    }

    private void subscribeToUpdates(PhoneContactsCallback callback) {
        mReceiver = getBroadcastReceiver(callback);
        IntentFilter filter = getIntentFilter();
        LocalBroadcastManager.getInstance(this.mActivity).registerReceiver(mReceiver, filter);
    }

    private BroadcastReceiver getBroadcastReceiver(final PhoneContactsCallback callback) {
        return new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action == null){
                    return;
                }

                if(action.equals(PhoneContactsSynchronizerTask.ACTION_CONTACT_SYNC_STARTED)){
                    callback.didStartSyncingContacts();
                }

                if(action.equals(PhoneContactsSynchronizerTask.ACTION_CONTACT_SYNC_UPDATED)) {
                    int position = intent.getIntExtra(PhoneContactsSynchronizerTask.EXTRA_CURRENT_POSITION, -1);
                    int max = intent.getIntExtra(PhoneContactsSynchronizerTask.EXTRA_MAX_POSITION, -1);
                    boolean wasChanged = intent.getBooleanExtra(PhoneContactsSynchronizerTask.EXTRA_WAS_CHANGED, false);
                    callback.didFinishLoadingPerson(wasChanged, position, max);
                }

                if(action.equals(PhoneContactsSynchronizerTask.ACTION_CONTACT_SYNC_COMPLETE)){
                    callback.didEndSyncingContacts();
                }

                if(action.equals(PhoneContactsSynchronizerTask.ACTION_CONTACT_SAVE_COMPLETE)){
                    callback.didEndSavingContacts();
                }
            }
        };
    }

    private IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PhoneContactsSynchronizerTask.ACTION_CONTACT_SYNC_UPDATED);
        intentFilter.addAction(PhoneContactsSynchronizerTask.ACTION_CONTACT_SYNC_STARTED);
        intentFilter.addAction(PhoneContactsSynchronizerTask.ACTION_CONTACT_SYNC_COMPLETE);
        intentFilter.addAction(PhoneContactsSynchronizerTask.ACTION_CONTACT_SAVE_COMPLETE);
        return intentFilter;
    }

    public void release() {
        LocalBroadcastManager.getInstance(this.mActivity).unregisterReceiver(mReceiver);
    }
}
