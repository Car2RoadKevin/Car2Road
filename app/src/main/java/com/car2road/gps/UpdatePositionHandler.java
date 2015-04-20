package com.car2road.gps;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Created by qi11110 on 25.03.2015.
 */
public class UpdatePositionHandler extends Handler {

    private StringBuilder message = new StringBuilder();
    private TextView positionTextView = null;

    public UpdatePositionHandler(TextView positionTextView)
    {
        this.positionTextView = positionTextView;

    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        message.setLength(0);
        message.append((String)msg.obj);
        Log.d("GPS", message.toString());

        positionTextView.setText(message.toString());

    }
}
