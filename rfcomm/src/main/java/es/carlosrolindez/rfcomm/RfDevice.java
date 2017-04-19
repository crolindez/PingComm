package es.carlosrolindez.rfcomm;


public abstract class RfDevice<TypeRfDevice> {
    private static String TAG = "TypeRfDevice";

    public final String deviceName;

    public final TypeRfDevice mDevice;
    public final int mPort;
    public boolean deviceConnected;

    protected RfDevice(String name, TypeRfDevice device, int port) {
        deviceName = name;
        deviceConnected = false;
        mDevice = device;
        mPort = port;
    }

    public abstract String getAddress();

}
