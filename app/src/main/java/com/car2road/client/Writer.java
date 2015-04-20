package com.car2road.client;

import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Created by qi11110 on 24.03.2015.
 */
public class Writer implements Runnable {

    private String message = null;
    private PrintWriter out;
    private Scanner sc;

    public Writer(PrintWriter out)
    {
        this.out = out;
    }

    public void run()
    {
        sc = new Scanner(System.in);

        while(true)
        {
                System.out.println("Entrez votre message : ");
                message = sc.nextLine();
                out.println(message);
                out.flush();
        }
    }
}
