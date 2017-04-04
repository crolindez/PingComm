package es.carlosrolindez.wificomm;



import android.util.Log;

import java.io.IOException;
import java.net.Socket;

import es.carlosrolindez.rfcomm.RfClientSocket;


public class WiFiClientSocket extends RfClientSocket<Socket,WiFiDevice> {
    private final static String TAG = "WiFiClientSocket";

    public WiFiClientSocket(WiFiCommManager service, WiFiDevice device) {
        super(service, device);
    }


    protected Socket createSocket(WiFiDevice device){
        Socket socket;
        try {
            socket = new Socket(device.mDevice, device.mPort);
        } catch (IOException e) {
            e.printStackTrace();
            socket = null;
        }
        return socket;
    }

    protected void connectSocket() {
        Log.e(TAG, "Connected as client to " + mSocket.getInetAddress().getHostName() + ":" + mSocket.getPort());
        mCommManager.setSocket(mSocket,false);
    }
}
