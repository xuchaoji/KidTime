package com.xuchaoji.android.timebank.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.xuchaoji.android.timebank.R;
import com.xuchaoji.android.timebank.data.TaskRecord;
import com.xuchaoji.android.timebank.manager.TimeBankManager;

import java.util.ArrayList;
import java.util.List;

public class TaskManageActivity extends AppCompatActivity {

    private ListView taskListView;
    private TaskAdapter adapter;
    private List<TaskRecord> tasks = new ArrayList<>();
    private TimeBankManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_manage);

        manager = TimeBankManager.getInstance();
        taskListView = findViewById(R.id.task_list);
        adapter = new TaskAdapter();
        taskListView.setAdapter(adapter);

        findViewById(R.id.btn_add_task).setOnClickListener(v -> showAddTaskDialog());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTasks();
    }

    private void refreshTasks() {
        tasks = manager.getAllTasksSync();
        adapter.notifyDataSetChanged();
    }

    private void showAddTaskDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_task, null);
        EditText titleInput = dialogView.findViewById(R.id.task_title_input);
        EditText rewardInput = dialogView.findViewById(R.id.task_reward_input);

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_task_title)
                .setView(dialogView)
                .setPositiveButton(R.string.add, (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String rewardStr = rewardInput.getText().toString().trim();
                    if (title.isEmpty() || rewardStr.isEmpty()) {
                        Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int reward = Integer.parseInt(rewardStr);
                    if (reward <= 0) return;
                    manager.createTask(title, reward);
                    refreshTasks();
                    Toast.makeText(this, R.string.task_created, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private class TaskAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return tasks.size();
        }

        @Override
        public TaskRecord getItem(int position) {
            return tasks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return tasks.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(TaskManageActivity.this)
                        .inflate(R.layout.item_task, parent, false);
                holder = new ViewHolder();
                holder.titleText = convertView.findViewById(R.id.item_task_title);
                holder.rewardText = convertView.findViewById(R.id.item_task_reward);
                holder.statusText = convertView.findViewById(R.id.item_task_status);
                holder.approveBtn = convertView.findViewById(R.id.item_task_approve);
                holder.rejectBtn = convertView.findViewById(R.id.item_task_reject);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            TaskRecord task = getItem(position);
            holder.titleText.setText(task.title);
            holder.rewardText.setText(getString(R.string.task_reward_format, task.rewardMinutes));

            switch (task.status) {
                case TaskRecord.STATUS_PENDING:
                    holder.statusText.setText(R.string.status_pending);
                    holder.statusText.setTextColor(0xFF757575);
                    holder.approveBtn.setVisibility(View.GONE);
                    holder.rejectBtn.setVisibility(View.GONE);
                    break;
                case TaskRecord.STATUS_REVIEWING:
                    holder.statusText.setText(R.string.status_reviewing);
                    holder.statusText.setTextColor(0xFFFFA000);
                    holder.approveBtn.setVisibility(View.VISIBLE);
                    holder.rejectBtn.setVisibility(View.VISIBLE);
                    break;
                case TaskRecord.STATUS_COMPLETED:
                    holder.statusText.setText(R.string.status_completed);
                    holder.statusText.setTextColor(0xFF4CAF50);
                    holder.approveBtn.setVisibility(View.GONE);
                    holder.rejectBtn.setVisibility(View.GONE);
                    break;
            }

            holder.approveBtn.setOnClickListener(v -> {
                manager.approveTask(task);
                refreshTasks();
                Toast.makeText(TaskManageActivity.this, R.string.task_approved, Toast.LENGTH_SHORT).show();
            });

            holder.rejectBtn.setOnClickListener(v -> {
                manager.rejectTask(task);
                refreshTasks();
                Toast.makeText(TaskManageActivity.this, R.string.task_rejected, Toast.LENGTH_SHORT).show();
            });

            return convertView;
        }
    }

    private static class ViewHolder {
        TextView titleText;
        TextView rewardText;
        TextView statusText;
        Button approveBtn;
        Button rejectBtn;
    }
}
