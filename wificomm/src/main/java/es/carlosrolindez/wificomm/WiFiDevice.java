package es.carlosrolindez.wificomm;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetAddress;
import java.net.UnknownHostException;

import es.carlosrolindez.rfcomm.RfDevice;


public class WiFiDevice extends RfDevice<InetAddress> implements Parcelable{
    private final static String TAG = "WiFiDevice";


    public WiFiDevice(String name, InetAddress device, int port) {
        super(name,device,port);
    }

    @Override
    public String getAddress() {
        return deviceName;
    }
    
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int arg1)
    {
        parcel.writeInt(this.mDevice.getAddress().length);
        parcel.writeByteArray(this.mDevice.getAddress());
        parcel.writeInt(this.mPort);
        parcel.writeString(this.deviceName);
    }

    public static final Creator<WiFiDevice> CREATOR = new Creator<WiFiDevice>() {

        @Override
        public WiFiDevice createFromParcel(Parcel parcel)
        {
            InetAddress inetAddress = null;
            byte[] ipAddress = new byte[parcel.readInt()];
            parcel.readByteArray(ipAddress);
            try {
                inetAddress = InetAddress.getByAddress(ipAddress);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            String name = parcel.readString();
            int port = parcel.readInt();
            return new WiFiDevice(name,inetAddress,port);
        }

        @Override
        public WiFiDevice[] newArray(int size)
        {
            return new WiFiDevice[size];
        }

    };
}
