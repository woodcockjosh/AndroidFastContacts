package com.beagile.fastcontacts.models.person.elements;

import com.beagile.fastcontacts.config.FastContactsDatabase;
import com.beagile.fastcontacts.models.person.Person;
import com.dbflow5.annotation.Column;
import com.dbflow5.annotation.ForeignKey;
import com.dbflow5.annotation.PrimaryKey;
import com.dbflow5.annotation.Table;
import com.dbflow5.structure.BaseModel;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Contract;

import java.io.Serializable;
import java.util.Objects;

@Table(database = FastContactsDatabase.class)
public class PhoneNumber extends BaseModel implements Serializable {

    private static final String HASH_SALT = "SUS3g4QfQvaFh9Yg4RZx5eh5COWP39PJ4t2fPVcZ";

    @ForeignKey(stubbedRelationship = true)
    public Person person;

    @PrimaryKey()
    @Column
    public String value;

    @Column(name = "hash_value")
    public String hashValue;

    @Column
    public String type;

    public PhoneNumber() {
        this("", "");
    }

    public PhoneNumber(String value, String type) {
        this._setPhoneNumber(value);
        this.type = type;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PhoneNumber phoneNumber = (PhoneNumber) o;

        return Objects.equals(value, phoneNumber.value);
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PhoneNumber{" +
                ", value='" + value + '\'' +
                ", hashValue='" + hashValue + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    private void _setPhoneNumber(String phone) {
        if (phone != null) {
            this.value = phone.toLowerCase();
            this.hashValue = DigestUtils.sha256Hex(this.value + HASH_SALT);
        } else {
            this.value = "";
        }
    }
}
