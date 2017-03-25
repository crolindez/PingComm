package es.carlosrolindez.rfcomm;


public abstract class RfDevice<TypeRfDevice> {
    private static String TAG = "TypeRfDevice";

    public final String deviceName;

    public final TypeRfDevice mDevice;
    public boolean deviceConnected;

    protected RfDevice(String name, TypeRfDevice device) {
        deviceName = name;
        deviceConnected = false;
        mDevice = device;
    }

    public abstract String getAddress();

}
