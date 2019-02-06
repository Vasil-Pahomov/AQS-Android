package ru.clevertec.AQS.storage;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.Date;
import java.util.List;

@Dao
public interface DLogDao {

    @Query("SELECT * FROM dlog WHERE rtime >= :from AND rtime < :to")
    List<DLog> getInRange(long from, long to);

    @Insert
    void insertAll(DLog... dLogs);

    @Query("DELETE FROM dlog")
    void wipe();

    @Query("SELECT MAX(id) FROM dlog")
    int getLastId();
}
