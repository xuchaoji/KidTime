package com.xuchaoji.android.timebank.ui;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.xuchaoji.android.timebank.manager.TimeBankManager;

public class ChildDashboardViewModel extends ViewModel {

    private final MutableLiveData<Integer> currentBalance = new MutableLiveData<>();
    private final MutableLiveData<Integer> overdraftDebt = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isTimerRunning = new MutableLiveData<>(false);

    public ChildDashboardViewModel() {
        syncFromManager();
    }

    public MutableLiveData<Integer> getCurrentBalance() {
        return currentBalance;
    }

    public MutableLiveData<Integer> getOverdraftDebt() {
        return overdraftDebt;
    }

    public MutableLiveData<Boolean> getIsTimerRunning() {
        return isTimerRunning;
    }

    public void syncFromManager() {
        TimeBankManager manager = TimeBankManager.getInstance();
        if (manager != null) {
            currentBalance.postValue(manager.getCurrentBalance());
            overdraftDebt.postValue(manager.getOverdraftDebt());
        }
    }

    public void setTimerRunning(boolean running) {
        isTimerRunning.postValue(running);
    }
}
