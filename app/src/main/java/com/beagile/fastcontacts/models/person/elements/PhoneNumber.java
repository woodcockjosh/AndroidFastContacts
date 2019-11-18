package com.beagile.fastcontacts.models.person.elements;

import android.util.Log;

import com.beagile.fastcontacts.config.FastContactsDatabase;
import com.beagile.fastcontacts.models.person.Person;
import com.beagile.fastcontacts.utils.PhoneFormatUtil;
import com.dbflow5.annotation.Column;
import com.dbflow5.annotation.ForeignKey;
import com.dbflow5.annotation.PrimaryKey;
import com.dbflow5.annotation.Table;
import com.dbflow5.database.DatabaseWrapper;
import com.dbflow5.structure.BaseModel;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

@Table(database = FastContactsDatabase.class)
public class PhoneNumber extends BaseModel implements Serializable {

    private static final String HASH_SALT = "SUS3g4QfQvaFh9Yg4RZx5eh5COWP39PJ4t2fPVcZ";

    @ForeignKey(stubbedRelationship = true)
    public Person person;

    @Column
    public String value;

    @PrimaryKey()
    @SerializedName("hash_value")
    @Expose
    @Column(name = "hash_value")
    public String hashValue;

    @Column
    public String type;

    public PhoneNumber(String hashValue) {
        this("", "");
        this.hashValue = hashValue;
    }

    public PhoneNumber() {
        this("", "");
    }

    public PhoneNumber(String value, String type) {
        this.setPhoneNumber(value);
        this.type = type;
    }

    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PhoneNumber phoneNumber = (PhoneNumber) o;

        return Objects.equals(hashValue, phoneNumber.hashValue);
    }

    @Override
    public int hashCode() {
        int result = hashValue != null ? hashValue.hashCode() : 0;
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

    private void setPhoneNumber(String phone) {
        if (phone != null && PhoneFormatUtil.isValid(phone)) {
            phone = PhoneFormatUtil.format(phone);
            this.value = phone;
            this.hashValue = DigestUtils.sha256Hex(this.value + HASH_SALT);
        } else {
            this.value = "";
            this.hashValue = null;
        }
    }

    @Override
    public boolean save(@NotNull DatabaseWrapper wrapper){
        if(this.person != null){
            return super.save(wrapper);
        }else{
            Log.e(this.getClass().getSimpleName(),"Attempted to save phone number " + this.value + " without a person associated");
            return false;
        }
    }
}
