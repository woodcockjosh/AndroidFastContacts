package com.beagile.fastcontacts.models.person.elements;

import android.database.Cursor;

import com.beagile.fastcontacts.config.FastContactsDatabase;
import com.beagile.fastcontacts.utils.CursorUtil;
import com.dbflow5.database.DatabaseWrapper;

import java.util.HashMap;
import java.util.Map;

public class PhoneNumberPersonIdHashMap extends HashMap<String, Integer> implements Map<String, Integer> {

    public PhoneNumberPersonIdHashMap() {

        DatabaseWrapper database = FastContactsDatabase.instance().getWritableDatabase();

        String rawStatement = "SELECT person_id, hash_value" +
                " FROM " +
                Email.class.getSimpleName();

        try (Cursor cursor = database.rawQuery(rawStatement, null)) {
            try {
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    String emailHash = CursorUtil.getString(cursor, "hash_value");
                    Integer personId = CursorUtil.getInt(cursor, "person_id");
                    this.put(emailHash, personId);
                }
            } finally {
                cursor.close();
            }
        }
    }
}
