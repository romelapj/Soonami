package com.romelapj.soonami

import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView

import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset
import java.text.SimpleDateFormat

/**
 * Displays information about a single earthquake.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Kick off an {@link AsyncTask} to perform the network request
        val task = TsunamiAsyncTask()
        task.execute()
    }

    /**
     * Update the screen to display information from the given [Event].
     */
    private fun updateUi(earthquake: Event) {
        // Display the earthquake title in the UI
        val titleTextView = findViewById<View>(R.id.title) as TextView
        titleTextView.setText(earthquake.title)

        // Display the earthquake date in the UI
        val dateTextView = findViewById(R.id.date) as TextView
        dateTextView.text = getDateString(earthquake.time)

        // Display whether or not there was a tsunami alert in the UI
        val tsunamiTextView = findViewById(R.id.tsunami_alert) as TextView
        tsunamiTextView.text = getTsunamiAlertString(earthquake.tsunamiAlert)
    }

    /**
     * Returns a formatted date and time string for when the earthquake happened.
     */
    private fun getDateString(timeInMilliseconds: Long): String {
        val formatter = SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm:ss z")
        return formatter.format(timeInMilliseconds)
    }

    /**
     * Return the display string for whether or not there was a tsunami alert for an earthquake.
     */
    private fun getTsunamiAlertString(tsunamiAlert: Int): String {
        when (tsunamiAlert) {
            0 -> return getString(R.string.alert_no)
            1 -> return getString(R.string.alert_yes)
            else -> return getString(R.string.alert_not_available)
        }
    }

    /**
     * [AsyncTask] to perform the network request on a background thread, and then
     * update the UI with the first earthquake in the response.
     */
    private inner class TsunamiAsyncTask : AsyncTask<URL, Void, Event>() {

        override fun doInBackground(vararg urls: URL): Event? {
            // Create URL object
            val url = createUrl(USGS_REQUEST_URL)

            // Perform HTTP request to the URL and receive a JSON response back
            var jsonResponse = ""
            try {
                jsonResponse = makeHttpRequest(url!!)
            } catch (e: IOException) {
                // TODO Handle the IOException
            }

            // Extract relevant fields from the JSON response and create an {@link Event} object

            // Return the {@link Event} object as the result fo the {@link TsunamiAsyncTask}
            return extractFeatureFromJson(jsonResponse)
        }

        /**
         * Update the screen with the given earthquake (which was the result of the
         * [TsunamiAsyncTask]).
         */
        override fun onPostExecute(earthquake: Event?) {
            if (earthquake == null) {
                return
            }

            updateUi(earthquake!!)
        }

        /**
         * Returns new URL object from the given string URL.
         */
        private fun createUrl(stringUrl: String): URL? {
            var url: URL? = null
            try {
                url = URL(stringUrl)
            } catch (exception: MalformedURLException) {
                Log.e(LOG_TAG, "Error with creating URL", exception)
                return null
            }

            return url
        }

        /**
         * Make an HTTP request to the given URL and return a String as the response.
         */
        @Throws(IOException::class)
        private fun makeHttpRequest(url: URL): String {
            var jsonResponse = ""
            var urlConnection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            try {
                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.requestMethod = "GET"
                urlConnection.readTimeout = 10000
                urlConnection.connectTimeout = 15000
                urlConnection.connect()
                inputStream = urlConnection.inputStream
                jsonResponse = readFromStream(inputStream)
            } catch (e: IOException) {
                // TODO: Handle the exception
            } finally {
                urlConnection?.disconnect()
                inputStream?.close()
            }
            return jsonResponse
        }

        /**
         * Convert the [InputStream] into a String which contains the
         * whole JSON response from the server.
         */
        @Throws(IOException::class)
        private fun readFromStream(inputStream: InputStream?): String {
            val output = StringBuilder()
            if (inputStream != null) {
                val inputStreamReader = InputStreamReader(inputStream, Charset.forName("UTF-8"))
                val reader = BufferedReader(inputStreamReader)
                var line: String? = reader.readLine()
                while (line != null) {
                    output.append(line)
                    line = reader.readLine()
                }
            }
            return output.toString()
        }

        /**
         * Return an [Event] object by parsing out information
         * about the first earthquake from the input earthquakeJSON string.
         */
        private fun extractFeatureFromJson(earthquakeJSON: String): Event? {
            try {
                val baseJsonResponse = JSONObject(earthquakeJSON)
                val featureArray = baseJsonResponse.getJSONArray("features")

                // If there are results in the features array
                if (featureArray.length() > 0) {
                    // Extract out the first feature (which is an earthquake)
                    val firstFeature = featureArray.getJSONObject(0)
                    val properties = firstFeature.getJSONObject("properties")

                    // Extract out the title, time, and tsunami values
                    val title = properties.getString("title")
                    val time = properties.getLong("time")
                    val tsunamiAlert = properties.getInt("tsunami")

                    // Create a new {@link Event} object
                    return Event(title, time, tsunamiAlert)
                }
            } catch (e: JSONException) {
                Log.e(LOG_TAG, "Problem parsing the earthquake JSON results", e)
            }

            return null
        }
    }

    companion object {

        /** Tag for the log messages  */
        val LOG_TAG = MainActivity::class.java.simpleName

        /** URL to query the USGS dataset for earthquake information  */
        private val USGS_REQUEST_URL = "https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=2012-01-01&endtime=2012-12-01&minmagnitude=6"
    }
}