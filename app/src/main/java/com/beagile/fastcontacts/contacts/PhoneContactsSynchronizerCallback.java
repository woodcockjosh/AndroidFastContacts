package com.beagile.fastcontacts.contacts;

import com.beagile.fastcontacts.services.PhoneContactsSynchronizerTask;

/**
 * Created by josh on 8/30/15.
 */
public interface PhoneContactsSynchronizerCallback {
    void didFinishLoadingPerson(PhoneContactsSynchronizerTask.ChangeType changeType, int current, int max);
}
