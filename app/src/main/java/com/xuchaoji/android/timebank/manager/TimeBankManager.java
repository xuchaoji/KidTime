package com.xuchaoji.android.timebank.manager;

import androidx.lifecycle.LiveData;

import com.xuchaoji.android.timebank.data.AppDatabase;
import com.xuchaoji.android.timebank.data.PrefsManager;
import com.xuchaoji.android.timebank.data.TaskDao;
import com.xuchaoji.android.timebank.data.TaskRecord;
import com.xuchaoji.android.timebank.data.TimeTransaction;
import com.xuchaoji.android.timebank.data.TransactionDao;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TimeBankManager {

    private static volatile TimeBankManager instance;

    private final TransactionDao transactionDao;
    private final TaskDao taskDao;
    private final PrefsManager prefs;
    private final ExecutorService executor;

    private TimeBankManager(AppDatabase database) {
        this.transactionDao = database.transactionDao();
        this.taskDao = database.taskDao();
        this.prefs = PrefsManager.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public static void init(AppDatabase database) {
        if (instance == null) {
            synchronized (TimeBankManager.class) {
                if (instance == null) {
                    instance = new TimeBankManager(database);
                }
            }
        }
    }

    public static TimeBankManager getInstance() {
        return instance;
    }

    public LiveData<List<TimeTransaction>> getAllTransactions() {
        return transactionDao.getAllTransactions();
    }

    public void checkAndApplyDailyReset() {
        String today = PrefsManager.getTodayDate();
        String lastResetDate = prefs.getLastResetDate();

        if (!today.equals(lastResetDate)) {
            int allowance = prefs.getDailyAllowance();
            int debt = prefs.getOverdraftDebt();
            int actualGrant = allowance - debt;

            int newBalance;
            int newDebt;
            int transactionAmount;

            if (actualGrant > 0) {
                newBalance = actualGrant;
                newDebt = 0;
                transactionAmount = actualGrant;
            } else {
                newBalance = 0;
                newDebt = Math.abs(actualGrant);
                transactionAmount = allowance;
            }

            prefs.setCurrentBalance(newBalance);
            prefs.setOverdraftDebt(newDebt);
            prefs.setLastResetDate(today);

            TimeTransaction txn = new TimeTransaction(
                    transactionAmount,
                    TimeTransaction.TYPE_DAILY_GRANT,
                    System.currentTimeMillis(),
                    "每日发放"
            );
            executor.execute(() -> transactionDao.insert(txn));
        }
    }

    public void applyPenalty(int penaltyMinutes, String reason) {
        int currentBalance = prefs.getCurrentBalance();
        int newBalance = currentBalance - penaltyMinutes;

        if (newBalance >= 0) {
            prefs.setCurrentBalance(newBalance);
        } else {
            int currentDebt = prefs.getOverdraftDebt();
            prefs.setOverdraftDebt(currentDebt + Math.abs(newBalance));
            prefs.setCurrentBalance(0);
        }

        TimeTransaction txn = new TimeTransaction(
                -penaltyMinutes,
                TimeTransaction.TYPE_PENALTY,
                System.currentTimeMillis(),
                reason
        );
        executor.execute(() -> transactionDao.insert(txn));
    }

    public void rewardTime(int rewardMinutes, String reason) {
        int currentBalance = prefs.getCurrentBalance();
        prefs.setCurrentBalance(currentBalance + rewardMinutes);

        TimeTransaction txn = new TimeTransaction(
                rewardMinutes,
                TimeTransaction.TYPE_TASK_REWARD,
                System.currentTimeMillis(),
                reason
        );
        executor.execute(() -> transactionDao.insert(txn));
    }

    public void consumeTime(int minutes) {
        int currentBalance = prefs.getCurrentBalance();
        int newBalance = Math.max(0, currentBalance - minutes);
        prefs.setCurrentBalance(newBalance);

        TimeTransaction txn = new TimeTransaction(
                -minutes,
                TimeTransaction.TYPE_USAGE_CONSUME,
                System.currentTimeMillis(),
                "应用使用消耗"
        );
        executor.execute(() -> transactionDao.insert(txn));
    }

    public int getCurrentBalance() {
        return prefs.getCurrentBalance();
    }

    public int getOverdraftDebt() {
        return prefs.getOverdraftDebt();
    }

    public int getDailyAllowance() {
        return prefs.getDailyAllowance();
    }

    public void setDailyAllowance(int minutes) {
        int oldAllowance = prefs.getDailyAllowance();
        int newAllowance = Math.max(1, minutes);
        if (newAllowance == oldAllowance) return;

        prefs.setDailyAllowance(newAllowance);

        int delta = newAllowance - oldAllowance;
        int newBalance = Math.max(0, prefs.getCurrentBalance() + delta);
        prefs.setCurrentBalance(newBalance);

        TimeTransaction txn = new TimeTransaction(
                delta,
                TimeTransaction.TYPE_DAILY_GRANT,
                System.currentTimeMillis(),
                "每日额度调整 (" + oldAllowance + "→" + newAllowance + ")"
        );
        executor.execute(() -> transactionDao.insert(txn));
    }

    public List<TimeTransaction> getAllTransactionsSync() {
        return transactionDao.getAllTransactionsSync();
    }

    public List<TaskRecord> getAllTasksSync() {
        return taskDao.getAllTasksSync();
    }

    public long createTask(String title, int rewardMinutes) {
        TaskRecord task = new TaskRecord(title, rewardMinutes, TaskRecord.STATUS_PENDING);
        return taskDao.insert(task);
    }

    public void updateTask(TaskRecord task) {
        taskDao.update(task);
    }

    public void approveTask(TaskRecord task) {
        task.status = TaskRecord.STATUS_COMPLETED;
        taskDao.update(task);
        rewardTime(task.rewardMinutes, "任务奖励: " + task.title);
    }

    public void rejectTask(TaskRecord task) {
        task.status = TaskRecord.STATUS_PENDING;
        taskDao.update(task);
    }

    public boolean verifyPin(String pin) {
        return prefs.getParentPin().equals(pin);
    }

    public void changePin(String newPin) {
        prefs.setParentPin(newPin);
    }
}
