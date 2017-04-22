package es.carlosrolindez.wificomm;


import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import es.carlosrolindez.rfcomm.RfServerManager;


public class WiFiServerManager extends RfServerManager<Socket,ServerSocket> {
    private static final String TAG = "WiFiServerManager";

    private NsdManager mNsdManager;
    private NsdManager.RegistrationListener mRegistrationListener;


    public WiFiServerManager(Context context, WiFiCommManager commManager) {
        super(context, commManager);
    }

    protected void initializeServerManager() {
        mNsdManager = (NsdManager) mContext.getSystemService(Context.NSD_SERVICE);
    }

    public ServerSocket openServerSocket(){

        ServerSocket server = null;

        // Initialize a server socket on the next available port.
        try {
            server = new ServerSocket(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initializeRegistrationListener();
        if (server!=null)
            registerService(server.getLocalPort());
        return server;

    }

    public Socket serviceAccept(ServerSocket server) {
        Socket socket;
        try {
            socket = server.accept();
/*            if (mChatClient == null) {
                int port = mSocket.getPort();
                InetAddress address = mSocket.getInetAddress();
                connectToServer(address, port);
            }*/

        } catch (IOException e) {
            e.printStackTrace();
            socket = null;
        }
        return socket;
    }

    public void closeServerSocket(ServerSocket server){
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mNsdManager!=null) {
            mNsdManager.unregisterService(mRegistrationListener);
        }
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int arg1) {
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
            }
        };
    }

    public void registerService(int port) {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(WiFiConstants.SERVICE_NAME+WiFiConstants.getHexName());
        serviceInfo.setServiceType(WiFiConstants.SERVICE_TYPE);

        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }
}
