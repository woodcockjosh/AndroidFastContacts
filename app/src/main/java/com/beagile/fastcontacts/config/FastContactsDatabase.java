package com.beagile.fastcontacts.config;

import android.content.Context;

import com.dbflow5.annotation.Database;
import com.dbflow5.config.DBFlowDatabase;
import com.dbflow5.config.FlowConfig;
import com.dbflow5.config.FlowManager;

import net.danlew.android.joda.JodaTimeAndroid;

import org.jetbrains.annotations.NotNull;

import jonathanfinerty.once.Once;

@Database(
        version = FastContactsDatabase.VERSION,
        foreignKeyConstraintsEnforced = true
)
abstract public class FastContactsDatabase extends DBFlowDatabase {
    public static final int VERSION = 1;

    @NotNull
    public static DBFlowDatabase instance() {
        return FlowManager.getDatabase(FastContactsDatabase.class);
    }

    public static void initialize(Context context) {
        FlowManager.init(new FlowConfig.Builder(context).build());
        JodaTimeAndroid.init(context);
        Once.initialise(context);
    }
}
