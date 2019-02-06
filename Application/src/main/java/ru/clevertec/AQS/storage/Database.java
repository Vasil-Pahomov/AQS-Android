package ru.clevertec.AQS.storage;

import android.arch.persistence.db.SupportSQLiteOpenHelper;
import android.arch.persistence.room.DatabaseConfiguration;
import android.arch.persistence.room.InvalidationTracker;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.support.annotation.NonNull;

@android.arch.persistence.room.Database(version = 1, entities = {DLog.class}, exportSchema = false)
public abstract class Database extends RoomDatabase {
    public abstract DLogDao getDLogDao();

    private static Database database;
    public static Database getDatabase(Context context) {
        if (database == null) {
            //todo: find out why removing allowMainThreadQueries causes crash despite all DB requests seem to be done in threads!
            database = Room.databaseBuilder(context, Database.class, "dlogdb").allowMainThreadQueries().build();
        }
        return database;
    }

}
