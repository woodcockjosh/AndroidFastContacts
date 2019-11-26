package com.beagile.fastcontacts.contacts;

/**
 * Created by josh on 8/30/15.
 */
public class PhoneContactsProgress {

    public enum Action {
        SyncStarted,
        SyncUpdate,
        SyncComplete,
        SaveComplete
    }

    private PhoneContactsCallback mCallback;
    private Action mAction;
    private int mCurrent;
    private int mMax;
    private boolean mWasChanged;

    public PhoneContactsProgress(PhoneContactsCallback callback, Action action){
        this.mAction = action;
        this.mCallback = callback;
    }

    public PhoneContactsProgress(PhoneContactsCallback callback, boolean wasChanged, int current, int max){
        this(callback, Action.SyncUpdate);
        this.mCurrent = current;
        this.mMax = max;
        this.mWasChanged = wasChanged;
    }

    public void sendUpdate() {
        switch(this.mAction){
            case SyncStarted:
                this.mCallback.didStartSyncingContacts();
                break;
            case SyncUpdate:
                this.mCallback.didFinishLoadingPerson(this.mWasChanged, this.mCurrent, this.mMax);
                break;
            case SyncComplete:
                this.mCallback.didEndSyncingContacts();
                break;
            case SaveComplete:
                this.mCallback.didEndSavingContacts();
                break;
            default:
                // Do nothing
                break;
        }
    }
}
