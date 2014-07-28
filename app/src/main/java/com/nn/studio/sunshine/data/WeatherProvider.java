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
        Cursor retCursor;
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            // "weather/*/*"
            case WEATHER_WITH_LOCATION_AND_DATE:
            {
                retCursor = null;
                break;
            }
            // "weather/*"
            case WEATHER_WITH_LOCATION: {
                retCursor = null;
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
                throw new UnsupportedOperationException("Unkown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        return 0;
    }
}
