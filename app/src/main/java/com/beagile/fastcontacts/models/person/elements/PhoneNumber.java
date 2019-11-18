package com.beagile.fastcontacts.models.person.elements;

import com.beagile.fastcontacts.config.FastContactsDatabase;
import com.beagile.fastcontacts.models.person.Person;
import com.dbflow5.annotation.Column;
import com.dbflow5.annotation.ForeignKey;
import com.dbflow5.annotation.PrimaryKey;
import com.dbflow5.annotation.Table;
import com.dbflow5.structure.BaseModel;

import org.jetbrains.annotations.Contract;

import java.io.Serializable;
import java.util.Objects;

@Table(database = FastContactsDatabase.class)
public class PhoneNumber extends BaseModel implements Serializable {

    @ForeignKey(stubbedRelationship = true)
    public Person person;

    @PrimaryKey()
    @Column
    public String value;

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

        PhoneNumber email = (PhoneNumber) o;

        return Objects.equals(value, email.value);
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
                ", type='" + type + '\'' +
                '}';
    }

    private void _setPhoneNumber(String email) {
        if (email != null) {
            this.value = email.toLowerCase();
        } else {
            this.value = "";
        }
    }
}
