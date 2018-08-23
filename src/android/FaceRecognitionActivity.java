//////////////////////////////////////////////////////////////////////////////
//
//  Copyright (c) since 1999. VASCO DATA SECURITY
//  All rights reserved. http://www.vasco.com
//
//////////////////////////////////////////////////////////////////////////////
package com.vasco.faceid;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.view.View;
import com.vasco.faceid.constants.Constants;
import com.vasco.faceid.recognizer.FaceRecognizer;
import com.vasco.faceid.recognizer.FaceRecognizerView;

public class FaceRecognitionActivity extends Activity
        implements FaceRecognizer.OnFaceEnrollmentListener, FaceRecognizer.OnFaceRecognitionListener
{
    /**
     * Layout displayed by the activity
     */
    private FrameLayout frameLayout;

    /**
     * Request type
     */
    private String requestType;

    /**
     * User name
     */
    private String userName;

    /**
     * Is the blink eye test enabled
     */
    private boolean isBlinkEyes;

    /**
     * Is the move head test enabled
     */
    private boolean isMoveHead;

    /**
     * Face recognizer view
     */
    private FaceRecognizerView mFaceRecognizerView;

    /**
     * Is the face recognition running 
     */
    private boolean running;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestType = getIntent().getStringExtra(Constants.EXTRA_REQUEST_TYPE);
        userName = getIntent().getStringExtra(Constants.EXTRA_USER_NAME);
        isBlinkEyes = getIntent().getBooleanExtra(Constants.EXTRA_BLINK_EYES, false);
        isMoveHead = getIntent().getBooleanExtra(Constants.EXTRA_MOVE_HEAD, false);

        // Adds a layout
        frameLayout = new FrameLayout(this);
        setContentView(frameLayout);

        if (running)
        {
            mFaceRecognizerView.pauseProcessingFrames();
            mFaceRecognizerView.resumeProcessingFrames();
            return;
        }

        running = true;
        createCameraPreview();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        running = false;

        if (mFaceRecognizerView != null)
        {
            mFaceRecognizerView.pauseProcessingFrames();
            mFaceRecognizerView = null;
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (running)
        {
            mFaceRecognizerView.pauseProcessingFrames();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (running)
        {
            mFaceRecognizerView.resumeProcessingFrames();
        } else
        {
            Intent result = new Intent();
            result.putExtra(Constants.EXTRA_REQUEST_TYPE, requestType);
            setResult(RESULT_CANCELED, result);
            finish();
        }
    }

    @Override
    public void onBackPressed()
    {
        this.actionCanceled();
    }

    private void createCameraPreview()
    {
        if (mFaceRecognizerView == null)
        {
            boolean isEnrollment = Constants.ACTION_ENROLLMENT.equalsIgnoreCase(requestType);
            mFaceRecognizerView = new FaceRecognizerView(this, isEnrollment, userName, isBlinkEyes, isMoveHead);
            mFaceRecognizerView.addOnFaceEnrollmentListener(this);
            mFaceRecognizerView.addOnFaceRecognitionListener(this);
            frameLayout.addView(mFaceRecognizerView, new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT));
            mFaceRecognizerView.startFaceRecognition();
        }
        frameLayout.bringToFront();
    }

    @Override
    public void faceEnrolled(String name, double quality)
    {
        Intent result = new Intent();
        result.putExtra(Constants.EXTRA_REQUEST_TYPE, requestType);
        result.putExtra(Constants.EXTRA_NAME, name);
        result.putExtra(Constants.EXTRA_QUALITY, quality);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void faceRecognized(String name, double score, boolean spoofingDetected)
    {
        Intent result = new Intent();
        result.putExtra(Constants.EXTRA_REQUEST_TYPE, requestType);
        result.putExtra(Constants.EXTRA_NAME, name);
        // The score returned by the server, at this point it should be between
        // 0 & 1 as negative value is returned when the user is unauthenticated.
        result.putExtra(Constants.EXTRA_SCORE, score);
        result.putExtra(Constants.EXTRA_SPOOFING_DETECTED, spoofingDetected);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void error(int retCode, String msg, double score)
    {
        Intent result = new Intent();
        result.putExtra(Constants.EXTRA_REQUEST_TYPE, requestType);
        result.putExtra(Constants.EXTRA_RETURN_CODE, retCode);
        result.putExtra(Constants.EXTRA_SCORE, score);
        result.putExtra(Constants.EXTRA_MESSAGE, msg);
        setResult(RESULT_CANCELED, result);
        finish();
    }

    @Override
    public void actionCanceled()
    {
        Intent result = new Intent();
        result.putExtra(Constants.EXTRA_REQUEST_TYPE, requestType);
        setResult(RESULT_CANCELED, result);
        finish();
    }
}