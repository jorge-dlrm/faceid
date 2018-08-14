//////////////////////////////////////////////////////////////////////////////
//
//  Copyright (c) since 1999. VASCO DATA SECURITY
//  All rights reserved. http://www.vasco.com
//
//////////////////////////////////////////////////////////////////////////////
package com.vasco.faceid.http;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpConnectionManager
{
    /**
     * JSON identifier for the model ID
     */
    private final static String QUERY_MODEL_NAME = "model_id";

    /**
     * JSON identifier for the model
     */
    private final static String QUERY_MODEL = "model";

    /**
     * Connection manager
     */
    private static HttpConnectionManager instance;

    /**
     * server URL
     */
    private URL serverURL;

    /**
     * Constructor
     * @throws MalformedURLException
     */
    private HttpConnectionManager() throws MalformedURLException
    {
        serverURL = new URL(DefaultConfig.SERVER_URL);
    }

    /**
     * Get the instance of the singleton
     * @return Connection manager
     * @throws MalformedURLException
     */
    public static synchronized HttpConnectionManager getInstance() throws MalformedURLException
    {
        if (instance == null)
        {
            instance = new HttpConnectionManager();
        }
        return instance;
    }

    /**
     * Initializes the http connection with the correct header and content.
     * @param pathToAppend path of the requested service
     * @param data Face data to be sent
     * @param modelID ID the user
     * @return Http connection
     * @throws IOException
     * @throws JSONException
     */
    private HttpURLConnection initializeHttpConnection(String pathToAppend, byte[] data, String modelID)
            throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) (new URL(serverURL, pathToAppend)).openConnection();
        connection.setReadTimeout(30000);
        connection.setConnectTimeout(15000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setUseCaches(false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        JSONObject jsonParam = new JSONObject();
        jsonParam.put(QUERY_MODEL_NAME, modelID);
        jsonParam.put(QUERY_MODEL, Base64.encodeToString(data, Base64.DEFAULT));

        OutputStream os = connection.getOutputStream();
        os.write(jsonParam.toString().getBytes("UTF-8"));
        os.close();
        return connection;
    }

    /**
     * Enrolls a user
     *
     * @param data Data to send to the server
     * @param modelID ID of the user to enroll
     * @return A biometric model with data about enrollment
     * @throws IOException
     * @throws JSONException
     */
    public EnrollmentResult createFaceModel(byte[] data, String modelID) throws IOException, JSONException
    {
        HttpURLConnection connection = initializeHttpConnection(DefaultConfig.ENROLLMENT_PATH, data, modelID);
        String returnValue = readConnectionResponse(connection);
        return new EnrollmentResult(returnValue, modelID);
    }

    /**
     * Recognizes a user
     *
     * @param data Data to send to the server
     * @param modelID ID of the user to recognize
     * @return A verification result with data about recognition
     * @throws IOException
     * @throws JSONException
     */
    public RecognitionResult verifyFaceModel(byte[] data, String modelID) throws IOException, JSONException
    {
        HttpURLConnection connection = initializeHttpConnection(DefaultConfig.VERIFICATION_PATH, data, modelID);
        String returnValue = readConnectionResponse(connection);
        return new RecognitionResult(returnValue);
    }

    /**
     * Returns the content of the http response.
     * @param connection http response
     * @return Body of the response
     * @throws IOException
     */
    private String readConnectionResponse(HttpURLConnection connection) throws IOException
    {
        InputStream is = null;
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        int responseCode = connection.getResponseCode();
        try
        {
            // If responseCode is between 400 & 599, the server return an error.
            is = new BufferedInputStream((responseCode >= 400 && responseCode < 600) ? connection.getErrorStream()
                    : connection.getInputStream());
            br = new BufferedReader(new InputStreamReader(is));
            String line;

            while ((line = br.readLine()) != null)
            {
                sb.append(line);
            }
        }
        finally
        {
            connection.disconnect();
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (Exception e)
                {
                    // ignore
                }
            }

            if (br != null)
            {
                try
                {
                    br.close();
                }
                catch (Exception e)
                {
                    // ignore
                }
            }
        }
        return sb.toString();
    }

}
