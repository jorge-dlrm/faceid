package com.vasco.faceid;

import android.content.Context;
import android.content.Intent;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;
import android.content.Intent;
import android.view.View;
import com.vasco.faceid.constants.Constants;;

public class FaceID extends CordovaPlugin {


    /**
     * Current action
     */
    private static String currentAction;

    private CallbackContext PUBLIC_CALLBACKS = null;

    private String cedula = "";

    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Context context = cordova.getActivity().getApplicationContext();
        PUBLIC_CALLBACKS = callbackContext;
        if(action.equals("enrolamiento")) {
            JSONObject obj = args.optJSONObject(0);
            if (obj != null) {
              cedula = obj.optString("cedula");
            } else {
              callbackContext.error("Falló el tema del args de la instancia");
              return true;
            }
            System.out.println("Entre 1");
            this.OnEnrollClicked();
        }
        else if(action.equals("verificacion")) {
            JSONObject respuesta = new JSONObject();
            JSONObject obj = args.optJSONObject(0);
            if (obj != null) {
              cedula = obj.optString("cedula");
            } else {
              callbackContext.error("Falló el tema del args de la instancia");
              return true;
            }
            System.out.println("Entre 1");
            this.OnVerifyClicked();
        }

        PluginResult pluginResult = new  PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true); // Keep callback
        return true;
    }

    /**
     * Sets required data in an intent and starts the Face Recognition Activity.
     *
     * @param requestType Enrollment or Recognition
     */
    private void startFaceRecognitionActivity(String requestType)
    {
        // Instantiate intent to start face recognition activity
        Intent intent = new Intent("com.vasco.faceid.FaceRecognitionActivity");

        intent.putExtra(Constants.EXTRA_REQUEST_TYPE, requestType);
        intent.putExtra(Constants.EXTRA_BLINK_EYES, true);
        intent.putExtra(Constants.EXTRA_MOVE_HEAD, true);
        intent.putExtra(Constants.EXTRA_USER_NAME, cedula);
        this.cordova.startActivityForResult((CordovaPlugin) this, intent, 0);
    }

    /**
     * Checks parameters and run face recognition activity
     *
     * @param actionName Enroll or Recognize
     */
    private void checkAndRunActivity(String actionName)
    {
        currentAction = actionName;
        startFaceRecognitionActivity(actionName);
    }

    public void OnEnrollClicked()
    {
        checkAndRunActivity(Constants.ACTION_ENROLLMENT);
    }

    public void OnVerifyClicked()
    {
        checkAndRunActivity(Constants.ACTION_RECOGNITION);
    }

    /**
     * Gets data from the face recognition activity's completion.
     *
     * @param requestCode Code that was given as input.
     * @param resultCode  Result code of the activity.
     * @param data        Intent containing the data we want to print.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        JSONObject respuesta = new JSONObject();
        String requestType = (data == null) ? "" : data.getStringExtra(Constants.EXTRA_REQUEST_TYPE);
        String resultText = "";

        boolean isEnrollment = Constants.ACTION_ENROLLMENT.equals(requestType);

        if(resultCode == cordova.getActivity().RESULT_OK){
                if (isEnrollment)
                {
                    // Enrollment
                    respuesta.put("Status", "Face Enrolled with success");
                    double quality = data.getDoubleExtra(Constants.EXTRA_QUALITY, -1);
                    respuesta.put("Quality", quality);
                    PluginResult resultado = new PluginResult(PluginResult.Status.OK, respuesta);
                    resultado.setKeepCallback(true);
                    PUBLIC_CALLBACKS.sendPluginResult(resultado);
                }
                else
                {
                    double score = data.getDoubleExtra(Constants.EXTRA_SCORE, -1);
                    if (score > 0)
                    {
                        respuesta.put("Status", "Face Enrolled with success");
                        PluginResult resultado = new PluginResult(PluginResult.Status.OK, respuesta);
                        resultado.setKeepCallback(true);
                        PUBLIC_CALLBACKS.sendPluginResult(resultado);
                    }
                    else
                    {
                        respuesta.put("Status", "Face NOT Recognized");
                        PluginResult resultado = new PluginResult(PluginResult.Status.OK, respuesta);
                        resultado.setKeepCallback(true);
                        PUBLIC_CALLBACKS.sendPluginResult(resultado);
                    }
                    respuesta.put("Score", score);
                    boolean spoofingDetected = data.getBooleanExtra(Constants.EXTRA_SPOOFING_DETECTED, false);
                    respuesta.put("Spoofing", spoofingDetected);
                    PluginResult resultado = new PluginResult(PluginResult.Status.OK, respuesta);
                    resultado.setKeepCallback(true);
                    PUBLIC_CALLBACKS.sendPluginResult(resultado);
                }

                String name = data.getStringExtra(Constants.EXTRA_NAME);
                respuesta.put("Name", name);
                PluginResult resultado = new PluginResult(PluginResult.Status.OK, respuesta);
                resultado.setKeepCallback(true);
                PUBLIC_CALLBACKS.sendPluginResult(resultado);
            }
            else if(resultCode == cordova.getActivity().RESULT_CANCELED){
                String message = (data == null) ? "" : data.getStringExtra(Constants.EXTRA_MESSAGE);
                if (message == null || message.isEmpty())
                {
                    // Scanning has been cancelled
                    resultText = isEnrollment ? "Enrollment" : "Recognition";
                    resultText += " cancelled.";
                    respuesta.put("Error", resultText);
                    PluginResult resultado = new PluginResult(PluginResult.Status.OK, respuesta);
                    resultado.setKeepCallback(true);
                    PUBLIC_CALLBACKS.sendPluginResult(resultado);
                }
                else
                {
                    // Error
                    int retCode = data.getIntExtra(Constants.EXTRA_RETURN_CODE, -1);
                    if (retCode == -3)
                    {
                        double score = data.getDoubleExtra(Constants.EXTRA_SCORE, -1);
                        respuesta.put("Error", message);
                        respuesta.put("Score", score);
                        PluginResult resultado = new PluginResult(PluginResult.Status.OK, respuesta);
                        resultado.setKeepCallback(true);
                        PUBLIC_CALLBACKS.sendPluginResult(resultado);
                    }
                    else
                    {
                        resultText = "Error during face " + requestType + ": (" + retCode + ") " + message;
                        respuesta.put("Error", resultText);
                        PluginResult resultado = new PluginResult(PluginResult.Status.OK, respuesta);
                        resultado.setKeepCallback(true);
                        PUBLIC_CALLBACKS.sendPluginResult(resultado);
                    }
                }
        }
        return;
    }
}
