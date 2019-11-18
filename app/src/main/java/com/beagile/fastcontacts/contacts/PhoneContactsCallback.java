package com.beagile.fastcontacts.contacts;

/**
 * Created by josh on 8/30/15.
 */
public interface PhoneContactsCallback {
    void didFinishLoadingPerson(boolean wasChanged, int current, int max);
    void didStartSyncingContacts();
    void didEndSyncingContacts();
}
