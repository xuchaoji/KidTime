package com.xuchaoji.android.timebank.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.xuchaoji.android.timebank.R;
import com.xuchaoji.android.timebank.data.TimeTransaction;
import com.xuchaoji.android.timebank.manager.TimeBankManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ParentSettingsActivity extends AppCompatActivity {

    private TextView allowanceText;
    private TextView balanceText;
    private TextView debtText;
    private TimeBankManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_settings);

        manager = TimeBankManager.getInstance();
        manager.checkAndApplyDailyReset();

        allowanceText = findViewById(R.id.allowance_text);
        balanceText = findViewById(R.id.balance_text);
        debtText = findViewById(R.id.debt_text);

        refreshStats();

        findViewById(R.id.btn_allowance_plus).setOnClickListener(v -> {
            int current = manager.getDailyAllowance();
            manager.setDailyAllowance(current + 5);
            refreshStats();
        });

        findViewById(R.id.btn_allowance_minus).setOnClickListener(v -> {
            int current = manager.getDailyAllowance();
            if (current > 5) {
                manager.setDailyAllowance(current - 5);
                refreshStats();
            }
        });

        findViewById(R.id.btn_change_pin).setOnClickListener(v -> showChangePinDialog());
        findViewById(R.id.btn_penalty).setOnClickListener(v -> showPenaltyDialog());
        findViewById(R.id.btn_tasks).setOnClickListener(v -> {
            startActivity(new Intent(this, TaskManageActivity.class));
        });
        findViewById(R.id.btn_bonus).setOnClickListener(v -> showBonusDialog());
        findViewById(R.id.btn_transactions).setOnClickListener(v -> showTransactionHistory());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStats();
    }

    private void refreshStats() {
        int daily = manager.getDailyAllowance();
        int balance = manager.getCurrentBalance();
        int debt = manager.getOverdraftDebt();

        allowanceText.setText(getString(R.string.parent_allowance_format, daily));
        balanceText.setText(getString(R.string.parent_balance_format, balance));
        debtText.setText(getString(R.string.parent_debt_format, debt));
    }

    private void showChangePinDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_pin_change, null);
        EditText oldPinInput = dialogView.findViewById(R.id.old_pin_input);
        EditText newPinInput = dialogView.findViewById(R.id.new_pin_input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.change_pin_title)
                .setView(dialogView)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String oldPin = oldPinInput.getText().toString();
                    String newPin = newPinInput.getText().toString();
                    if (!manager.verifyPin(oldPin)) {
                        Toast.makeText(this, R.string.pin_incorrect_old, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (newPin.isEmpty()) {
                        Toast.makeText(this, R.string.pin_empty, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    manager.changePin(newPin);
                    Toast.makeText(this, R.string.pin_changed, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showPenaltyDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_penalty, null);
        EditText minutesInput = dialogView.findViewById(R.id.penalty_minutes_input);
        EditText reasonInput = dialogView.findViewById(R.id.penalty_reason_input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.apply_penalty_title)
                .setView(dialogView)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String minutesStr = minutesInput.getText().toString();
                    String reason = reasonInput.getText().toString();
                    if (minutesStr.isEmpty()) {
                        Toast.makeText(this, R.string.enter_minutes, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int minutes = Integer.parseInt(minutesStr);
                    if (minutes <= 0) return;
                    if (reason.isEmpty()) reason = getString(R.string.penalty_default_reason);
                    manager.applyPenalty(minutes, reason);
                    refreshStats();
                    Toast.makeText(this, R.string.penalty_applied, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showBonusDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_bonus, null);
        EditText minutesInput = dialogView.findViewById(R.id.bonus_minutes_input);
        EditText reasonInput = dialogView.findViewById(R.id.bonus_reason_input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_bonus_time)
                .setView(dialogView)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String minutesStr = minutesInput.getText().toString();
                    String reason = reasonInput.getText().toString();
                    if (minutesStr.isEmpty()) {
                        Toast.makeText(this, R.string.enter_minutes, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int minutes = Integer.parseInt(minutesStr);
                    if (minutes <= 0) return;
                    if (reason.isEmpty()) reason = getString(R.string.bonus_default_reason);
                    manager.rewardTime(minutes, reason);
                    refreshStats();
                    Toast.makeText(this, R.string.bonus_added, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showTransactionHistory() {
        List<TimeTransaction> transactions = manager.getAllTransactionsSync();

        if (transactions.isEmpty()) {
            Toast.makeText(this, R.string.no_transactions, Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        StringBuilder sb = new StringBuilder();
        for (TimeTransaction t : transactions) {
            String typeLabel;
            switch (t.type) {
                case TimeTransaction.TYPE_DAILY_GRANT: typeLabel = "每日发放"; break;
                case TimeTransaction.TYPE_TASK_REWARD: typeLabel = "任务奖励"; break;
                case TimeTransaction.TYPE_USAGE_CONSUME: typeLabel = "使用消耗"; break;
                case TimeTransaction.TYPE_PENALTY: typeLabel = "违规惩罚"; break;
                default: typeLabel = "其他"; break;
            }
            String sign = t.amount >= 0 ? "+" : "";
            sb.append(String.format(Locale.getDefault(),
                    "[%s] %s%s 分钟  %s\n%s\n\n",
                    typeLabel, sign, t.amount, sdf.format(new Date(t.timestamp)), t.description));
        }

        TextView textView = new TextView(this);
        textView.setText(sb.toString().trim());
        textView.setPadding(32, 24, 32, 24);
        textView.setTextSize(14f);
        textView.setMovementMethod(new ScrollingMovementMethod());
        textView.setVerticalScrollBarEnabled(true);

        new AlertDialog.Builder(this)
                .setTitle(R.string.transaction_history)
                .setView(textView)
                .setPositiveButton(R.string.close, null)
                .create()
                .show();
    }
}
