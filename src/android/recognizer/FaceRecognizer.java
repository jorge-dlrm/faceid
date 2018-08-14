//////////////////////////////////////////////////////////////////////////////
//
//  Copyright (c) since 1999. VASCO DATA SECURITY
//  All rights reserved. http://www.vasco.com
//
//////////////////////////////////////////////////////////////////////////////
package com.vasco.faceid.recognizer;

public interface FaceRecognizer
{
    public void addOnFaceRecognitionListener(FaceRecognizer.OnFaceRecognitionListener listener);

    public void addOnFaceEnrollmentListener(FaceRecognizer.OnFaceEnrollmentListener listener);

    public void pauseProcessingFrames();

    public void resumeProcessingFrames();

    public interface ErrorHandler
    {
        public void error(int retCode, String msg, double score);

        public void actionCanceled();
    }

    public interface OnFaceRecognitionListener extends ErrorHandler
    {
        public void faceRecognized(String name, double score, boolean spoofingDetected);
    }

    public interface OnFaceEnrollmentListener extends ErrorHandler
    {
        public void faceEnrolled(String name, double quality);
    }
}
