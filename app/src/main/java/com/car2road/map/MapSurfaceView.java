package com.car2road.map;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.location.LocationProvider;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.car2road.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by qi11110 on 02.04.2015.
 */
public class MapSurfaceView extends SurfaceView implements SurfaceHolder.Callback{

    private SurfaceHolder surfaceHolder;

    private DrawingThread drawingThread;

    private SparseArray<Voiture> voitures;

    public MapSurfaceView(Context context)
    {
        super(context);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);

        drawingThread = new DrawingThread();
    }

    @Override
    public void draw(Canvas canvas)
    {
        canvas.drawARGB(255,255,255,255);

        int size = voitures.size();
        for(int i=0;i<size;i++)
        {
            Voiture voiture = voitures.valueAt(i);
            voiture.onDraw(canvas);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        drawingThread.keepDrawing=true;
        if(Thread.State.NEW.equals(drawingThread.getState()) || Thread.State.RUNNABLE.equals(drawingThread.getState()))
        {
            drawingThread.start();
        }
        else if (Thread.State.TERMINATED.equals(drawingThread.getState()))
        {
            drawingThread = new DrawingThread();
            drawingThread.start();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        drawingThread.keepDrawing = false;

        boolean joined = false;
        while (!joined) {
            try {
                drawingThread.join();
                joined = true;
            } catch (InterruptedException e) {}
        }
    }

    private class DrawingThread extends  Thread
    {
        boolean keepDrawing = true;

        @Override
        public void run() {
            while (keepDrawing) {
                Canvas canvas = null;

                try {
                    // On récupère le canvas pour dessiner dessus
                    canvas = surfaceHolder.lockCanvas();
                    // On s'assure qu'aucun autre thread n'accède au holder
                    synchronized (surfaceHolder) {
                        // Et on dessine
                        if(canvas != null) {
                            draw(canvas);
                        }
                    }
                } finally {
                    // Notre dessin fini, on relâche le Canvas pour que le dessin s'affiche
                    if (canvas != null)
                        surfaceHolder.unlockCanvasAndPost(canvas);
                }

                // Pour dessiner à 50 fps
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {}
            }
        }
    }

    public SparseArray<Voiture> getVoitures() {
        return voitures;
    }

    public void setVoitureList(SparseArray<Voiture> voitureList) {
        this.voitures = voitureList;
    }

}
