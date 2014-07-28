package com.nn.studio.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class ForecastFragment extends Fragment {
    private final String TAG = this.getClass().getName();
    private ArrayAdapter<String> mAdapter;
    private List<WeatherModel> mAdapterBackingData;
    private final int METRIC_UNITS = 0;
    private final int IMPERIAL_UNITS = 1;

    public ForecastFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals(getString(R.string.pref_temp_unit_key))){

                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView weatherView = (ListView) rootView.findViewById(R.id.listview_forecast);

        if(mAdapter == null){
            mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview, new ArrayList<String>());
        }
        weatherView.setAdapter(mAdapter);

        weatherView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String forecast = mAdapter.getItem(position);
                Intent detailedActivityLaunch = new Intent(getActivity(), DetailActivity.class);
                detailedActivityLaunch.putExtra("FORECAST", forecast);
                startActivity(detailedActivityLaunch);
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_refresh:
                updateWeather();
                return true;
            case R.id.action_view_map:
                showLocationOnMap();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showLocationOnMap() {
        Intent mapIntent =  new Intent(Intent.ACTION_VIEW);
        String location = userLocation();
        Uri geoLocation = Uri.parse("geo:0,0?q=" + location);
        if(mapIntent.resolveActivity(getActivity().getPackageManager()) != null){
            mapIntent.setData(geoLocation);
            startActivity(mapIntent);
        }
    }

    private String userLocation(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String LOCATION_KEY = getResources().getString(R.string.pref_location_key);
        String defaultLocation = getResources().getString(R.string.pref_location_default);
        String location = prefs.getString(LOCATION_KEY, defaultLocation);

        return location;
    }

    private int temperatureUnit(){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String TEMP_UNIT_KEY = getString(R.string.pref_temp_unit_key);
        String defaultUnit = getResources().getString(R.string.pref_temp_unit_default);
        String currentUnit = prefs.getString(TEMP_UNIT_KEY, defaultUnit);

        Log.v(TAG, "CurrentUNIT::" + currentUnit);
        return Integer.parseInt(currentUnit);
    }

    private void setWeatherList(){
        if(mAdapter != null && mAdapterBackingData != null){
            mAdapter.clear();
            int tempUnit = temperatureUnit();
            Boolean isImperialSystem = tempUnit == IMPERIAL_UNITS;
            for (WeatherModel model: mAdapterBackingData){
                mAdapter.add(model.toString(isImperialSystem));
            }
        }
    }

    private void updateWeather(){
        new FetchWeatherTask().execute(userLocation());
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, ArrayList<WeatherModel>> {
        private final String TAG = this.getClass().getName();
        @Override
        protected ArrayList<WeatherModel> doInBackground(String... postCodes) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            ArrayList<WeatherModel> forecasts = new ArrayList<WeatherModel>();
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                String authority = "api.openweathermap.org";
                String baseUrl = "data/2.5/forecast/daily";
                Uri.Builder builder = new Uri.Builder();
                builder.scheme("http").authority(authority).
                        appendPath(baseUrl).
                        appendQueryParameter("q", postCodes[0]).
                        appendQueryParameter("mode", "json").
                        appendQueryParameter("units", "metric").
                        appendQueryParameter("cnt", "7");
                URL url = new URL(builder.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                forecasts = WeatherDataParser.getWeatherDataFromJson(forecastJsonStr);
            } catch (JSONException ex){
                ex.printStackTrace();
                return null;
            }

            return forecasts;
        }

        @Override
        protected void onPostExecute(ArrayList<WeatherModel> weatherModels) {
            if(weatherModels != null){
                mAdapterBackingData = weatherModels;
                setWeatherList();
            }
            //mAdapter.notifyDataSetChanged();
        }
    }
}
