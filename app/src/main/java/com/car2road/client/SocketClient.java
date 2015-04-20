package com.car2road.client;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Observer;

/**
 * Classe client gérant la connexion entre l'application et un serveur par l'intermédiaire de socket.
 * La classe implémente Runnable et doit donc uniquement être utilisé dans un thread.
 * Lors de l'appelle à la fonction run, le SocketClient tente de se connecter au serveur.
 * Si la connexion réussit, alors elle crée un second thread qui instantie une @SocketReader pour écouter les données entrants.
 * Cette SocketReader extends Observable et envoie donc les messages reçus aux Observer
 *
 */
public class SocketClient implements Runnable{

    /**
     * Instance de @Socket gérant la connexion avec le serveur.
     */
    private Socket socket=null;

    /**
     * Port du serveur
     */
    private int port;

    /**
     * Ip du serveur
     */
    private String serverIp;

    /**
     * Contexte de l'activité où est initialisé le SocketClient.
     */
    private Context context;

    /**
     * Nombre d'essaies maximum de connexion au serveur.
     */
    private int maxConnectionTry = 2;

    /**
     * Nombre d'essaies actuel de connexion au serveur.
     */
    private int connectionTry;

    /**
     * Liste des observers qui observeront le SocketReader pour recevoir les données lu par la socket.
     */
    private Collection<Observer> observerList;

    /**
     * Constructeur du socketClient.
     *
     * @param port Le port du serveur sur lequel se connecter
     * @param serverIp L'ip du serveur sur lequel se connecter
     * @param observerList Liste d'observer recevant les données lues par la SocketReader
     * @param context Le Context de l'activité créant cette SocketClient
     */
    public SocketClient(int port, String serverIp, Collection<Observer> observerList, Context context)
    {
        connectionTry=1;
        this.context = context;
        this.port = port;
        this.serverIp = serverIp;
        this.observerList = observerList;
    }

    public void run()
    {
        try
        {
            InetAddress serverAddress = InetAddress.getByName(serverIp);

            if(connect(serverAddress, port))
            {
                try {
                    new Thread(new SocketReader(new BufferedReader(new InputStreamReader(socket.getInputStream())), observerList)).start();
                }
                catch(IOException e)
                {
                    Log.e("SocketClient", "Erreur dans la création du thread de lecture", e);
                }
            }

        }
        catch (UnknownHostException e) {
            Log.e("SocketClient","Impossible de se connecter à l'adresse "+socket.getLocalAddress(), e);
        }
    }

    private void showToast(final String message)
    {
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("SocketClient", message);
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean connect(InetAddress serverAddress,int port)
    {
        try
        {
            showToast("Demande de connexion");
            socket = new Socket(serverAddress, port);
            showToast("Connexion établie avec le serveur");
            return true;
        } catch(ConnectException e) {
            Log.e("SocketClient", "Erreur de connexion", e);
            showToast("Impossible de se connecter : timeout de la tentative de connexion");

            if(connectionTry<maxConnectionTry)
            {
                connectionTry++;
                showToast("Nouvelle tentative de connexion dans 2s");
                try {
                    Thread.sleep(2000);
                }catch(InterruptedException e2)
                {
                    Log.e("SocketClient", "Erreur dans le thread d'attente", e);
                }
                return connect(serverAddress, port);
            }
            else
            {
                showToast("Le maximum d'essaies à la connexion a été atteint");
                return false;
            }


        } catch(IOException e) {
            Log.e("SocketClient", "Erreur de connection", e);
            return false;
        }
    }


}
