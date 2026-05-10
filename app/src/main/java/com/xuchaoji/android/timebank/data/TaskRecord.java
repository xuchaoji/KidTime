package com.xuchaoji.android.timebank.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "task_records")
public class TaskRecord {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_REVIEWING = 1;
    public static final int STATUS_COMPLETED = 2;

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;

    public int rewardMinutes;

    public int status;

    public TaskRecord() {
    }

    @androidx.room.Ignore
    public TaskRecord(String title, int rewardMinutes, int status) {
        this.title = title;
        this.rewardMinutes = rewardMinutes;
        this.status = status;
    }
}
