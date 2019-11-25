package com.beagile.fastcontacts.tasks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.beagile.fastcontacts.config.FastContactsDatabase;
import com.beagile.fastcontacts.contacts.PhoneContactsSynchronizerCallback;
import com.beagile.fastcontacts.models.person.Person;
import com.beagile.fastcontacts.models.person.Person_Table;
import com.beagile.fastcontacts.models.person.elements.Email;
import com.beagile.fastcontacts.models.person.elements.EmailPersonIdHashMap;
import com.beagile.fastcontacts.models.person.elements.PhoneNumber;
import com.beagile.fastcontacts.models.person.elements.PhoneNumberPersonIdHashMap;
import com.beagile.fastcontacts.utils.CursorUtil;
import com.beagile.fastcontacts.utils.PhoneFormatUtil;
import com.beagile.fastcontacts.utils.StringUtil;
import com.dbflow5.config.DBFlowDatabase;
import com.dbflow5.config.FlowManager;
import com.dbflow5.database.DatabaseWrapper;
import com.dbflow5.query.SQLite;
import com.dbflow5.query.Select;
import com.dbflow5.transaction.FastStoreModelTransaction;
import com.dbflow5.transaction.ITransaction;
import com.dbflow5.transaction.Transaction;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class PhoneContactsSynchronizerTask extends AsyncTask {

    // region: Enum

    private static final String CHANNEL_ID = PhoneContactsSynchronizerTask.class.getSimpleName();
    private static final String TAG = PhoneContactsSynchronizerTask.class.getSimpleName();

    public static final String ACTION_CONTACT_SYNC_STARTED = "ACTION_CONTACT_SYNC_STARTED";
    public static final String ACTION_CONTACT_SYNC_UPDATED = "ACTION_CONTACT_SYNC_UPDATED";
    public static final String ACTION_CONTACT_SYNC_COMPLETE = "ACTION_CONTACT_SYNC_COMPLETE";
    public static final String ACTION_CONTACT_SAVE_COMPLETE = "ACTION_CONTACT_SAVE_COMPLETE";

    public static final String EXTRA_CURRENT_POSITION = "EXTRA_CURRENT_POSITION";
    public static final String EXTRA_MAX_POSITION = "EXTRA_MAX_POSITION";
    public static final String EXTRA_WAS_CHANGED = "EXTRA_WAS_CHANGED";

    public enum ChangeType {
        ADD, MERGE, UPDATE, IGNORE, DELETE, DISASSOCIATE
    }

    // endregion

    // region: Constants
    private static final String VERSION = "version";
    private static final String LAST_SYNCED = "last_contact_sync_timestamp_in_millis";
    private static final long UNKNOWN_ID = -1;
    private static final long CYCLES_PER_MEMORY_CLEAN = 200;

    // endregion

    // region: Class properties
    private int mAllPersons;
    private List<Person> mPersonsToDelete;
    private int mPersonsUpdated;
    private int mPersonsAdded;
    private int mPersonsMerged;
    private int mPersonsDisassociated;
    private final IBinder mBinder = new LocalBinder();

    private long mPersonContactID;
    private long mContactContactID;
    private String mPersonID;

    private boolean mIsLimitingListByLastUpdateDate;

    private Context mContext;
    private LocalBroadcastManager mBroadcastManager;
    private PhoneContactsSynchronizerCallback callback;

    private boolean mLastRowChangedIsTheSameAsCurrentRowChanged;
    private long mLastRowChangedTimestamp;
    private PhoneNumberPersonIdHashMap phoneNumberPersonIdHashMap;
    private EmailPersonIdHashMap emailPersonIdHashMap;
    private List<Person> personsToSave;

    // endregion

    // region: Cursor variables

    private Cursor mPersonsCursor;
    private Cursor mContactsCursor;

    // endregion

    // region: Singleton and constructor

    public PhoneContactsSynchronizerTask() {
        this.mLastRowChangedTimestamp = 0;
        this.personsToSave = new ArrayList<>();
    }

    public PhoneContactsSynchronizerTask(Context context) {
        this();
        this.mContext = context;
        this.mBroadcastManager = LocalBroadcastManager.getInstance(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Long doInBackground(Object[] objects) {

        this._setup();
        Long count = (long) mContactsCursor.getCount();

        _sendBroadcast(ACTION_CONTACT_SYNC_STARTED);

        this.sync(new PhoneContactsSynchronizerCallback() {
            @Override
            public void didFinishLoadingPerson(ChangeType changeType, int current, int max) {
                boolean wasChanged = (changeType != ChangeType.IGNORE && changeType != ChangeType.MERGE);
                _sendBroadcast(wasChanged, current, max);
            }
        });

        _sendBroadcast(ACTION_CONTACT_SYNC_COMPLETE);

        savePersons();

        _sendBroadcast(ACTION_CONTACT_SAVE_COMPLETE);

        return count;
    }

    protected void onPostExecute(Long result) {
        this.mBroadcastManager = null;
        this.mContext = null;
    }

    public static String[] getFieldProjection() {
        List<String> projectionList = new ArrayList<>();

        projectionList.add(ContactsContract.Data.CONTACT_ID);
        projectionList.add(ContactsContract.Data.MIMETYPE);
        projectionList.add(ContactsContract.Data.PHOTO_URI);
        projectionList.add(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);
        projectionList.add(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
        projectionList.add(ContactsContract.CommonDataKinds.Phone.NUMBER);
        projectionList.add(ContactsContract.CommonDataKinds.Phone.TYPE);
        projectionList.add(ContactsContract.CommonDataKinds.Phone.LABEL);
        projectionList.add(ContactsContract.CommonDataKinds.Email.ADDRESS);
        projectionList.add(ContactsContract.CommonDataKinds.Email.TYPE);
        projectionList.add(ContactsContract.CommonDataKinds.Email.LABEL);
        projectionList.add(VERSION);
        projectionList.add(ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP);

        return projectionList.toArray(new String[0]);
    }

    // endregion

    // region: Orchestration

    @NotNull
    @Contract(pure = true)
    public static String getSelection(boolean limitList) {

        return "(" +
                ContactsContract.Data.MIMETYPE +
                " = ?" +
                " OR " +
                ContactsContract.Data.MIMETYPE +
                " = ?" +
                " OR " +
                ContactsContract.Data.MIMETYPE +
                " = ?" +
                " OR " +
                ContactsContract.Data.MIMETYPE +
                " = ?)" +
                " AND " +
                ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP +
                " > ?";

    }

    @NotNull
    public static String[] getSelectionArgs(boolean limitList, Context context) {
        List<String> selectionArgsBuilder = new ArrayList<>();

        selectionArgsBuilder.add(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        selectionArgsBuilder.add(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        selectionArgsBuilder.add(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        selectionArgsBuilder.add(ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);

        SharedPreferences prefs = context.getSharedPreferences(LAST_SYNCED, 0);
        long lastSyncTimestampInMillis = prefs.getLong(LAST_SYNCED, 0);
        selectionArgsBuilder.add(String.valueOf(lastSyncTimestampInMillis));
        return selectionArgsBuilder.toArray(new String[0]);
    }

    public synchronized void sync(PhoneContactsSynchronizerCallback callback) {

        if (!this.mLastRowChangedIsTheSameAsCurrentRowChanged) {

            Log.i(TAG, "Begin synchronizing phone contacts with FastContacts contacts");

            int cycleCount = 0;
            do {
                this._syncNextContact(callback);
                if (cycleCount++ > CYCLES_PER_MEMORY_CLEAN) {
                    System.gc();
                    cycleCount = 0;
                }
            } while (!this.mPersonsCursor.isAfterLast() || !this.mContactsCursor.isAfterLast());

            this._commitChangesThatWouldHaveAffectedCursors(callback);
            this._saveLastSyncedTime();
            this._logTotals();
        } else {
            Log.i(TAG, "Skipped phone contacts synchronization with FastContacts contacts");
        }
        this._releaseCursors();
    }

    private void _releaseCursors() {
        this.mContactsCursor.close();
        this.mContactsCursor = null;

        if (this.mPersonsAdded > 0 || this.mPersonsToDelete.size() > 0) {
            this.mPersonsCursor.close();
            this.mPersonsCursor = null;
        }
    }

    @SuppressLint("CommitPrefEdits")
    private void _saveLastSyncedTime() {

        long lastSyncTimestampInMillis = DateTime.now().toDate().getTime();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.mContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(LAST_SYNCED, lastSyncTimestampInMillis);
        editor.apply();
    }

    private void _setup() {
        this.mAllPersons = 0;
        this.mPersonsToDelete = new ArrayList<>();
        this.mPersonsUpdated = 0;
        this.mPersonsAdded = 0;
        this.mPersonsMerged = 0;
        this.mPersonsDisassociated = 0;

        this.phoneNumberPersonIdHashMap = new PhoneNumberPersonIdHashMap();
        this.emailPersonIdHashMap = new EmailPersonIdHashMap();

        this.mIsLimitingListByLastUpdateDate = true;
        this.mPersonID = null;
        this.mPersonsCursor = this._getPersonsCursor();
        this.mContactsCursor = this._getContactsCursor();

        this._goToFirstPerson();
        this._goToFirstContact();
        this._changeListToFullContactListInCaseContactsWereDeleted();
        this._checkRowChangedForDuplicateSyncEvents();
    }

    /**
     * This was implemented to deal with an issue caused by onchange being called multiple times by a content observer
     * The behavior varies from device to device
     */
    private void _checkRowChangedForDuplicateSyncEvents() {
        this.mLastRowChangedIsTheSameAsCurrentRowChanged = false;

        if (!this.mContactsCursor.isAfterLast()) {
            long lastUpdated = CursorUtil.getLong(this.mContactsCursor, ContactsContract.Data.CONTACT_LAST_UPDATED_TIMESTAMP);
            if (lastUpdated > this.mLastRowChangedTimestamp) {
                this.mLastRowChangedTimestamp = lastUpdated;
            } else {
                this.mLastRowChangedIsTheSameAsCurrentRowChanged = true;
            }
        }
    }

    private void _changeListToFullContactListInCaseContactsWereDeleted() {
        if (this.mContactsCursor.isAfterLast()) {
            this.mIsLimitingListByLastUpdateDate = false;
            this.mContactsCursor = this._getContactsCursor();
            this._goToFirstContact();
        }
    }

    private void _commitChangesThatWouldHaveAffectedCursors(PhoneContactsSynchronizerCallback callback) {
        for (Person person : this.mPersonsToDelete) {
            person.delete(FastContactsDatabase.instance());
        }
        if (this.mPersonsToDelete.size() > 0) {
            callback.didFinishLoadingPerson(ChangeType.DELETE, mContactsCursor.getPosition(), mContactsCursor.getCount());
        }
    }

    // endregionP

    // region: Cursor setup

    private void _logTotals() {
        Log.i(TAG, "Total added contacts: " + this.mPersonsAdded);
        Log.i(TAG, "Total merged contacts: " + this.mPersonsMerged);
        Log.i(TAG, "Total updated contacts: " + this.mPersonsUpdated);
        Log.i(TAG, "Total deleted contacts: " + this.mPersonsToDelete.size());
        Log.i(TAG, "Total disassociated contacts: " + this.mPersonsDisassociated);
        Log.i(TAG, "Total contacts: " + this.mAllPersons);
    }

    private void _syncNextContact(PhoneContactsSynchronizerCallback callback) {

        if (this.mContactContactID == UNKNOWN_ID && this.mPersonContactID == UNKNOWN_ID) {
            // FastContacts contact id not in phone contacts
            this._goToNextContact();
            this._goToNextPerson();
        } else if (this.mContactContactID > this.mPersonContactID) {
            this._addPerson(callback);
            this._goToNextContact();
        } else if (this.mContactContactID == this.mPersonContactID) {
            this._updatePerson(callback);
            this._goToNextContact();
            this._goToNextPerson();
        } else if (this.mPersonID == null) {
            callback.didFinishLoadingPerson(ChangeType.IGNORE, mContactsCursor.getPosition(), mContactsCursor.getCount() - 1);
            this._goToNextPerson();
        } else {
            this._removePerson(callback);
            this._goToNextPerson();
        }
    }

    private Cursor _getContactsCursor() {
        Uri uri = ContactsContract.Data.CONTENT_URI;

        String[] projection = getFieldProjection();

        String selection;
        String[] selectionArgs;
        selection = getSelection(mIsLimitingListByLastUpdateDate);
        selectionArgs = getSelectionArgs(mIsLimitingListByLastUpdateDate, this.mContext);

        String sortOrder = ContactsContract.Data.CONTACT_ID + " DESC";

        return this.mContext.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
    }

    private Cursor _getNewPersonsCursor() {
        DatabaseWrapper database = FastContactsDatabase.instance().getWritableDatabase();

        String rawStatement = "SELECT "
                + "phone_contact_id, "
                + "phone_contact_version, "
                + "id, "
                + "user_id FROM "
                + Person.class.getSimpleName() + " ORDER BY "
                + "phone_contact_id DESC ";

        Cursor cursor = database.rawQuery(rawStatement, null);

        Log.i(TAG, "New person cursor created");

        return cursor;
    }

    private Cursor _getPersonsCursor() {
        if (mPersonsCursor == null) {
            return _getNewPersonsCursor();
        } else {
            return mPersonsCursor;
        }
    }

    private void _goToFirstPerson() {
        if (this.mPersonsCursor.moveToFirst()) {
            this.mPersonContactID = CursorUtil.getLong(this.mPersonsCursor, "phone_contact_id");
        } else {
            this.mPersonContactID = UNKNOWN_ID;
        }
    }

    // endregion

    // region: Cursor movement

    private void _goToFirstContact() {
        if (this.mContactsCursor.moveToFirst()) {
            this.mContactContactID = CursorUtil.getLong(this.mContactsCursor, ContactsContract.Data.CONTACT_ID);
        } else {
            this.mContactContactID = UNKNOWN_ID;
        }
    }

    private void _goToNextPerson() {
        if (this.mPersonsCursor.moveToNext()) {
            this.mPersonContactID = CursorUtil.getLong(this.mPersonsCursor, "phone_contact_id");
            this.mPersonID = CursorUtil.getString(this.mPersonsCursor, "user_id");
        } else {
            this.mPersonContactID = UNKNOWN_ID;
            this.mPersonID = null;
        }
    }

    private void _goToNextContact() {
        if (!this.mContactsCursor.isAfterLast()) {
            this.mContactContactID = CursorUtil.getInt(this.mContactsCursor, ContactsContract.Data.CONTACT_ID);
        } else {
            this.mContactContactID = UNKNOWN_ID;
        }
    }

    private void _addPerson(PhoneContactsSynchronizerCallback callback) {

        Person person = new Person();

        person.setPhoneContactID(CursorUtil.getInt(mContactsCursor, ContactsContract.Data.CONTACT_ID));
        person.setPhoneContactVersion(CursorUtil.getInt(mContactsCursor, VERSION));

        _populatePerson(person);

        if (_isValidPerson(person)) {
            Integer autoincrementID = this.lookupPersonIdWithContactInformation(person);
            ChangeType type;
            if (this._personAlreadyExistsBasedOnContactInformation(autoincrementID)) {
                person = this._mergePersonInfo(person, autoincrementID);
                this.mPersonsMerged++;
                type = ChangeType.MERGE;
            } else {
                this.mPersonsAdded++;
                type = ChangeType.ADD;
            }
            try {
                this.savePerson(person);
            } catch (NullPointerException e) {
                //Person will be null if _mergePersonInfo returns null or if lookedUpPerson is null.
            }
            callback.didFinishLoadingPerson(type, mContactsCursor.getPosition(), mContactsCursor.getCount() - 1);
        }
    }

    // endregion

    // region: Add Person

    private boolean _personAlreadyExistsBasedOnContactInformation(Integer autoincrementID) {
        return autoincrementID != null;
    }

    private Person _mergePersonInfo(Person person, int autoIncrementID) {

        person.setAutoincrementID(autoIncrementID);

        Person lookedUpPerson = SQLite.select()
                .from(Person.class)
                .where(Person_Table.id.eq(autoIncrementID))
                .querySingle(FastContactsDatabase.instance());

        if (lookedUpPerson != null) {
            if (StringUtils.isEmpty(lookedUpPerson.getFirstName()) && StringUtils.isEmpty(lookedUpPerson.getLastName())) {
                lookedUpPerson.setFullName(person.getFirstName(), person.getLastName());
            }

            lookedUpPerson.addEmails(person.getEmails());
            lookedUpPerson.addPhones(person.getPhoneNumbers());
            lookedUpPerson.setPhoneContactVersion(person.getPhoneContactVersion());
            lookedUpPerson.setPhoneContactID(person.getPhoneContactID());
        }
        return lookedUpPerson;
    }

    private void _updatePerson(PhoneContactsSynchronizerCallback callback) {

        long contactContactVersion = CursorUtil.getLong(mContactsCursor, VERSION);
        long personContactVersion = CursorUtil.getLong(mPersonsCursor, "phone_contact_version");
        String fastContactsId = CursorUtil.getString(mPersonsCursor, "user_id");

        Person person = new Person();

        person.setUserId(fastContactsId);
        person.setPhoneContactID(CursorUtil.getInt(mContactsCursor, ContactsContract.Data.CONTACT_ID));
        person.setPhoneContactVersion(contactContactVersion);

        _populatePerson(person);

        if (_isValidPerson(person)) {
            if (contactContactVersion > personContactVersion) {
                person.setAutoincrementID(CursorUtil.getInt(mPersonsCursor, "id"));
                this.mPersonsUpdated++;
                this.savePerson(person);
                callback.didFinishLoadingPerson(ChangeType.UPDATE, mContactsCursor.getPosition(), mContactsCursor.getCount() - 1);
            } else {
                callback.didFinishLoadingPerson(ChangeType.IGNORE, mContactsCursor.getPosition(), mContactsCursor.getCount() - 1);
            }
        } else {
            if (person.getUserId() == null) {
                this.mPersonsToDelete.add(person);
            } else {
                this.mPersonsDisassociated++;
                person.setPhoneContactID(0);
                this.savePerson(person);
                callback.didFinishLoadingPerson(ChangeType.MERGE, mContactsCursor.getPosition(), mContactsCursor.getCount() - 1);
            }
        }
    }

    // endregion

    // region: Update Person

    private void _removePerson(PhoneContactsSynchronizerCallback callback) {
        if (!this.mIsLimitingListByLastUpdateDate) {
            String fastContactsId = CursorUtil.getString(mPersonsCursor, "user_id");
            int autoincrementID = CursorUtil.getInt(mPersonsCursor, "id");

            if (fastContactsId == null) {
                Person person = new Person();
                person.setAutoincrementID(autoincrementID);
                this.mPersonsToDelete.add(person);
            } else {
                Person person = new Select().from(Person.class)
                        .where(Person_Table.id.eq(autoincrementID))
                        .querySingle(FastContactsDatabase.instance());

                if (person != null) {
                    person.setPhoneContactID(UNKNOWN_ID);
                    person.setPhoneContactVersion(UNKNOWN_ID);

                    this.savePerson(person);
                    this.mPersonsDisassociated++;

                    callback.didFinishLoadingPerson(ChangeType.DISASSOCIATE, mContactsCursor.getPosition(), mContactsCursor.getCount() - 1);
                } else {
                    Log.e(TAG, "Could not find person with autoincrement id: " + autoincrementID);
                }
            }
        }
    }

    // endregion

    // region: Remove person

    private void _populatePerson(Person person) {

        long firstContactId = this.mContactContactID;
        long currentContactId = this.mContactContactID;

        while (!this.mContactsCursor.isAfterLast() && currentContactId == firstContactId) {

            _populatePersonField(person);

            if (this.mContactsCursor.moveToNext()) {
                currentContactId = CursorUtil.getLong(this.mContactsCursor, ContactsContract.Data.CONTACT_ID);
            }
        }

        int totalContactInfoCount = person.getEmails().size() + person.getPhoneNumbers().size();
        person.setHasContactInfo(totalContactInfoCount > 0);

        this.mAllPersons++;
    }

    // endregion

    // region: Data readers

    private void _populatePersonField(Person person) {

        String mimeType = this.mContactsCursor.getString(this.mContactsCursor.getColumnIndex(ContactsContract.Data.MIMETYPE));

        // Name
        if (ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
            String firstName = CursorUtil.getString(this.mContactsCursor,
                    ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME);
            String lastName = CursorUtil.getString(this.mContactsCursor,
                    ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME);
            person.setFullName(firstName, lastName);
        } else if (ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
            String number = CursorUtil.getString(this.mContactsCursor, ContactsContract.CommonDataKinds.Phone.NUMBER);

            if (PhoneFormatUtil.isValid(number)) {
                int type = CursorUtil.getInt(this.mContactsCursor, ContactsContract.CommonDataKinds.Phone.TYPE);

                String label;
                if (ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM != type) {
                    label = ContactsContract.CommonDataKinds.Phone.getTypeLabel(mContext.getResources(), type, "").toString();
                } else {
                    label = CursorUtil.getString(this.mContactsCursor, ContactsContract.CommonDataKinds.Phone.LABEL);
                }
                PhoneNumber phoneNumber = new PhoneNumber(number, label);
                person.addPhone(phoneNumber);
            }
        } else if (ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE.equals(mimeType)) {
            String address = CursorUtil.getString(this.mContactsCursor, ContactsContract.CommonDataKinds.Email.ADDRESS);

            if (StringUtil.isValidEmail(address)) {
                int type = CursorUtil.getInt(this.mContactsCursor, ContactsContract.CommonDataKinds.Email.TYPE);

                String label;
                if (ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM != type) {
                    label = ContactsContract.CommonDataKinds.Email.getTypeLabel(mContext.getResources(), type, "").toString();
                } else {
                    label = CursorUtil.getString(this.mContactsCursor, ContactsContract.CommonDataKinds.Email.LABEL);
                }
                Email email = new Email(address, label);
                person.addEmail(email);
            }
        } else if (ContactsContract.Data.PHOTO_URI.equals(mimeType)) {

            String photoURI = CursorUtil.getString(this.mContactsCursor, ContactsContract.Data.PHOTO_URI);

            person.setImagePath(photoURI);
        }
    }

    private boolean _isValidPerson(Person person) {
        boolean hasName = (!TextUtils.isEmpty(person.getFirstName()) || !TextUtils.isEmpty(person.getLastName()));

        boolean hasEmail = person.getEmails() != null && person.getEmails().size() > 0;
        boolean hasPhoneNumber = person.getPhoneNumbers() != null && person.getPhoneNumbers().size() > 0;

        return hasName || hasEmail || hasPhoneNumber;
    }

    private Integer lookupPersonIdWithContactInformation(Person person) {
        if (person.emails != null) {
            for (Email email : person.emails) {
                Integer personId = this.emailPersonIdHashMap.get(email.hashValue);
                if (personId != null) {
                    return personId;
                }
            }
        }

        if (person.phoneNumbers != null) {
            for (PhoneNumber phoneNumber : person.phoneNumbers) {
                Integer personId = this.phoneNumberPersonIdHashMap.get(phoneNumber.hashValue);
                if (personId != null) {
                    return personId;
                }
            }
        }

        return null;
    }

    private void savePerson(final Person person) {
        if (person == null) {
            return;
        }

        this.personsToSave.add(person);

        if (person.emails != null) {
            for (Email email : person.emails) {
                this.emailPersonIdHashMap.put(email.hashValue, person.getPersonId());
            }
        }

        if (person.phoneNumbers != null) {
            for (PhoneNumber phoneNumber : person.phoneNumbers) {
                this.phoneNumberPersonIdHashMap.put(phoneNumber.hashValue, person.getPersonId());
            }
        }
    }

    private void savePersons() {
        FastStoreModelTransaction
                .saveBuilder(FlowManager.getModelAdapter(Person.class))
                .addAll(personsToSave)
                .build()
                .execute(FastContactsDatabase.instance());
    }

    // endregion

    // region: Broadcast

    public class LocalBinder extends Binder {
        public PhoneContactsSynchronizerTask getService() {
            // Return this instance of LocalService so clients can call public methods
            return PhoneContactsSynchronizerTask.this;
        }
    }

    private void _sendBroadcast(final String action) {
        Intent outIntent = new Intent(action);
        mBroadcastManager.sendBroadcast(outIntent);
    }

    private void _sendBroadcast(boolean wasChanged, int currentPosition, int maxPosition) {

        Intent outIntent = new Intent(ACTION_CONTACT_SYNC_UPDATED);
        outIntent.putExtra(EXTRA_WAS_CHANGED, wasChanged);
        outIntent.putExtra(EXTRA_CURRENT_POSITION, currentPosition);
        outIntent.putExtra(EXTRA_MAX_POSITION, maxPosition);
        mBroadcastManager.sendBroadcast(outIntent);
    }

    // endregion
}
