package com.nn.studio.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
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
    private ArrayAdapter<String> mAdapter;

    public ForecastFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_refresh:
                new FetchWeatherTask().execute("94043");
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
        private final String TAG = this.getClass().getName();
        @Override
        protected String[] doInBackground(String... postCodes) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String[] forecasts = null;
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
                        appendQueryParameter("unit", "metric").
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
                forecasts = WeatherDataParser.getWeatherDataFromJson(forecastJsonStr, 7);
            } catch (JSONException ex){
                ex.printStackTrace();
                return null;
            }

            return forecasts;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            for (String item : strings){
                mAdapter.add(item);
            }
            //mAdapter.notifyDataSetChanged();
        }
    }
}
