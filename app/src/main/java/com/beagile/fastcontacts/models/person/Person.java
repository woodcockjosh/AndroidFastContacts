package com.beagile.fastcontacts.models.person;

import android.text.TextUtils;

import com.beagile.fastcontacts.config.FastContactsDatabase;
import com.beagile.fastcontacts.models.person.elements.Email;
import com.beagile.fastcontacts.models.person.elements.Email_Table;
import com.beagile.fastcontacts.models.person.elements.PhoneNumber;
import com.beagile.fastcontacts.models.person.elements.PhoneNumber_Table;
import com.beagile.fastcontacts.utils.PhoneFormatUtil;
import com.beagile.fastcontacts.utils.StringUtil;
import com.dbflow5.annotation.Column;
import com.dbflow5.annotation.OneToMany;
import com.dbflow5.annotation.OneToManyMethod;
import com.dbflow5.annotation.PrimaryKey;
import com.dbflow5.annotation.Table;
import com.dbflow5.database.DatabaseWrapper;
import com.dbflow5.query.SQLite;
import com.dbflow5.structure.BaseModel;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Table(database = FastContactsDatabase.class)
public class Person extends BaseModel implements Serializable {

    // region: Enum

    enum DisplayType {
        Default,
        Connect,
        Invite,
        Invited,
        NA
    }

    enum InviteType {
        PhoneOnly,
        EmailOnly,
        PhoneOrEmail,
        NA
    }

    enum InviteSelection {
        None,
        One,
        Many
    }

    class InviteInfo {
        public final InviteType type;
        public final InviteSelection selection;

        InviteInfo(InviteType type, InviteSelection selection) {
            this.type = type;
            this.selection = selection;
        }

        InviteInfo(InviteType type) {
            this.type = type;
            this.selection = InviteSelection.None;
        }
    }

    //endregion

    // region: DBFlow variables

    // Requirement for Cursor Adapters
    @Column(name = "id")
    @PrimaryKey(autoincrement = true)
    public int mId;
    @SuppressWarnings("WeakerAccess")
    @SerializedName("emails")
    @Expose
    public List<Email> emails;
    @SuppressWarnings("WeakerAccess")
    @SerializedName("phones")
    @Expose
    public List<PhoneNumber> phoneNumbers;
    @Column(name = "user_id", getterName = "getUserId", setterName = "setUserId")
    @SerializedName("user_id")
    @Expose
    private String mUserId;
    @Column(name = "image_url", getterName = "getImageUrl", setterName = "setImageUrl")
    @SerializedName("image_url")
    @Expose
    private String mImageUrl;
    @Column(name = "first_name", getterName = "getFirstName", setterName = "setFirstName")
    @SerializedName("first_name")
    @Expose
    private String mFirstName;

    @Column(name = "last_name", getterName = "getLastName", setterName = "setLastName")
    @SerializedName("last_name")
    @Expose
    private String mLastName;

    @Column(name = "phone_contact_id", getterName = "getPhoneContactID", setterName = "setPhoneContactID")
    private long _phoneContactID;

    // endregion

    // region: Manager variables
    @Column(name = "phone_contact_version", getterName = "getPhoneContactVersion",
            setterName = "setPhoneContactVersion")
    private long _phoneContactVersion;

    @Column(name = "is_invited", getterName = "getIsInvitedIntVal", setterName = "setIsInvitedIntVal")
    private int mInvited;

    @Column(name = "is_connection", getterName = "getIsConnectionIntVal", setterName = "setIsConnectionIntVal")
    private int mIsConnection;

    @Column(name = "has_contact_info", getterName = "getHasContactInfoIntVal", setterName = "setHasContactInfoIntVal")
    private int mHasContactInfo;

    // endregion

    // region: Constructor and setup methods

    @SuppressWarnings("WeakerAccess")
    public Person(String id) {
        this.mUserId = id;
        this.mFirstName = "";
        this.mLastName = "";
        this.mImageUrl = "";
        this.phoneNumbers = new ArrayList<>();
        this.emails = new ArrayList<>();
    }

    public Person() {
        this(null);
    }

    // endregion

    // region: Getters

    @OneToMany(oneToManyMethods = {OneToManyMethod.ALL}, variableName = "emails")
    public List<Email> getEmails() {
        if (this.emails == null || this.emails.isEmpty()) {
            this.emails = SQLite.select()
                    .from(Email.class)
                    .where(Email_Table.person_id.eq(this.mId))
                    .queryList(FastContactsDatabase.instance());
            for (Email email : this.emails) {
                email.person = this;
            }
        }
        return this.emails;
    }

    @OneToMany(oneToManyMethods = {OneToManyMethod.ALL}, variableName = "phoneNumbers")
    public List<PhoneNumber> getPhoneNumbers() {
        if (this.phoneNumbers == null || this.phoneNumbers.isEmpty()) {
            this.phoneNumbers = SQLite.select()
                    .from(PhoneNumber.class)
                    .where(PhoneNumber_Table.person_id.eq(this.mId))
                    .queryList(FastContactsDatabase.instance());
            for (PhoneNumber phoneNumber : this.phoneNumbers) {
                phoneNumber.person = this;
            }
        }
        return this.phoneNumbers;
    }

    public void addPhone(PhoneNumber phone) {
        if (phone == null) {
            return;
        }

        phone.person = this;
        this.setHasContactInfo(true);
        if (!this.phoneNumbers.contains(phone)) {
            this.phoneNumbers.add(phone);

            Collections.sort(this.phoneNumbers, new Comparator<PhoneNumber>() {
                @Override
                public int compare(PhoneNumber lhs, PhoneNumber rhs) {
                    if (lhs.type == null && rhs.type == null) {
                        return 0;
                    } else if (lhs.type == null) {
                        return -1;
                    } else if (rhs.type == null) {
                        return 1;
                    } else {
                        return lhs.type.compareTo(rhs.type);
                    }
                }
            });
        }
    }

    public void addEmail(Email email) {
        if (email == null) {
            return;
        }

        if (!StringUtil.isValidEmail(email.value)) {
            return;
        }

        email.person = this;
        this.setHasContactInfo(true);
        if (!this.emails.contains(email)) {
            this.emails.add(email);
            Collections.sort(this.emails, new Comparator<Email>() {
                @Override
                public int compare(Email lhs, Email rhs) {
                    if (lhs.type == null && rhs.type == null) {
                        return 0;
                    } else if (lhs.type == null) {
                        return -1;
                    } else if (rhs.type == null) {
                        return 1;
                    } else {
                        return lhs.type.compareTo(rhs.type);
                    }
                }
            });
        }
    }

    public String getUserId() {
        return this.mUserId;
    }

    public void setUserId(String id) {
        mUserId = id;
    }

    public int getPersonId() {
        return mId;
    }

    public int getIsInvitedIntVal() {
        return mInvited;
    }

    public void setIsInvitedIntVal(int isInvited) {
        mInvited = isInvited;
    }

    public int getHasContactInfoIntVal() {
        return mHasContactInfo;
    }

    public void setHasContactInfoIntVal(int hasContactInfo) {
        mHasContactInfo = hasContactInfo;
    }

    public void setIsInvited(boolean isInvited) {
        this.mInvited = isInvited ? 1 : 0;
    }

    public String getFirstName() {
        if (this.mFirstName != null) {
            return this.mFirstName;
        } else {
            return "";
        }
    }

    public void setFirstName(String firstName) {
        mFirstName = firstName;
    }

    public String getLastName() {
        if (this.mFirstName != null) {
            return this.mLastName;
        } else {
            return "";
        }
    }

    public void setLastName(String lastName) {
        mLastName = lastName;
    }

    public String getImageUrl() {
        return this.mImageUrl;
    }

    public void setImageUrl(String url) {
        this.mImageUrl = url;
    }

    // endregion

    // region: Setters

    public long getPhoneContactID() {
        return this._phoneContactID;
    }

    public void setPhoneContactID(long id) {
        this._phoneContactID = id;
    }

    public int getIsConnectionIntVal() {
        return this.mIsConnection;
    }

    public void setIsConnectionIntVal(int isConnection) {
        this.mIsConnection = isConnection;
    }

    public boolean isConnection() {
        return this.mIsConnection == 1;
    }

    public boolean isInvited() {
        return this.mInvited == 1;
    }

    public void setImagePath(String path) {
        this.mImageUrl = path;
    }

    public String getFullName() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(this.mFirstName)) {
            stringBuilder.append(this.mFirstName);
        }

        if (!TextUtils.isEmpty(this.mLastName)) {
            if (!TextUtils.isEmpty(this.mFirstName)) {
                stringBuilder.append(" ");
            }
            stringBuilder.append(this.mLastName);
        }

        return stringBuilder.toString();
    }

    public String getDisplayName() {
        String displayName = "";

        if (!TextUtils.isEmpty(this.mFirstName)) {
            displayName = this.getFullName();
        } else if (!TextUtils.isEmpty(this.mLastName)) {
            displayName = this.getFullName();
        } else if (getEmails().size() > 0) {
            displayName = this.getEmail().value;
        } else if (getPhoneNumbers().size() > 0) {
            displayName = this.getPhoneNumber().value;
        }
        return displayName;
    }

    public DisplayType getDisplayType() {
        if (this.getUserId() == null) {
            if (this.hasContactInfo()) {
                if(this.isInvited()){
                    return DisplayType.Invited;
                }else{
                    return DisplayType.Invite;
                }
            } else {
                return DisplayType.NA;
            }
        } else if (!this.isConnection()) {
            return DisplayType.Connect;
        } else {
            return DisplayType.Default;
        }
    }

    public InviteInfo getInviteInfo() {
        if (this.hasContactInfo()) {
            boolean hasPhone = this.hasPhone();
            boolean hasEmail = this.hasEmail();
            boolean hasManyEmails = this.getEmails().size() > 1;
            boolean hasManyPhones = this.getPhoneNumbers().size() > 1;
            if (hasPhone && hasEmail) {
                return new InviteInfo(InviteType.PhoneOrEmail, InviteSelection.Many);
            } else if (hasPhone) {
                if (hasManyPhones) {
                    return new InviteInfo(InviteType.PhoneOnly, InviteSelection.One);
                } else {
                    return new InviteInfo(InviteType.PhoneOnly, InviteSelection.Many);
                }
            } else if (hasEmail) {
                if (hasManyEmails) {
                    return new InviteInfo(InviteType.EmailOnly, InviteSelection.Many);
                } else {
                    return new InviteInfo(InviteType.EmailOnly, InviteSelection.One);
                }
            } else {
                return new InviteInfo(InviteType.NA);
            }
        } else {
            return new InviteInfo(InviteType.NA);
        }
    }

    public PhoneNumber getPhoneNumber() {
        return this.phoneNumbers.size() > 0 ? this.phoneNumbers.get(0) : null;
    }

    public Email getEmail() {
        return this.emails.size() > 0 ? this.emails.get(0) : null;
    }

    public long getPhoneContactVersion() {
        return _phoneContactVersion;
    }

    public void setPhoneContactVersion(long version) {
        this._phoneContactVersion = version;
    }

    public void setAutoincrementID(int id) {
        this.mId = id;
    }

    public void setFullName(String firstName, String lastName) {
        this.mFirstName = firstName;
        this.mLastName = lastName;
    }

    public void setHasContactInfo(boolean hasContactInfo) {
        setHasContactInfoIntVal(hasContactInfo ? 1 : 0);
    }

    public void setIsConnection(boolean isConnection) {
        this.mIsConnection = isConnection ? 1 : 0;
    }

    // endregion

    // region: DBFlow methods

    public void addPhones(List<PhoneNumber> phoneNumbers) {
        if (this.phoneNumbers == null) {
            this.phoneNumbers = new ArrayList<>();
        }
        for (PhoneNumber number : phoneNumbers) {
            this.addPhone(number);
        }
    }

    public void addEmails(List<Email> emails) {
        if (this.emails == null) {
            this.emails = new ArrayList<>();
        }

        for (Email email : emails) {
            this.addEmail(email);
        }
    }

    // endregion

    // region: Other methods

    @Override
    public boolean delete(@NotNull DatabaseWrapper wrapper) {
        for (Email email : getEmails()) {
            email.delete(wrapper);
        }
        for (PhoneNumber phoneNumber : this.getPhoneNumbers()) {
            phoneNumber.delete(wrapper);
        }

        return super.delete(wrapper);
    }

    public Boolean hasImage() {
        return !TextUtils.isEmpty(mImageUrl);
    }

    public String getFirstLetter() {
        if (TextUtils.isEmpty(this.mFirstName)) {
            return "";
        }

        return this.mFirstName.substring(0, 1);
    }

    public boolean hasEmail() {
        return this.emails.size() > 0;
    }

    public boolean hasPhone() {
        return this.phoneNumbers.size() > 0;
    }

    public boolean hasContactInfo() {
        return this.mHasContactInfo == 1;
    }

    public boolean hasAnyEmails(List<String> emails) {

        for (Email myEmail : this.emails) {
            for (String givenEmail : emails) {
                if (givenEmail.equals(myEmail.value)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean hasAnyPhones(List<String> phones) {

        for (PhoneNumber myPhone : this.phoneNumbers) {
            for (String givenPhone : phones) {
                if (givenPhone.equals(myPhone.value)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Gets a list of phone numbers which could be used as a phone number for another user.
     *
     * @return A list of phone number which could be for other users.
     */
    public List<PhoneNumber> getSearchablePhones() {
        List<PhoneNumber> searchablePhones = new ArrayList<>();

        for (PhoneNumber phone : this.phoneNumbers) {
            if (PhoneFormatUtil.isSearchable(phone.value)) {
                searchablePhones.add(phone);
            }
        }

        return searchablePhones;
    }

    // endregion
}
