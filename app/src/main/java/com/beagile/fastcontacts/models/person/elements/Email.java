package com.beagile.fastcontacts.models.person.elements;

import com.beagile.fastcontacts.config.FastContactsDatabase;
import com.beagile.fastcontacts.models.person.Person;
import com.dbflow5.annotation.Column;
import com.dbflow5.annotation.ForeignKey;
import com.dbflow5.annotation.PrimaryKey;
import com.dbflow5.annotation.Table;
import com.dbflow5.structure.BaseModel;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Contract;

import java.io.Serializable;
import java.util.Objects;

@Table(database = FastContactsDatabase.class)
public class Email extends BaseModel implements Serializable {

    private static final String HASH_SALT = "3i5UZlnHZUEt5gqiXsesqDfkxcKtbaKwPYJBMOrg";

    @ForeignKey(stubbedRelationship = true)
    public Person person;

    @Column
    public String value;

    @PrimaryKey()
    @SerializedName("hash_value")
    @Column(name = "hash_value")
    public String hashValue;

    @Column
    public String type;

    public Email() {
        this("", "");
    }

    public Email(String hashValue) {
        this("", "");
        this.hashValue = hashValue;
    }

    public Email(String value, String type) {
        this.setEmail(value);
        this.type = type;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Email email = (Email) o;

        return Objects.equals(hashValue, email.hashValue);
    }

    @Override
    public int hashCode() {
        int result = hashValue != null ? hashValue.hashCode() : 0;
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

    private void setEmail(String email) {
        if (email != null) {
            this.value = email.toLowerCase();
            this.hashValue =  DigestUtils.sha256Hex(this.value + HASH_SALT);
        } else {
            this.value = "";
            this.hashValue = null;
        }
    }
}
