package com.example.monica.b_cycle.services;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class JsonDownloader extends AsyncTask<String, Void, String> {

    private RouteFinder routeFinder;

    public JsonDownloader(RouteFinder routeFinder) {
        this.routeFinder = routeFinder;
    }

    /**
     * Creates URL from the first parameter.
     * Attempts to download data from URL. Returns data as String if successful.
     *
     * @param params
     * @return
     */
    @Override
    protected String doInBackground(String... params) {
        String link = params[0];
        try {
            URL url = new URL(link);
            InputStream is = url.openConnection().getInputStream();
            StringBuffer buffer = new StringBuffer();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            return buffer.toString();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Calls the Json parser method from the listener.
     * @param result
     */
    @Override
    protected void onPostExecute(String result) {
        routeFinder.parseJson(result);
    }
}
