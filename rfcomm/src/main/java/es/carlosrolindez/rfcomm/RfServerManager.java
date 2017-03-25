package es.carlosrolindez.rfcomm;


public abstract class RfServerManager<TypeRfSocket, TypeRfServerSocket> extends Thread {

    private static final String TAG = "RfServerManager";
    private final RfCommManager<TypeRfSocket> mCommManager;

    private final TypeRfServerSocket mServerSocket;

    protected abstract TypeRfServerSocket openServerSocket();
    protected abstract TypeRfSocket serviceAccept(TypeRfServerSocket server);
    protected abstract void closeServerSocket(TypeRfServerSocket server);

    protected RfServerManager(RfCommManager<TypeRfSocket> commManager) {
        mCommManager = commManager;
        mServerSocket = openServerSocket();
    }

    public void startService() {
        start();
    }

    public void closeService() {
        cancel();
    }



    public void run() {

        if (mServerSocket==null) return;
        TypeRfSocket socket = serviceAccept(mServerSocket);
        // If a connection was accepted
        if (socket != null) {
            mCommManager.setSocket(socket,true);
            closeServerSocket(mServerSocket);

        }
    }

    /** Will stopSocket the listening socket, and cause the thread to finish */
    private void cancel() {
        if (mServerSocket != null) {
            closeServerSocket(mServerSocket);

        }
    }

}
