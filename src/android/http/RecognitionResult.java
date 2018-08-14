//////////////////////////////////////////////////////////////////////////////
//
//  Copyright (c) since 1999. VASCO DATA SECURITY
//  All rights reserved. http://www.vasco.com
//
//////////////////////////////////////////////////////////////////////////////
package com.vasco.faceid.http;

import org.json.JSONException;
import org.json.JSONObject;

public class RecognitionResult extends ServerResult
{
    private boolean authenticated;

    private double score;

    private String antiSpoofingMode;

    private boolean antiSpoofingResult;

    private double samplesQuality;

    private double samplesQualitySimilarity;

    public RecognitionResult(String value) throws JSONException
    {
        JSONObject json = new JSONObject(value);
        if (value.contains("\"error\""))
        {
            error = json.getString("error");
            return;
        }
        this.authenticated = json.getBoolean("authenticated");
        this.score = json.getDouble("score");
        this.antiSpoofingMode = json.getString("antispoofing_mode");
        this.antiSpoofingResult = json.getBoolean("antispoofing_result");
        this.samplesQuality = json.getDouble("samples_quality");
        this.samplesQualitySimilarity = json.getDouble("samples_quality_similarity");
    }

    public boolean isAuthenticated()
    {
        return authenticated;
    }

    public double getScore()
    {
        return score;
    }

    public boolean isAntiSpoofingResult()
    {
        return antiSpoofingResult;
    }

    @Override
    public String toString()
    {
        return "RecognitionResult{" + "authenticated=" + authenticated + ", score=" + score + ", antiSpoofingMode='"
                + antiSpoofingMode + '\'' + ", antiSpoofingResult=" + antiSpoofingResult + ", samplesQuality="
                + samplesQuality + ", samplesQualitySimilarity=" + samplesQualitySimilarity + '}';
    }
}
