package com.car2road.client;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by qi11110 on 24.03.2015.
 */
public class SocketReader extends Observable implements Runnable {

    private BufferedReader in;

    public SocketReader(BufferedReader in, Collection<Observer> observerList)
    {
        this.in = in;
        for(Observer observer : observerList)
        {
            addObserver(observer);
        }

    }

    public void run()
    {
        try {
            while (true) {
                try {
                    char[] bufferHeader = new char[4];
                    if (in.read(bufferHeader, 0, 4) != -1) {

                        String buffer = new String(bufferHeader);
                        buffer = buffer.replace(" ", "");
                        Log.i("SocketReader", buffer);

                        int sizeMessage = Integer.parseInt(buffer, 16);
                        char[] bufferChar = new char[sizeMessage];

                        int sizeChar = in.read(bufferChar, 0, sizeMessage);
                        if (sizeChar != -1) {
                            buffer = new String(bufferChar).substring(0, sizeChar);
                            setChanged();
                            notifyObservers(buffer);
                        }
                    }

                } catch (NumberFormatException e) {
                    Log.d("ReadHeader", "Impossible de convertir le header en int", e);
                }
            }
        }
        catch(IOException e)
        {
            Log.e("SocketReader","Erreur dans la lecture du message",e);
        }
    }
}
