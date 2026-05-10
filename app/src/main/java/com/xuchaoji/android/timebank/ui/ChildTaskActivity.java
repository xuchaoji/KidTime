package com.xuchaoji.android.timebank.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.xuchaoji.android.timebank.R;
import com.xuchaoji.android.timebank.data.TaskRecord;
import com.xuchaoji.android.timebank.manager.TimeBankManager;

import java.util.ArrayList;
import java.util.List;

public class ChildTaskActivity extends AppCompatActivity {

    private ListView taskListView;
    private TaskAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_task);

        taskListView = findViewById(R.id.task_list);
        adapter = new TaskAdapter();
        taskListView.setAdapter(adapter);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTasks();
    }

    private void refreshTasks() {
        List<TaskRecord> all = TimeBankManager.getInstance().getAllTasksSync();
        List<TaskRecord> pending = new ArrayList<>();
        for (TaskRecord t : all) {
            if (t.status == TaskRecord.STATUS_PENDING) {
                pending.add(t);
            }
        }
        adapter.setTasks(pending);
    }

    private class TaskAdapter extends BaseAdapter {
        private List<TaskRecord> tasks = new ArrayList<>();

        void setTasks(List<TaskRecord> tasks) {
            this.tasks = tasks;
            notifyDataSetChanged();
        }

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
                convertView = LayoutInflater.from(ChildTaskActivity.this)
                        .inflate(R.layout.item_child_task, parent, false);
                holder = new ViewHolder();
                holder.titleText = convertView.findViewById(R.id.item_task_title);
                holder.rewardText = convertView.findViewById(R.id.item_task_reward);
                holder.acceptBtn = convertView.findViewById(R.id.item_task_accept);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            TaskRecord task = getItem(position);
            holder.titleText.setText(task.title);
            holder.rewardText.setText(getString(R.string.task_reward_format, task.rewardMinutes));

            holder.acceptBtn.setOnClickListener(v -> {
                task.status = TaskRecord.STATUS_REVIEWING;
                TimeBankManager.getInstance().updateTask(task);
                refreshTasks();
                Toast.makeText(ChildTaskActivity.this, R.string.task_accepted_toast, Toast.LENGTH_SHORT).show();
            });

            return convertView;
        }
    }

    private static class ViewHolder {
        TextView titleText;
        TextView rewardText;
        Button acceptBtn;
    }
}
