/*
 *  FILE create by antoine Roux
 *  <antoinroux@hotmail.fr>
 */

package com.example.root.bluesms;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.os.Handler;

/**
 * Created by root on 09/09/15.
 * class that create a bluetooth server and wait client incoming connection
 */
public class thServer extends Thread {
    private BluetoothServerSocket sockServ;
    private List<thClient> lstClient = new ArrayList<thClient>();
    private Handler handler =null;
    private Context ctx = null;

    /**
     * constructor of server thread class
     * @param adapter bluetooth adaptater for create the server
     * @param uuid uuid of the blueSms server
     * @param _ctx context of the main application
     * handler for communicate between service and client thread ( this handle is pass to each thClient)
     */
    public thServer(BluetoothAdapter adapter, UUID uuid, Handler h, Context _ctx) {
        super();
        handler = h;
        ctx = _ctx;
        Log.println(Log.ASSERT, "thServer", "Lancement d'un thread pour gerer le server");
        try {
            sockServ = adapter.listenUsingRfcommWithServiceRecord("BlueSms", uuid);
        } catch (IOException e) {
            Log.println(Log.ERROR, "thServer constructor", String.valueOf(e));
        }
    }

    /**
     * run method of server thread that wait for client connection and create for each of them a new thClient
     */
    @Override
    public void run() {
        BluetoothSocket sock = null;

        super.run();
        Log.println(Log.ASSERT, "launch server", "boucle sur le accept");
        while(!Thread.interrupted()) {
            try {
                sock = sockServ.accept();
                if (sock != null) {
                    Log.d("CONNECTED", "Connected bluetooth");
                    thClient client = new thClient(sock, handler, ctx);
                    lstClient.add(client);
                    client.start();
                }
            } catch (IOException e) {
                Log.println(Log.ERROR,"thServer run","erreur lors du accept");
            }
        }

        try {
            sockServ.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * method that proper interrupt the BlueSms server by close server socket
     */
    @Override
    public void interrupt() {
        super.interrupt();
        try {
            sockServ.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * method that return the list of all client
     * @return List<thClient>
     */
    public List<thClient> getClients() {
        return lstClient;
    }
}
