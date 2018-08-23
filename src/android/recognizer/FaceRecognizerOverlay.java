//////////////////////////////////////////////////////////////////////////////
//
//  Copyright (c) since 1999. VASCO DATA SECURITY
//  All rights reserved. http://www.vasco.com
//
//////////////////////////////////////////////////////////////////////////////

package com.vasco.faceid.recognizer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.RoundRectShape;
import android.view.Display;
import android.view.View;
import android.content.res.Resources;
import android.view.WindowManager;

import com.keylemon.oasiscs.face.OasisCSFaceClient;
import java.util.HashMap;

public class FaceRecognizerOverlay extends View
{

    private final static long refreshRate = 750; // refresh in ms.

    private ShapeDrawable scanningZoneDrawable;

    private ShapeDrawable backgroundZoneDrawable;

    private int scanningZoneLeft;

    private int scanningZoneTop;

    private int scanningZoneWidth;

    private int scanningZoneHeight;

    private Paint actionMessagePaint;

    private Paint challengeMessagePaint;

    private String actionMessage;

    private String challengeMessage;

    private OasisCSFaceClient.QualityControlIssues positionQualityControlIssues;

    private OasisCSFaceClient.QualityControlIssues lightingQualityControlIssues;

    private OasisCSFaceClient.QualityControlIssues shakyQualityControlIssues;

    private long lastUpdate;

    private long lastPositionUpdate;

    private long lastLightingUpdate;

    private long lastShakyUpdate;

    private int callbackIconMargin;

    private int callbackIconSize;

    private int allIconsWidth;

    private int callbackIconsMarginLeft;

    /**
     * Creates the decorations that will be put on top of the camera preview.
     *
     * @param context Android context.
     * @param isEnrollment True if we are in enrollment mode and not recognition mode.
     */
    public FaceRecognizerOverlay(Context context, boolean isEnrollment)
    {
        super(context);


        // Draw translucent rounded rect in center of the view.
        scanningZoneDrawable = new ShapeDrawable(
                new RoundRectShape(new float[] { 35, 35, 35, 35, 35, 35, 35, 35 }, null, null));
        scanningZoneDrawable.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
        scanningZoneDrawable.getPaint().setStrokeWidth(8);
        scanningZoneDrawable.getPaint().setAntiAlias(true);
        scanningZoneDrawable.getPaint().setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point p = new Point();
        display.getSize(p);

        int screenWidth = p.x;
        int screenHeight = p.y;

        scanningZoneWidth = screenWidth * 75 / 100;
        scanningZoneHeight = screenHeight * 56 / 100;

        scanningZoneLeft = (screenWidth - scanningZoneWidth) / 2;
        scanningZoneTop = (screenHeight - scanningZoneHeight) / 2;

        Rect rect = new Rect(scanningZoneLeft, scanningZoneTop, scanningZoneWidth + scanningZoneLeft,
                scanningZoneHeight + scanningZoneTop);
        scanningZoneDrawable.setBounds(rect);

        // Draw the grey background on the all screen.
        backgroundZoneDrawable = new ShapeDrawable(new RectShape());
        backgroundZoneDrawable.getPaint().setColor(Color.parseColor("#99000000"));
        backgroundZoneDrawable.setBounds(0, 0, screenWidth, screenHeight);

        // Initialize action message paint (better here than in onDraw)
        actionMessagePaint = new Paint();
        actionMessagePaint.setColor(Color.WHITE);
        actionMessagePaint.setTypeface(Typeface.DEFAULT_BOLD);
        actionMessagePaint.setTextSize(screenWidth * 6 / 100);
        actionMessagePaint.setTextAlign(Paint.Align.CENTER);

        actionMessage = isEnrollment ? "Enrolamiento" : "Autenticación";

        // Initialize action message paint (better here than in onDraw)
        challengeMessagePaint = new Paint();
        challengeMessagePaint.setColor(Color.WHITE);
        challengeMessagePaint.setTypeface(Typeface.DEFAULT_BOLD);
        challengeMessagePaint.setTextSize(screenWidth * 10 / 100);
        challengeMessagePaint.setTextAlign(Paint.Align.CENTER);

        challengeMessage = "";

        // Calculate callback icons size and margin based on screen size.
        callbackIconMargin = scanningZoneTop * 5 / 100;
        callbackIconSize = (scanningZoneTop * 40 / 100) - callbackIconMargin;
        allIconsWidth = 3 * (callbackIconMargin + callbackIconSize);
        callbackIconsMarginLeft = (scanningZoneWidth - allIconsWidth) / 2;

        // Last update of callback icons.
        lastUpdate = lastLightingUpdate = lastPositionUpdate = lastShakyUpdate = System.currentTimeMillis();
    }

    /**
     * Manages the drawables and text messages that will be printed in the canvas
     *
     * @param canvas Canvas to print drawables in.
     */
    @Override
    protected void onDraw(Canvas canvas)
    {
        // Draw the zones.
        backgroundZoneDrawable.draw(canvas);
        scanningZoneDrawable.draw(canvas);

        Resources activityRes = getResources();
        int drawableID = activityRes.getIdentifier("face_ok", "drawable", this.getContext().getPackageName());
        Bitmap positionBitmap;
        if (positionQualityControlIssues != null)
        {
            drawableID = activityRes.getIdentifier("face_not_detected_unknown", "drawable", this.getContext().getPackageName());

            if (positionQualityControlIssues.faceNotDetected)
            {
                drawableID = activityRes.getIdentifier("face_not_detected", "drawable", this.getContext().getPackageName());
            }
            else if (positionQualityControlIssues.faceTooClose)
            {
                drawableID = activityRes.getIdentifier("face_too_close", "drawable", this.getContext().getPackageName());
            }
            else if (positionQualityControlIssues.faceTooFar)
            {
                drawableID = activityRes.getIdentifier("face_too_far", "drawable", this.getContext().getPackageName());
            }
            else if (positionQualityControlIssues.faceNotCenteredBottom)
            {
                drawableID = activityRes.getIdentifier("position_not_centered_bottom", "drawable", this.getContext().getPackageName());
            }
            else if (positionQualityControlIssues.faceNotCenteredLeft)
            {
                drawableID = activityRes.getIdentifier("position_not_centered_left", "drawable", this.getContext().getPackageName());
            }
            else if (positionQualityControlIssues.faceNotCenteredRight)
            {
                drawableID = activityRes.getIdentifier("position_not_centered_right", "drawable", this.getContext().getPackageName());
            }
            else if (positionQualityControlIssues.faceNotCenteredTop)
            {
                drawableID = activityRes.getIdentifier("position_not_centered_top", "drawable", this.getContext().getPackageName());
            }
            else if (positionQualityControlIssues.facePitchAngleTooHigh)
            {
                drawableID = activityRes.getIdentifier("position_too_high", "drawable", this.getContext().getPackageName());
            }
            else if (positionQualityControlIssues.facePitchAngleTooLow)
            {
                drawableID = activityRes.getIdentifier("position_too_low", "drawable", this.getContext().getPackageName());
            }
            else if (positionQualityControlIssues.faceYawAngleTooHigh)
            {
                drawableID = activityRes.getIdentifier("position_yaw_too_left", "drawable", this.getContext().getPackageName());
            }
            else if (positionQualityControlIssues.faceYawAngleTooLow)
            {
                drawableID = activityRes.getIdentifier("position_yaw_too_right", "drawable", this.getContext().getPackageName());
            }
        }
        positionBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), drawableID),
                callbackIconSize, callbackIconSize, true);
        canvas.drawBitmap(positionBitmap, scanningZoneLeft + callbackIconsMarginLeft,
                scanningZoneTop - callbackIconSize - callbackIconMargin, null);

        // Lighting Callback
        Bitmap lightingBitmap;
        drawableID = activityRes.getIdentifier("lighting_ok", "drawable", this.getContext().getPackageName());
        if (lightingQualityControlIssues != null)
        {
            drawableID = activityRes.getIdentifier("lighting_not_uniform", "drawable", this.getContext().getPackageName());
            if (lightingQualityControlIssues.lightingNotUniform)
            {
                drawableID = activityRes.getIdentifier("lighting_not_uniform", "drawable", this.getContext().getPackageName());
            }
            else if (lightingQualityControlIssues.tooBright)
            {
                drawableID = activityRes.getIdentifier("lighting_too_bright", "drawable", this.getContext().getPackageName());
            }
            else if (lightingQualityControlIssues.tooDark)
            {
                drawableID = activityRes.getIdentifier("lighting_too_dark", "drawable", this.getContext().getPackageName());
            }
        }
        lightingBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), drawableID),
                callbackIconSize, callbackIconSize, true);
        canvas.drawBitmap(lightingBitmap,
                scanningZoneLeft + callbackIconsMarginLeft + callbackIconSize + callbackIconMargin,
                scanningZoneTop - callbackIconSize - callbackIconMargin, null);

        // Shaky Callback
        Bitmap shakyBitmap;
        drawableID = activityRes.getIdentifier("shaky_ok", "drawable", this.getContext().getPackageName());
        if (shakyQualityControlIssues != null && shakyQualityControlIssues.tooShaky)
        {
            drawableID = activityRes.getIdentifier("too_shaky", "drawable", this.getContext().getPackageName());
        }
        shakyBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), drawableID),
                callbackIconSize, callbackIconSize, true);
        canvas.drawBitmap(shakyBitmap,
                scanningZoneLeft + callbackIconsMarginLeft + 2 * (callbackIconSize + callbackIconMargin),
                scanningZoneTop - callbackIconSize - callbackIconMargin, null);

        // Draw action message
        float actionMessageX = canvas.getWidth() / 2;
        float actionMessageY = scanningZoneTop + scanningZoneHeight + (canvas.getHeight() * 8 / 100);
        canvas.drawText(actionMessage, actionMessageX, actionMessageY, actionMessagePaint);

        // Draw challenge message
        float challengeMessageX = canvas.getWidth() / 2;
        float challengeMessageY = canvas.getHeight() - (canvas.getHeight() * 4 / 100);
        canvas.drawText(challengeMessage, challengeMessageX, challengeMessageY, challengeMessagePaint);
    }

    /**
     * Updates fields depending on the refresh rate.
     *
     * @param qualityIssueType Type of issue.
     */
    public void updateRecognitionCallback(OasisCSFaceClient.QualityControlIssues qualityIssueType)
    {
        long currentUpdateTime = System.currentTimeMillis();
        if (qualityIssueType == null)
        {
            shakyQualityControlIssues = null;
            lightingQualityControlIssues = null;
            positionQualityControlIssues = null;
            lastUpdate = currentUpdateTime;
            this.invalidate();
            return;
        }

        // Update last update based on key.
        if (qualityIssueType.faceNotDetected || qualityIssueType.faceTooClose || qualityIssueType.faceTooFar
                || qualityIssueType.faceNotCenteredTop || qualityIssueType.faceNotCenteredRight
                || qualityIssueType.faceNotCenteredBottom || qualityIssueType.faceNotCenteredLeft
                || qualityIssueType.faceYawAngleTooHigh || qualityIssueType.facePitchAngleTooLow
                || qualityIssueType.faceYawAngleTooHigh || qualityIssueType.faceYawAngleTooLow)
        {
            positionQualityControlIssues = qualityIssueType;
            lastPositionUpdate = currentUpdateTime;
        }
        if (qualityIssueType.tooBright || qualityIssueType.tooDark)
        {
            lightingQualityControlIssues = qualityIssueType;
            lastLightingUpdate = currentUpdateTime;
        }
        if (qualityIssueType.tooShaky)
        {
            shakyQualityControlIssues = qualityIssueType;
            lastShakyUpdate = currentUpdateTime;
        }

        // If there was no update within the last 750ms we can guess this
        // attribute is valid then reset default callback icon.
        if (currentUpdateTime - lastPositionUpdate > refreshRate)
        {
            positionQualityControlIssues = null;
        }
        if (currentUpdateTime - lastLightingUpdate > refreshRate)
        {
            lightingQualityControlIssues = null;
        }
        if (currentUpdateTime - lastShakyUpdate > refreshRate)
        {
            shakyQualityControlIssues = null;
        }

        // Refresh only if the last update was older than 750ms or the refresh is to reset to icon.
        if ((currentUpdateTime - lastUpdate) > refreshRate)
        {
            this.invalidate();
            lastUpdate = currentUpdateTime;
        }
    }

    /**
     * Updates the action message.
     * @param message Message to update.
     */
    public void updateActionMessage(String message)
    {
        updateActionMessage(message, "");
    }

    /**
     * Updates the action message with a challenge.
     * @param actionMessage Message to update.
     * @param challengeMessage Message for the challenge.
     */
    public void updateActionMessage(String actionMessage, String challengeMessage)
    {
        this.actionMessage = actionMessage;
        this.challengeMessage = challengeMessage;
        this.invalidate();
    }
}




