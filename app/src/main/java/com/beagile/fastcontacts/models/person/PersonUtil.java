package com.beagile.fastcontacts.models.person;

import android.database.Cursor;

import com.beagile.fastcontacts.config.FastContactsDatabase;
import com.beagile.fastcontacts.models.person.elements.Email;
import com.beagile.fastcontacts.models.person.elements.PhoneNumber;
import com.beagile.fastcontacts.utils.CursorUtil;
import com.dbflow5.database.DatabaseWrapper;
import com.dbflow5.query.Select;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by josh on 9/20/15.
 * <p>
 * Helper for Person to do non-instance related operations and queries.
 */
public class PersonUtil {

    private PersonUtil() {
    }

    public static Person getPersonFromStorageWithAutoIncrementId(int autoIncrementId) {
        return new Select().from(Person.class)
                .where(Person_Table.id.eq(autoIncrementId))
                .querySingle(FastContactsDatabase.instance());
    }

    public static Person getPersonFromStorageWithUserId(String userId) {
        Person person = new Select()
                .from(Person.class)
                .where(Person_Table.user_id.eq(userId))
                .querySingle(FastContactsDatabase.instance());

        if (person == null) {
            person = new Person(userId);
        }

        return person;
    }

    @Nullable
    public static Integer lookupPersonAutoIncrementIDWithContactInformation(@NotNull Person person) {

        Integer autoincrementID = _lookupPersonAutoincrementIDBasedOnEmails(person.getEmails());
        if (autoincrementID != null) {
            return autoincrementID;
        }

        autoincrementID = _lookupPersonAutoincrementIDBasedOnPhoneNumbers(person.getPhoneNumbers());
        if (autoincrementID != null) {
            return autoincrementID;
        }

        return null;
    }

    @Nullable
    private static Integer _lookupPersonAutoincrementIDBasedOnEmails(@NotNull List<Email> emails) {

        if (emails.size() > 0) {
            DatabaseWrapper database = FastContactsDatabase.instance().getWritableDatabase();
            StringBuilder rawStatementBuilder = new StringBuilder();

            rawStatementBuilder.append("SELECT ");
            rawStatementBuilder.append("person_id");
            rawStatementBuilder.append(" FROM ");
            rawStatementBuilder.append(Email.class.getSimpleName());

            for (Email email : emails) {
                rawStatementBuilder.append(email == emails.get(0) ? " WHERE " : " OR ");
                rawStatementBuilder.append("value");
                rawStatementBuilder.append(" = '");
                rawStatementBuilder.append(email.value);
                rawStatementBuilder.append("'");
            }

            String rawStatement = rawStatementBuilder.toString();

            try (Cursor cursor = database.rawQuery(rawStatement, null)) {
                if (cursor.moveToFirst()) {
                    return CursorUtil.getInt(cursor, "person_id");
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    @Nullable
    private static Integer _lookupPersonAutoincrementIDBasedOnPhoneNumbers(@NotNull List<PhoneNumber> phoneNumbers) {

        if (phoneNumbers.size() > 0) {
            DatabaseWrapper database = FastContactsDatabase.instance()
                    .getWritableDatabase();
            StringBuilder rawStatementBuilder = new StringBuilder();

            rawStatementBuilder.append("SELECT ");
            rawStatementBuilder.append("person_id");
            rawStatementBuilder.append(" FROM ");
            rawStatementBuilder.append(PhoneNumber.class.getSimpleName());

            for (PhoneNumber phoneNumber : phoneNumbers) {
                rawStatementBuilder.append(phoneNumber == phoneNumbers.get(0) ? " WHERE " : " OR ");
                rawStatementBuilder.append("value");
                rawStatementBuilder.append(" = '");
                rawStatementBuilder.append(phoneNumber.value);
                rawStatementBuilder.append("'");
            }

            String rawStatement = rawStatementBuilder.toString();

            try (Cursor cursor = database.rawQuery(rawStatement, null)) {
                if (cursor.moveToFirst()) {
                    return CursorUtil.getInt(cursor, "person_id");
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    public static Person getPersonWithUserId(int openTimeUserID) {
        return new Select().from(Person.class)
                .where(Person_Table.id.eq(openTimeUserID))
                .querySingle(FastContactsDatabase.instance());
    }
}
