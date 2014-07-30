package com.nn.studio.sunshine.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import java.sql.SQLException;

/**
 * Created by jibi on 28/7/14.
 */
public class WeatherProvider extends ContentProvider {
    private final String TAG = this.getClass().getName();

    private static final int WEATHER = 100;
    private static final int WEATHER_WITH_LOCATION = 101;
    private static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    private static final int LOCATION = 300;
    private static final int LOCATION_ID = 301;

    private interface Tables {
        String WEATHER_JOIN_LOCATION = WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                WeatherContract.LocationEntry.TABLE_NAME +
                " ON " + WeatherContract.WeatherEntry.TABLE_NAME +
                "." + WeatherContract.WeatherEntry.COLUMN_LOC_KEY +
                " = " + WeatherContract.LocationEntry.TABLE_NAME +
                "." + WeatherContract.LocationEntry._ID;
    }

    private interface Queries {
        String LOCATION_BY_SETTING = WeatherContract.LocationEntry.TABLE_NAME +
                "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? ";
        String LOCATION_BY_SETTING_AND_STARTDATE = WeatherContract.LocationEntry.TABLE_NAME +
                "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                WeatherContract.WeatherEntry.COLUMN_DATETEXT + " >= ? ";
        String LOCATION_BY_SETTING_AND_DATE = WeatherContract.LocationEntry.TABLE_NAME +
                "." + WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND " +
                WeatherContract.WeatherEntry.COLUMN_DATETEXT + " = ? ";
    }

    private static UriMatcher sUriMatcher;
    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        sUriMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.PATH_WEATHER, WEATHER);
        sUriMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.PATH_WEATHER + "/*", WEATHER_WITH_LOCATION);
        sUriMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.PATH_WEATHER + "/*/*" , WEATHER_WITH_LOCATION_AND_DATE);

        sUriMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.PATH_LOCATION, LOCATION);
        sUriMatcher.addURI(WeatherContract.AUTHORITY, WeatherContract.PATH_LOCATION + "/#", LOCATION_ID);
    }

    private WeatherDbHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new WeatherDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        // Here's the switch statement that, given a URI, will determine what kind of request it is,
        // and query the database accordingly.
        Cursor retCursor = null;
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            // "weather/*/*"
            case WEATHER_WITH_LOCATION_AND_DATE:
            {
                retCursor = weatherByLocationAndDate(uri, projection, sortOrder);
                break;
            }
            // "weather/*"
            case WEATHER_WITH_LOCATION: {
                retCursor = weatherByLocation(uri, projection, sortOrder);
                break;
            }
            // "weather"
            case WEATHER: {
                queryBuilder.setTables(WeatherContract.WeatherEntry.TABLE_NAME);
                break;
            }
            // "location/*"
            case LOCATION_ID: {
                queryBuilder.setTables(WeatherContract.LocationEntry.TABLE_NAME);
                queryBuilder.appendWhere(WeatherContract.LocationEntry._ID + "=" + ContentUris.parseId(uri));
                break;
            }
            // "location"
            case LOCATION: {
                queryBuilder.setTables(WeatherContract.LocationEntry.TABLE_NAME);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        if(retCursor == null){
            SQLiteDatabase db = mOpenHelper.getReadableDatabase();
            retCursor = queryBuilder.query(
                    db,
                    projection,
                    selection,
                    selectionArgs,
                    null,
                    null,
                    sortOrder
            );
        }
        retCursor.setNotificationUri(getContext().getContentResolver(), uri);
        return retCursor;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        Log.d(TAG, Integer.toString(match));

        switch (match){
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case LOCATION_ID:
                return WeatherContract.LocationEntry.CONTENT_ITEM_TYPE;
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        Uri result;
        long _id;

        switch (match){
            case WEATHER:
                _id = db.insert(WeatherContract.WeatherEntry.TABLE_NAME, null, contentValues);
                if(_id > 0){
                    result = WeatherContract.WeatherEntry.buildWeatherUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            case LOCATION:
                _id = db.insert(WeatherContract.LocationEntry.TABLE_NAME, null, contentValues);
                if(_id > 0){
                    result = WeatherContract.LocationEntry.buildLocationUri(_id);
                } else {
                    throw new android.database.SQLException("Failed to insert row into " + uri);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri::" + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }

    private Cursor weatherByLocation(Uri uri, String[] projection, String sortOrder){
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String startDate = WeatherContract.WeatherEntry.getStartDateFromUri(uri);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String[] selectionArgs;
        String selection;

        selectionArgs = startDate == null ? new String[]{locationSetting} : new String[]{locationSetting, startDate};
        selection = startDate == null ? Queries.LOCATION_BY_SETTING : Queries.LOCATION_BY_SETTING_AND_STARTDATE;

        qb.setTables(Tables.WEATHER_JOIN_LOCATION);
        return qb.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }

    private Cursor weatherByLocationAndDate(Uri uri, String[] projection, String sortOrder){
        String locationSetting = WeatherContract.WeatherEntry.getLocationSettingFromUri(uri);
        String date = WeatherContract.WeatherEntry.getDateFromUri(uri);
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        String[] selectionArgs = new String[]{locationSetting, date};
        String selection = Queries.LOCATION_BY_SETTING_AND_DATE;

        qb.setTables(Tables.WEATHER_JOIN_LOCATION);
        return qb.query(mOpenHelper.getReadableDatabase(),
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder);
    }
}
