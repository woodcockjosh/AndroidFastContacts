package com.beagile.fastcontacts.application;

import android.app.Application;

import com.beagile.fastcontacts.config.FastContactsDatabase;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FastContactsDatabase.initialize(this);
    }
}
