//////////////////////////////////////////////////////////////////////////////
//
//  Copyright (c) since 1999. VASCO DATA SECURITY
//  All rights reserved. http://www.vasco.com
//
//////////////////////////////////////////////////////////////////////////////

package com.vasco.faceid.recognizer;

import static android.R.drawable.progress_horizontal;

import com.keylemon.oasiscs.face.OasisCSFaceClient;
import com.keylemon.oasiscs.face.camera.CameraPreview;
import com.vasco.faceid.http.EnrollmentResult;
import com.vasco.faceid.http.HttpConnectionManager;
import com.vasco.faceid.http.ServerResult;
import com.vasco.faceid.http.RecognitionResult;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class FaceRecognizerView extends RelativeLayout
        implements FaceRecognizer, OasisCSFaceClient.OasisCSFaceClientListener,
        OasisCSFaceClient.EyeBlinkChallengeListener, OasisCSFaceClient.HeadMovementChallengeListener
{
    /** Tag to log info with */
    private final static String LOG_TAG = FaceRecognizerView.class.getSimpleName();

    /** True is the user chose enrollment, false if he chose recognition */
    protected boolean mEnrollment;

    /** Name provided by the user */
    protected String userName;

    /** Listener that will be called for enrollment callbacks */
    protected FaceRecognizer.OnFaceEnrollmentListener enrollmentListener;

    /** Listener that will be called for recognition callbacks */
    protected FaceRecognizer.OnFaceRecognitionListener recognitionListener;

    /** Face recognition library */
    private OasisCSFaceClient oasisFace;

    /** Camera preview that is displayed to the user */
    private CameraPreview mCameraPreview;

    /** Progress bar displayed on top of the camera preview */
    private ProgressBar mProgressBar;

    /** Asynchronous task that sends http requests to the face recognition server */
    private AsyncTask<byte[], Void, ServerResult> mNetworkTask = null;

    /** Dialog that prints messages about the progress */
    private ProgressDialog progressDialog;

    /** Recognition callback that will be used to sow which parameter to improve for optimal recognition */
    private FaceRecognizerOverlay recognitionCallbackView;

    /** Anti spoofing mode that was chosen by the user */
    private OasisCSFaceClient.SpoofingDetectionMode antiSpoofingMode = OasisCSFaceClient.SpoofingDetectionMode.NONE;

    /**
     * Constructor that initializes the UI and set fields with data that the user passed as input.
     *
     * @param context
     * @param isEnrollment
     * @param userName
     * @param isBlinkEyes
     * @param isMoveHead
     */
    public FaceRecognizerView(Context context, boolean isEnrollment, String userName, boolean isBlinkEyes,
            boolean isMoveHead)
    {
        super(context);
        this.mEnrollment = isEnrollment;
        this.userName = userName;
        initFaceRecognitionUI();
        if (isBlinkEyes && isMoveHead)
        {
            antiSpoofingMode = OasisCSFaceClient.SpoofingDetectionMode.EYE_BLINK_AND_HEAD_MOVEMENT;
        }
        else if (isBlinkEyes)
        {
            antiSpoofingMode = OasisCSFaceClient.SpoofingDetectionMode.EYE_BLINK;
        }
        else if (isMoveHead)
        {
            antiSpoofingMode = OasisCSFaceClient.SpoofingDetectionMode.HEAD_MOVEMENT;
        }
    }

    /**
     * Constructor that initializes the UI
     *
     * @param context
     * @param attrs
     */
    public FaceRecognizerView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        initFaceRecognitionUI();
    }

    /**
     * Sets the face recognition listener that will be updated at each step of the recognition
     * @param listener
     */
    public void addOnFaceRecognitionListener(FaceRecognizer.OnFaceRecognitionListener listener)
    {
        this.recognitionListener = listener;
    }

    /**
     * Sets the face enrollment listener that will be updated at each step of the recognition
     * @param listener
     */
    public void addOnFaceEnrollmentListener(FaceRecognizer.OnFaceEnrollmentListener listener)
    {
        this.enrollmentListener = listener;
    }

    /**
     * Notifies the active listener of the error passed in parameters.
     * @param code
     * @param msg
     * @param enrollment
     * @param score
     */
    protected void notifyListenerError(int code, String msg, boolean enrollment, double score)
    {
        pauseProcessingFrames();
        if (enrollment)
        {
            if (enrollmentListener != null)
            {
                enrollmentListener.error(code, msg, score);
            }
            else
            {
                Log.e(LOG_TAG, "No enrollment listener for error found...");
            }
        }
        else
        {
            if (recognitionListener != null)
            {
                recognitionListener.error(code, msg, score);
            }
            else
            {
                Log.e(LOG_TAG, "No recognition listener for error found...");
            }
        }
    }

    /**
     * Notifies the face recognition listener that the verification finished with the score passed in parameter
     * @param score
     */
    protected void notifyListenerRecognized(double score, boolean spoofingDetected)
    {
        pauseProcessingFrames();
        if (recognitionListener != null)
        {
            recognitionListener.faceRecognized(this.userName, score, spoofingDetected);
        }
        else
        {
            Log.e(LOG_TAG, "No recognition listener found...");
        }
    }

    /**
     * Notifies the face enrollment listener that the enrollment finished with the quality passed in parameter
     * @param quality
     */
    protected void notifyListenerEnrolled(double quality)
    {
        pauseProcessingFrames();
        if (enrollmentListener != null)
        {
            enrollmentListener.faceEnrolled(this.userName, quality);
        }
        else
        {
            Log.e(LOG_TAG, "No enrollment listener found...");
        }
    }

    /**
     * Initializes the UI and the face recognition library.
     */
    protected void initFaceRecognitionUI()
    {
        oasisFace = new OasisCSFaceClient(getContext(), this);
        oasisFace.setEyeBlinkChallengeListener(this);
        oasisFace.setHeadMovementChallengeListener(this);

        mCameraPreview = new CameraPreview(getContext());
        addView(mCameraPreview, LayoutParams.MATCH_PARENT);
        recognitionCallbackView = new FaceRecognizerOverlay(getContext(), mEnrollment);
        addView(recognitionCallbackView);

        // Set progressbar style.
        RelativeLayout.LayoutParams progressBarParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        progressBarParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);

        mProgressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        mProgressBar.setLayoutParams(progressBarParams);
        mProgressBar.setMinimumHeight(10);
        mProgressBar.setProgress(0);
        mProgressBar.setIndeterminate(false);

        // Get Layer Drawable from default progress bar theme.
        LayerDrawable progressBarDrawable = (LayerDrawable) getResources().getDrawable(progress_horizontal);

        // Get 'background' item from this drawable and customize it.
        GradientDrawable backgroundDrawable = (GradientDrawable) progressBarDrawable
                .findDrawableByLayerId(android.R.id.background);
        backgroundDrawable.setCornerRadius(0);
        backgroundDrawable.setColor(Color.parseColor("#005394"));
        progressBarDrawable.setDrawableByLayerId(android.R.id.background, backgroundDrawable);

        // Create a new clipDrawable (used for progress bar)
        // and add it a gradient drawable to draw the progress.
        GradientDrawable customGradientDrawable = new GradientDrawable();
        customGradientDrawable.setColor(Color.parseColor("#1ADBD0"));
        customGradientDrawable.setCornerRadius(0);
        ClipDrawable customClipDrawable = new ClipDrawable(customGradientDrawable, Gravity.START,
                ClipDrawable.HORIZONTAL);
        customClipDrawable.setLevel(mProgressBar.getProgress());

        // Set the progress bar with our customized item.
        progressBarDrawable.setDrawableByLayerId(android.R.id.progress, customClipDrawable);
        mProgressBar.setProgressDrawable(progressBarDrawable);

        addView(mProgressBar);
    }

    /**
     * Pauses the recognition process
     */
    @Override
    public void pauseProcessingFrames()
    {
        oasisFace.stopCamera();
        // Try to stop the current network task because the application goes in background.
        // Be aware that if the server is receiving enrollment datas, the face model will be
        // created on the server part. Closing the network task only ensure the datas are
        // not use from client side.
        if (mNetworkTask != null && !mNetworkTask.isCancelled())
        {
            mNetworkTask.cancel(true);
            Log.i(LOG_TAG, "Try to stop enrolling task");
        }

        if (progressDialog != null && progressDialog.isShowing())
        {
            progressDialog.dismiss();
        }
    }

    /**
     * Resumes the recognition process
     */
    @Override
    public void resumeProcessingFrames()
    {
        oasisFace.startCamera(mCameraPreview);
    }

    /**
     * Callback called when the eye blink challenge is started
     */
    @Override
    public void onEyeBlinkChallengeStarted()
    {
        String challengeMessage = "Parpadea !";
        recognitionCallbackView.updateActionMessage("", challengeMessage);
    }

    /**
     * Callback called when the eye blink challenge is stopped
     */
    @Override
    public void onEyeBlinkChallengeStopped()
    {
        recognitionCallbackView.updateActionMessage("");

    }

    /**
     * Callback called when the head movement challenge is started
     */
    @Override
    public void onHeadMovementChallengeStarted(OasisCSFaceClient.HeadMovementChallengeType headMovementChallengeType)
    {
        String actionMessage = "";
        String challengeMessage = "";
        switch (headMovementChallengeType)
        {
            case TURN_LEFT:
                actionMessage = "Gira a la Izquierda";
                break;
            case TURN_RIGHT:
                actionMessage = "Gira a la Derecha";
                break;
        }
        recognitionCallbackView.updateActionMessage(actionMessage, challengeMessage);
    }

    /**
     * Callback called when the head movement challenge is stopped
     */
    @Override
    public void onHeadMovementChallengeStopped()
    {
        recognitionCallbackView.updateActionMessage("");
    }

    /**
     * Callback called when the camera is started
     */
    @Override
    public void onCameraStarted()
    {
        Log.i(LOG_TAG, "Camera started");
        // Start enrollment or verification with anti-spoofing measure (default is none).
        try
        {
            if (mEnrollment)
            {
                oasisFace.startAcquisitionForEnrollment();
            }
            else
            {
                oasisFace.startAcquisitionForVerification(antiSpoofingMode);
            }
        }
        catch (Exception e)
        {
            // ignore, if it fails it's because we don't have access to Camera.
        }
    }

    /**
     * Callback called when the camera is stopped
     */
    @Override
    public void onCameraStopped()
    {
        Log.i(LOG_TAG, "Camera stopped");
    }

    /**
     * Callback called when there is an error with the camera
     */
    @Override
    public void onCameraError(Exception e)
    {
        // ignore.
    }

    /**
     * Callback called when the quality control is started
     */
    @Override
    public void onQualityControlStarted()
    {
        // ignore.
    }

    /**
     * Callback called when the quality control changes. Used to update the quality information displayed
     */
    @Override
    public void onQualityControlFeedback(OasisCSFaceClient.QualityControlIssues issues)
    {
        String message = "";
        // Check image quality issues and set Textview callback.
        if (issues.faceNotDetected)
        {
            message = "Cara no detectada";
        }
        else if (issues.faceTooClose)
        {
            message = "Cara demasiado cerca";
        }
        else if (issues.faceTooFar)
        {
            message = "Cara demasiado lejos";
        }
        else if (issues.faceNotCenteredTop)
        {
            message = "Cara muy arriba";
        }
        else if (issues.faceNotCenteredRight)
        {
            message = "Cara muy a la derecha";
        }
        else if (issues.faceNotCenteredBottom)
        {
            message = "Cara muy Abajo";
        }
        else if (issues.faceNotCenteredLeft)
        {
            message = "Cara muy a la izquierda";
        }
        else if (issues.facePitchAngleTooHigh)
        {
            message = "Ángulo de la cara muy alto";
        }
        else if (issues.facePitchAngleTooLow)
        {
            message = "Ángulo de la cara muy abajo";
        }
        else if (issues.faceYawAngleTooHigh)
        {
            message = "Inclinación de la cara muy alta";
        }
        else if (issues.faceYawAngleTooLow)
        {
            message = "Inclinación de la cara muy baja";
        }
        else if (issues.tooShaky)
        {
            message = "Imagen muy borrosa";
        }
        else if (issues.tooBright)
        {
            message = "Imagen muy brillante";
        }
        else if (issues.tooDark)
        {
            message = "Imagen muy oscura";
        }
        else if (issues.lightingNotUniform)
        {
            message = "La luz no es uniforme";
        }

        recognitionCallbackView.updateRecognitionCallback(issues);

        Log.i("FaceRecognizerView", message);
    }

     /**
     * Callback called when the quality control is finished
     */
     @Override
     public void onQualityControlDone()
    {
        recognitionCallbackView.updateRecognitionCallback(null);
    }

    /**
     * Callback called when the enrollment acquisition has successfully terminated
     * @param bytes
     */
    @Override
    public void onSuccessfulAcquisitionForEnrollment(byte[] bytes) {
        // Scanning data have been successfully acquired,
        // Now, send those data to the server to enroll identity
        Log.i(LOG_TAG, "Successful acquisition...");
        executeAcquisitionAction(bytes, mEnrollment);
    }

    /**
     * Callback called when the face recognition acquisition has successfully terminated
     * @param bytes
     * @param spoofingDetected
     */
    @Override
    public void onSuccessfulAcquisitionForVerification(byte[] bytes, boolean spoofingDetected)
    {
        // Scanning data have been successfully acquired,
        // Now, send those data to the server to verify identity
        Log.i(LOG_TAG, "Successful acquisition...");
        executeAcquisitionAction(bytes, mEnrollment);
    }

    /**
     * Callback called when the enrollment acquisition has failed
     * @param failureReason Reason of the failure
     */
    @Override
    public void onFailedAcquisitionForEnrollment(OasisCSFaceClient.AcquisitionFailureReason failureReason)
    {
        failedAcquisition(failureReason);
    }

    /**
     * Callback called when the face recognition acquisition has failed
     * @param failureReason Reason of the failure
     */
    @Override
    public void onFailedAcquisitionForVerification(OasisCSFaceClient.AcquisitionFailureReason failureReason)
    {
        failedAcquisition(failureReason);
    }

    /**
     * Deals with the failed acquisition
     * @param failureReason
     */
    private void failedAcquisition(OasisCSFaceClient.AcquisitionFailureReason failureReason)
    {
        Log.i(LOG_TAG, "Failed to acquire");
        String errorMessage;
        switch (failureReason)
        {
            case QUALITY_CONTROL_TIMEOUT:
                errorMessage = "Timeout: failed to get a good enough image quality.";
                break;
            case RECORDING_TIMEOUT:
                errorMessage = "Timeout: failed to acquire the biometric samples.";
                break;
            case CANCELED:
                errorMessage = "Acquisition cancelled.";
                break;
            default:
                errorMessage = "Face recognition acquisition failure.";
        }
        notifyListenerError(-2, errorMessage, mEnrollment, -1);
    }

    /**
     * Callback that give the progress as a float between 0 and 1
     * @param v
     */
    @Override
    public void onRecordingProgress(float v)
    {
        mProgressBar.setProgress((int) (v * 100));
    }

    /**
     * Send data acquired to the server for enrollment or recognition.
     * Generate a server result on success and use callback.
     *
     * @param data acquisition data from onSuccessToAcquire.
     * @param isEnrollment boolean stating if we are in enrollment mode or recognition mode.
     */
    private void executeAcquisitionAction(byte[] data, final boolean isEnrollment)
    {
        mNetworkTask = new AsyncTask<byte[], Void, ServerResult>()
        {

            @Override
            protected void onPreExecute()
            {
                // Set progress dialog display during network call.
                progressDialog = new ProgressDialog(getContext());
                progressDialog.setCancelable(false);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.setIndeterminate(true);
                progressDialog.setMessage(isEnrollment ? "Enrolando..." : "Autenticando...");

                progressDialog.show();
            }

            @Override
            protected ServerResult doInBackground(byte[]... bytes)
            {
                ServerResult sResult = null;
                try
                {
                    String modelId = userName;
                    if (isEnrollment)
                    {
                        // If the FaceRecognizerView is in enrollment mode, try to create face model.
                        sResult = HttpConnectionManager.getInstance().createFaceModel(bytes[0], modelId);
                    }
                    else
                    {
                        // Get the last generated face model id and verify the current
                        // scanned face match this face model.
                        // It throw a Network Error if the client can't be contacted.
                        if (!"".equals(modelId))
                        {
                            sResult = HttpConnectionManager.getInstance().verifyFaceModel(bytes[0], modelId);
                        }
                    }
                }
                catch (Exception e)
                {
                    Log.e(LOG_TAG, "Error during process...", e);
                }
                return sResult;
            }

            @Override
            protected void onPostExecute(ServerResult serverResult)
            {
                if (progressDialog.isShowing())
                {
                    progressDialog.dismiss();
                }

                if (serverResult == null)
                {
                    // If server result == null, it is probably because a network error occurs.
                    FaceRecognizerView.this.notifyListenerError(-1, "A network error occurred.", isEnrollment, -2);
                }
                else
                {
                    if (serverResult.getError() != null)
                    {
                        FaceRecognizerView.this.notifyListenerError(-1, serverResult.getError(), isEnrollment, -2);
                    }
                    else if (isEnrollment)
                    {
                        // Notify listener than enrollment was a success and send it the modelId generated and the sample quality.
                        EnrollmentResult eResult = (EnrollmentResult) serverResult;
                        FaceRecognizerView.this.notifyListenerEnrolled(eResult.getQuality());
                    }
                    else
                    {
                        RecognitionResult vResult = (RecognitionResult) serverResult;
                        if (vResult.isAuthenticated())
                        {
                            // Notify the listener than the user with current saved ID has been recognized.
                            // If isAntiSpoofingResult is true then spoofing detected is false.
                            FaceRecognizerView.this.notifyListenerRecognized(vResult.getScore(), !vResult.isAntiSpoofingResult());
                        }
                        else
                        {
                            FaceRecognizerView.this.notifyListenerError(-3, "User not authenticated.", false, vResult.getScore());
                        }
                    }
                }
            }

            @Override
            protected void onCancelled(ServerResult serverResult)
            {
                super.onCancelled(serverResult);
                if (progressDialog.isShowing())
                {
                    progressDialog.dismiss();
                }
                Log.i(LOG_TAG, "enrollment cancelled");
            }
        }.execute(data);
    }

    /**
     * Starts the camera preview
     */
    public void startFaceRecognition()
    {
        oasisFace.startCamera(mCameraPreview);
    }
}
