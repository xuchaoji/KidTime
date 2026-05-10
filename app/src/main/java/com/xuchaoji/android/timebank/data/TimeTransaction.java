package com.xuchaoji.android.timebank.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "time_transactions")
public class TimeTransaction {

    public static final int TYPE_DAILY_GRANT = 1;
    public static final int TYPE_TASK_REWARD = 2;
    public static final int TYPE_USAGE_CONSUME = 3;
    public static final int TYPE_PENALTY = 4;

    @PrimaryKey(autoGenerate = true)
    public long id;

    public int amount;

    public int type;

    public long timestamp;

    public String description;

    public TimeTransaction() {
    }

    @androidx.room.Ignore
    public TimeTransaction(int amount, int type, long timestamp, String description) {
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
        this.description = description;
    }
}
