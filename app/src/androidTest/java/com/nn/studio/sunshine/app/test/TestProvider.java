package com.nn.studio.sunshine.app.test;

/**
 * Created by jibi on 26/7/14.
 */
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import com.nn.studio.sunshine.data.WeatherContract.LocationEntry;
import com.nn.studio.sunshine.data.WeatherContract.WeatherEntry;
import com.nn.studio.sunshine.data.WeatherDbHelper;

import java.util.Map;
import java.util.Set;


public class TestProvider extends AndroidTestCase {

    public static final String LOG_TAG = TestProvider.class.getSimpleName();

    public void testDeleteDb() throws Throwable {
        mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    }

    public void testInsertReadProvider() {
        WeatherDbHelper dbHelper = new WeatherDbHelper(mContext);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues testValues = TestDb.createNorthPoleLocationValues();

        long locationRowId;
        locationRowId = db.insert(LocationEntry.TABLE_NAME, null, testValues);

        // Verify we got a row back.
        assertTrue(locationRowId != -1);
        Log.d(LOG_TAG, "New row id: " + locationRowId);

        Cursor cursor = mContext.getContentResolver().query(
                LocationEntry.buildLocationUri(locationRowId),
                null, // all columns
                null, // Columns for the "where" clause
                null, // Values for the "where" clause
                null // sortOrder
        );

        TestDb.validateCursor(cursor, testValues);

        ContentValues weatherValues = TestDb.createWeatherValues(locationRowId);

        long weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weatherValues);
        assertTrue(weatherRowId != -1);

        Cursor weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        TestDb.validateCursor(weatherCursor, weatherValues);

        dbHelper.close();
    }

    /* Tests to make sure that the content provider
    returns the proper content types when resolving Uris
    * */
    public void testGetType(){
        String type;
        ContentResolver mResolver = mContext.getContentResolver();

        /*Log.d(LOG_TAG, WeatherEntry.CONTENT_URI.toString());
        type = mResolver.getType(WeatherEntry.CONTENT_URI);
        assertEquals(WeatherEntry.CONTENT_TYPE, type)*/;

        String testLocation = "Mumbai,India";
        type = mResolver.getType(WeatherEntry.buildWeatherLocation(testLocation));
        assertEquals(WeatherEntry.CONTENT_TYPE, type);

        String testDate = "20140727";
        type = mResolver.getType(WeatherEntry.buildWeatherLocationWithDate(testLocation, testDate));
        assertEquals(WeatherEntry.CONTENT_ITEM_TYPE, type);

        type = mResolver.getType(LocationEntry.CONTENT_URI);
        assertEquals(LocationEntry.CONTENT_TYPE, type);

        // content://com.example.android.sunshine.app/location/1
        Uri location = LocationEntry.buildLocationUri(1L);
        type = mResolver.getType(location);
        assertEquals(LocationEntry.CONTENT_ITEM_TYPE, type);

    }
}