package com.xuchaoji.android.timebank.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PrefsManager {

    private static final String PREFS_NAME = "kidtime_prefs";
    private static volatile PrefsManager instance;

    private static final String KEY_PARENT_PIN = "PARENT_PIN";
    private static final String KEY_DAILY_ALLOWANCE = "DAILY_ALLOWANCE";
    private static final String KEY_CURRENT_BALANCE = "CURRENT_BALANCE";
    private static final String KEY_OVERDRAFT_DEBT = "OVERDRAFT_DEBT";
    private static final String KEY_LAST_RESET_DATE = "LAST_RESET_DATE";

    private final SharedPreferences prefs;

    private PrefsManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void init(Context context) {
        if (instance == null) {
            synchronized (PrefsManager.class) {
                if (instance == null) {
                    instance = new PrefsManager(context);
                }
            }
        }
    }

    public static PrefsManager getInstance() {
        return instance;
    }

    public String getParentPin() {
        return prefs.getString(KEY_PARENT_PIN, "1234");
    }

    public void setParentPin(String pin) {
        prefs.edit().putString(KEY_PARENT_PIN, pin).apply();
    }

    public int getDailyAllowance() {
        return prefs.getInt(KEY_DAILY_ALLOWANCE, 30);
    }

    public void setDailyAllowance(int minutes) {
        prefs.edit().putInt(KEY_DAILY_ALLOWANCE, minutes).apply();
    }

    public int getCurrentBalance() {
        return prefs.getInt(KEY_CURRENT_BALANCE, 30);
    }

    public void setCurrentBalance(int balance) {
        prefs.edit().putInt(KEY_CURRENT_BALANCE, balance).apply();
    }

    public int getOverdraftDebt() {
        return prefs.getInt(KEY_OVERDRAFT_DEBT, 0);
    }

    public void setOverdraftDebt(int debt) {
        prefs.edit().putInt(KEY_OVERDRAFT_DEBT, debt).apply();
    }

    public String getLastResetDate() {
        return prefs.getString(KEY_LAST_RESET_DATE, "");
    }

    public void setLastResetDate(String date) {
        prefs.edit().putString(KEY_LAST_RESET_DATE, date).apply();
    }

    public static String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}
