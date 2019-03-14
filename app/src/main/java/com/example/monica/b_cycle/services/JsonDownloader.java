package com.example.monica.b_cycle.services;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class JsonDownloader extends AsyncTask<String, Void, String> {

    private Finder finder;

    JsonDownloader(Finder finder) {
        this.finder = finder;
    }

    /**
     * Creates URL from the first parameter.
     * Attempts to download data from URL and returns data as String if successful.
     *
     * @param params arguments
     * @return json data as string
     */
    @Override
    protected String doInBackground(String... params) {
        String link = params[0];
        try {
            InputStream is = new URL(link).openConnection().getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder builder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line + "\n");
            }

            return builder.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Calls the Json parser method from the listener.
     * @param jsonData data retrieved from calling the finder API
     */
    @Override
    protected void onPostExecute(String jsonData) {
        finder.parseJson(jsonData);
    }
}
