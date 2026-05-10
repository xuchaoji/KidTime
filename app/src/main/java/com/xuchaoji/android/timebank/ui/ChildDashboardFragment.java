package com.xuchaoji.android.timebank.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.button.MaterialButton;
import com.xuchaoji.android.timebank.R;
import com.xuchaoji.android.timebank.manager.TimeBankManager;
import com.xuchaoji.android.timebank.service.TimerForegroundService;
import com.xuchaoji.android.timebank.view.CircleTimerView;

public class ChildDashboardFragment extends Fragment {

    private ChildDashboardViewModel viewModel;
    private CircleTimerView circleTimerView;
    private MaterialButton toggleButton;
    private CardView warningCard;
    private TextView warningText;
    private TextView statusText;

    private BroadcastReceiver tickReceiver;
    private BroadcastReceiver secondTickReceiver;
    private BroadcastReceiver timeUpReceiver;
    private boolean isTimerRunning = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChildDashboardViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_child_dashboard, container, false);

        circleTimerView = view.findViewById(R.id.circle_timer_view);
        toggleButton = view.findViewById(R.id.btn_toggle);
        warningCard = view.findViewById(R.id.warning_card);
        warningText = view.findViewById(R.id.warning_text);
        statusText = view.findViewById(R.id.status_text);
        View btnParent = view.findViewById(R.id.btn_parent);
        View btnChildTasks = view.findViewById(R.id.btn_child_tasks);

        TimeBankManager manager = TimeBankManager.getInstance();
        int balance = manager.getCurrentBalance();
        int allowance = manager.getDailyAllowance();

        circleTimerView.setMaxTime(Math.max(allowance, balance));
        circleTimerView.setCurrentTime(balance);

        toggleButton.setOnClickListener(v -> {
            if (isTimerRunning) {
                stopTimer();
            } else {
                startTimer();
            }
        });

        viewModel.getOverdraftDebt().observe(getViewLifecycleOwner(), debt -> {
            if (debt != null && debt > 0) {
                warningText.setText(getString(R.string.warning_debt_format, debt));
                warningCard.setVisibility(View.VISIBLE);
            } else {
                warningCard.setVisibility(View.GONE);
            }
        });

        setupReceivers();

        btnParent.setOnClickListener(v -> showParentPinDialog());
        btnChildTasks.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), ChildTaskActivity.class)));

        viewModel.syncFromManager();
        updateButtonState(false);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        syncCircle();
    }

    private void syncCircle() {
        if (circleTimerView != null) {
            TimeBankManager mgr = TimeBankManager.getInstance();
            int allowance = mgr.getDailyAllowance();
            int balance = mgr.getCurrentBalance();
            circleTimerView.setMaxTime(Math.max(allowance, balance));
            circleTimerView.setCurrentTime(balance);
            viewModel.syncFromManager();
        }
    }

    private void updateButtonState(boolean running) {
        isTimerRunning = running;
        if (running) {
            toggleButton.setText(R.string.btn_stop);
            toggleButton.setIconResource(R.drawable.ic_stop);
            toggleButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.coral));
            toggleButton.setIconTint(
                    ContextCompat.getColorStateList(requireContext(), R.color.white));
            statusText.setText("🔴 计时中…");
        } else {
            toggleButton.setText(R.string.btn_start);
            toggleButton.setIconResource(R.drawable.ic_play);
            toggleButton.setBackgroundTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.timer_green));
            toggleButton.setIconTint(
                    ContextCompat.getColorStateList(requireContext(), R.color.white));
            statusText.setText("");
        }
    }

    private void setupReceivers() {
        tickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int remaining = intent.getIntExtra(TimerForegroundService.EXTRA_REMAINING_MINUTES, 0);
                circleTimerView.clearCountdown();
                circleTimerView.setCurrentTime(remaining);
                viewModel.syncFromManager();
            }
        };

        secondTickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int seconds = intent.getIntExtra(TimerForegroundService.EXTRA_REMAINING_SECONDS, 0);
                circleTimerView.setCountdownSeconds(seconds);
            }
        };

        timeUpReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateButtonState(false);
                viewModel.setTimerRunning(false);
                circleTimerView.clearCountdown();
                circleTimerView.setCurrentTime(0);
                if (getContext() != null) {
                    Toast.makeText(getContext(), R.string.time_up, Toast.LENGTH_LONG).show();
                }
            }
        };

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(requireContext());
        lbm.registerReceiver(tickReceiver, new IntentFilter(TimerForegroundService.ACTION_TICK));
        lbm.registerReceiver(secondTickReceiver, new IntentFilter(TimerForegroundService.ACTION_SECOND_TICK));
        lbm.registerReceiver(timeUpReceiver, new IntentFilter(TimerForegroundService.ACTION_TIME_UP));
    }

    private void startTimer() {
        TimeBankManager.getInstance().checkAndApplyDailyReset();
        viewModel.syncFromManager();

        int balance = TimeBankManager.getInstance().getCurrentBalance();
        if (balance <= 0) {
            Toast.makeText(getContext(), R.string.no_balance, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(getActivity(), TimerForegroundService.class);
        requireActivity().startService(intent);
        viewModel.setTimerRunning(true);
        updateButtonState(true);
    }

    private void stopTimer() {
        Intent intent = new Intent(getActivity(), TimerForegroundService.class);
        intent.setAction(TimerForegroundService.ACTION_STOP);
        requireActivity().startService(intent);
        viewModel.setTimerRunning(false);
        viewModel.syncFromManager();
        syncCircle();
        updateButtonState(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(requireContext());
        lbm.unregisterReceiver(tickReceiver);
        lbm.unregisterReceiver(secondTickReceiver);
        lbm.unregisterReceiver(timeUpReceiver);
    }

    private void showParentPinDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_pin_verify, null);
        EditText pinInput = dialogView.findViewById(R.id.pin_input);

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.parent_settings_title)
                .setView(dialogView)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String pin = pinInput.getText().toString();
                    if (TimeBankManager.getInstance().verifyPin(pin)) {
                        startActivity(new Intent(getActivity(), ParentSettingsActivity.class));
                    } else {
                        Toast.makeText(getContext(), R.string.pin_incorrect_old, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
