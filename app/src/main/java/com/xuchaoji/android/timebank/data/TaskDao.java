package com.xuchaoji.android.timebank.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TaskDao {

    @Query("SELECT * FROM task_records ORDER BY id DESC")
    LiveData<List<TaskRecord>> getAllTasks();

    @Query("SELECT * FROM task_records ORDER BY id DESC")
    List<TaskRecord> getAllTasksSync();

    @Query("SELECT * FROM task_records WHERE id = :id")
    LiveData<TaskRecord> getTaskLive(long id);

    @Insert
    long insert(TaskRecord task);

    @Update
    void update(TaskRecord task);

    @Query("SELECT * FROM task_records WHERE id = :id")
    TaskRecord getTaskById(long id);

    @Query("DELETE FROM task_records")
    void deleteAll();
}
