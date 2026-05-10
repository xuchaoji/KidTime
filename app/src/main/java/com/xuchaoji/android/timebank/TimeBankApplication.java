package com.xuchaoji.android.timebank;

import android.app.Application;

import com.xuchaoji.android.timebank.data.AppDatabase;
import com.xuchaoji.android.timebank.data.PrefsManager;
import com.xuchaoji.android.timebank.manager.TimeBankManager;

public class TimeBankApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        PrefsManager.init(this);
        AppDatabase database = AppDatabase.getDatabase(this);
        TimeBankManager.init(database);
    }
}
