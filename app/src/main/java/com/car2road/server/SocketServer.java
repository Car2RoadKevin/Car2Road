package com.car2road.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by qi11110 on 24.03.2015.
 */
public class SocketServer implements Runnable {

    private ServerSocket serverSocket = null;
    private Socket socket = null;
    private int port;

    public SocketServer(int port)
    {
        this.port = port;
    }

    public void run()
    {
        try
        {
            serverSocket = new ServerSocket(port);
            System.out.println("Le serveur est prêt !");

            while(true) {
                try {
                    socket = serverSocket.accept();
                    System.out.println("Un client s'est connecté");
                }catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        catch(IOException e){
            e.printStackTrace();
        }


    }

}
