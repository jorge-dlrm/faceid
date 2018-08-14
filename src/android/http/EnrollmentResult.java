//////////////////////////////////////////////////////////////////////////////
//
//  Copyright (c) since 1999. VASCO DATA SECURITY
//  All rights reserved. http://www.vasco.com
//
//////////////////////////////////////////////////////////////////////////////
package com.vasco.faceid.http;

import org.json.JSONException;
import org.json.JSONObject;

public class EnrollmentResult extends ServerResult
{

    private String modelId;

    private double sampleQuality;

    public EnrollmentResult(String value, String id) throws JSONException
    {
        JSONObject json = new JSONObject(value);
        if (value.contains("\"error\""))
        {
            error = json.getString("error");
            return;
        }
        this.sampleQuality = json.getDouble("quality");
        modelId = id;
    }

    @Override
    public String toString()
    {
        return "EnrollmentResult{" + "modelId='" + modelId + '\'' + ", sampleQuality=" + sampleQuality + '}';
    }

    public double getQuality() {
        return sampleQuality;
    }
}
