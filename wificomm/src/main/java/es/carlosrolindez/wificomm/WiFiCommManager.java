package es.carlosrolindez.wificomm;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import es.carlosrolindez.rfcomm.RfCommManager;


public class WiFiCommManager extends RfCommManager<Socket> {
    private static final String TAG = "WiFiCommManager";

    public WiFiCommManager(Context context) {
        super(context);
    }

    @Override
    protected InputStream getInputStream() {
        InputStream is = null;

        try {
            is = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return is;
    }

    @Override
    protected OutputStream getOutputStream() {
        OutputStream os = null;

        try {
            os = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return os;
    }

    @Override
    public void closeSocket() {
        try {
            Log.e(TAG,"Socket closed");
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InetAddress getConnectedDevice(){
        if (socket!=null) {
            if (socket.isConnected())
                return socket.getInetAddress();
        }

        return null;
    }

    public int getPort() {
        if (socket!=null) {
            if (socket.isConnected())
                return socket.getPort();
        }
        return 0;
    }

}
