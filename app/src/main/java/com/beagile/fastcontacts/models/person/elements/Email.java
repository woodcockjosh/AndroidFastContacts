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
public class Email extends BaseModel implements Serializable {

    private static final String HASH_SALT = "3i5UZlnHZUEt5gqiXsesqDfkxcKtbaKwPYJBMOrg";

    @ForeignKey(stubbedRelationship = true)
    public Person person;

    @PrimaryKey()
    @Column
    public String value;

    @Column(name = "hash_value")
    public String hashValue;

    @Column
    public String type;

    public Email() {
        this("", "");
    }

    public Email(String value, String type) {
        this._setEmail(value);
        this.type = type;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Email email = (Email) o;

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
        return "Email{" +
                ", value='" + value + '\'' +
                ", hashValue='" + hashValue + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    private void _setEmail(String email) {
        if (email != null) {
            this.value = email.toLowerCase();
            this.hashValue =  DigestUtils.sha256Hex(this.value + HASH_SALT);
        } else {
            this.value = "";
            this.hashValue = "";
        }
    }
}
