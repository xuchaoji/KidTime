package com.xuchaoji.android.timebank.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {

    @Query("SELECT * FROM time_transactions ORDER BY timestamp DESC")
    LiveData<List<TimeTransaction>> getAllTransactions();

    @Query("SELECT * FROM time_transactions ORDER BY timestamp DESC")
    List<TimeTransaction> getAllTransactionsSync();

    @Insert
    void insert(TimeTransaction transaction);

    @Query("DELETE FROM time_transactions")
    void deleteAll();
}
