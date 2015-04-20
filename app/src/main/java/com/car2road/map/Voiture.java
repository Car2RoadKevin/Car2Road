package com.car2road.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import com.car2road.R;
import com.google.android.gms.maps.model.Circle;

/**
 * Created by qi11110 on 02.04.2015.
 */
public class Voiture {

    private Bitmap bitmap;

    private int LONGUEUR_BITMAP_PIXEL = 20;

    /**
     * Mileu sur l'axe x
     */
    private int x;

    /**
     * Milieu sur l'axe y
     */
    private int y;

    float angle;

    Location location;

    Context context;

    private Circle circle = null;

    private Circle accurencyCircle = null;

    public Voiture(Context context)
    {
        this.location = new Location(LocationManager.PASSIVE_PROVIDER);
        this.context = context;
        bitmap = buildBitmap();
        x=0;
        y=0;
        angle = 0;
    }

    public Voiture(Context context, Location location, int x, int y, float angle)
    {
        this.context = context;
        this.location = location;
        bitmap = buildBitmap();
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    private Bitmap buildBitmap()
    {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.point_navigation);

        double scaled = LONGUEUR_BITMAP_PIXEL / (double) bitmap.getWidth();
        Log.d(getClass().getName(), "scaled : " + scaled);

        bitmap = Bitmap.createScaledBitmap(bitmap, (int) (bitmap.getWidth()*scaled), (int) (bitmap.getHeight()*scaled), false);

        return bitmap;
    }

    public void onDraw(Canvas canvas)
    {
        drawVehicle(canvas);
        drawAccurencyCircle(canvas);
    }

    private void drawVehicle(Canvas canvas)
    {
        //Log.i(this.getClass().getName(), "x : " + x + ", y : " + y);

        RectF src = new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight());
        RectF dst = new RectF(x - bitmap.getWidth()/2, y - bitmap.getHeight()/2, x + bitmap.getWidth()/2, y + bitmap.getHeight()/2);

        Matrix matrix = new Matrix();
        if(!matrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER))
        {
            Log.e("Voiture", "Erreur dans le setRectToRect");
        }

        matrix.postRotate(angle, x , y);

        canvas.drawBitmap(bitmap, matrix, null);
    }

    private void drawAccurencyCircle(Canvas canvas)
    {
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);

        float accuracy = location.getAccuracy();
        float accuracyPixel = accuracy * MapEngine.METER_TO_PIXEL;
        if (accuracyPixel > 260)
        {
            accuracyPixel = 260;
        }
        canvas.drawCircle(x, y, accuracyPixel, paint);
    }

    public int getWidth()
    {
        return bitmap.getWidth();
    }

    public int getHeight()
    {
        return bitmap.getHeight();
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public Circle getCircle() {
        return circle;
    }

    public void setCircle(Circle circle) {
        this.circle = circle;
    }

    public Circle getAccurencyCircle() {
        return accurencyCircle;
    }

    public void setAccurencyCircle(Circle accurencyCircle) {
        this.accurencyCircle = accurencyCircle;
    }
}
