package es.carlosrolindez.rfcomm;


public abstract class RfClientSocket<TypeRfSocket, TypeRfDevice> extends Thread {

    private static final String TAG = "RfClientSocket";
    protected final RfCommManager<TypeRfSocket> mCommManager;
    protected final TypeRfSocket mSocket;

    abstract protected TypeRfSocket createSocket(TypeRfDevice device);
    abstract protected void connectSocket();


    protected RfClientSocket(RfCommManager<TypeRfSocket> commManager, TypeRfDevice device) {

        this.mCommManager = commManager;
        this.mSocket = createSocket(device);
    }

    @Override
    public void run() {
        connectSocket();
    }


}
