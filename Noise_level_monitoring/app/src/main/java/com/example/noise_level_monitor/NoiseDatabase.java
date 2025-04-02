package com.example.noise_level_monitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class NoiseDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "noise_level_database.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_NAME = "noise_measurements";

    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_NOISE_LEVEL = "noise_level";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_STUDY_SUITABILITY = "study_suitability";

    private static NoiseDatabase instance;

    private NoiseDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);//initilize a instance of helpre
    }

    public static synchronized NoiseDatabase getInstance(Context context) { // application context
    //access database instance
        if (instance == null) {
            instance = new NoiseDatabase(context.getApplicationContext());
        }
        return instance;
    }//context is SDK current state of enviro

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NOISE_LEVEL + " REAL, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                COLUMN_STUDY_SUITABILITY + " TEXT" +
                ")";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database schema upgrades if necessary
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
//if version incresaese all tables are dropped called automatically
    public void insert(DataAddonHistory measurement) {
        SQLiteDatabase db = this.getWritableDatabase();//this  is helpre
        ContentValues values = new ContentValues(); //cv -key value pairs
        values.put(COLUMN_NOISE_LEVEL, measurement.getNoiseLevel());
        values.put(COLUMN_TIMESTAMP, measurement.getTimestamp());
        values.put(COLUMN_STUDY_SUITABILITY, measurement.getStudySuitability());
        db.insert(TABLE_NAME, null, values);
    }

    public List<DataAddonHistory> getLatest100Measurements() {
        SQLiteDatabase db = this.getReadableDatabase();//reda only mode
        String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT 100";
        Cursor cursor = db.rawQuery(query, null);
        List<DataAddonHistory> measurements = new ArrayList<>();
        if (cursor.moveToFirst()) {//check and moves to first row
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                double noiseLevel = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_NOISE_LEVEL));
                long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                String studySuitability = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STUDY_SUITABILITY));
                measurements.add(new DataAddonHistory(id, noiseLevel, timestamp, studySuitability));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return measurements; // Do not close the database here
    }

    public int getTotalMeasurementsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + TABLE_NAME;
        Cursor cursor = db.rawQuery(query, null);//return cursot(result set)
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count; // Do not close the database here
    }
//total rows
    public void trimOldMeasurements() {
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_ID + " NOT IN " +
                "(SELECT " + COLUMN_ID + " FROM " + TABLE_NAME + " ORDER BY " + COLUMN_TIMESTAMP + " DESC LIMIT 100)";
        db.execSQL(query);
    }
}
