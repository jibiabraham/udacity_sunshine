package com.nn.studio.sunshine.app.test;

/**
 * Created by jibi on 26/7/14.
 */
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
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

        Uri locationEntry = mContext.getContentResolver().insert(LocationEntry.CONTENT_URI, testValues);
        assertTrue(locationEntry != null);
        long locationRowId = ContentUris.parseId(locationEntry);
        assertTrue(locationRowId != -1);

        // Verify we got a row back.
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

        Uri weatherInsertUri = mContext.getContentResolver().insert(WeatherEntry.CONTENT_URI, weatherValues);
        assertTrue(weatherInsertUri != null);

        Cursor weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,
                null,
                null,
                null,
                null
        );

        TestDb.validateCursor(weatherCursor, weatherValues);

        addAllContentValues(weatherValues, testValues);

        // Get the joined Weather and Location data
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocation(TestDb.TEST_LOCATION),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        TestDb.validateCursor(weatherCursor, weatherValues);

        // Get the joined Weather and Location data with a start date
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocationWithStartDate(
                        TestDb.TEST_LOCATION, TestDb.TEST_DATE),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        TestDb.validateCursor(weatherCursor, weatherValues);

        // Get the joined Weather and Location data with an exact date
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocationWithDate(TestDb.TEST_LOCATION, TestDb.TEST_DATE),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
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

        type = mResolver.getType(WeatherEntry.buildWeatherLocation(TestDb.TEST_LOCATION));
        assertEquals(WeatherEntry.CONTENT_TYPE, type);

        type = mResolver.getType(WeatherEntry.buildWeatherLocationWithDate(TestDb.TEST_LOCATION, TestDb.TEST_DATE));
        assertEquals(WeatherEntry.CONTENT_ITEM_TYPE, type);

        type = mResolver.getType(LocationEntry.CONTENT_URI);
        assertEquals(LocationEntry.CONTENT_TYPE, type);

        // content://com.example.android.sunshine.app/location/1
        Uri location = LocationEntry.buildLocationUri(1L);
        type = mResolver.getType(location);
        assertEquals(LocationEntry.CONTENT_ITEM_TYPE, type);

    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void addAllContentValues(ContentValues destination, ContentValues source) {
        for (String key : source.keySet()) {
            destination.put(key, source.getAsString(key));
        }
    }
}