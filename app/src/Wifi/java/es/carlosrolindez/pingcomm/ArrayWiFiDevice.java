package es.carlosrolindez.pingcomm;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import es.carlosrolindez.wificomm.WiFiDevice;


public class ArrayWiFiDevice extends ArrayList<WiFiDevice> implements Parcelable{
    public static String TAG = "ArrayWiFiDevice";
	
	public void addSorted(WiFiDevice newDevice) {
		if (isEmpty()) {
			add(newDevice);
			return;
		}
		int position=0;
		for (WiFiDevice device : this) {
			if (newDevice.deviceName.compareToIgnoreCase(device.deviceName)<=0) {
				add(position, newDevice);
				return;
			}
			position++;
		}
		add(newDevice);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int arg1)
	{

		parcel.writeInt(this.size());
		for (WiFiDevice device:this) {
			parcel.writeParcelable(device, arg1);
		}

	}

	public static final Parcelable.Creator<ArrayWiFiDevice> CREATOR = new Parcelable.Creator<ArrayWiFiDevice>() {

		@Override
		public ArrayWiFiDevice createFromParcel(Parcel parcel)
		{
            ArrayWiFiDevice list = new ArrayWiFiDevice();

			int size = parcel.readInt();
			for (int i=0; i<size ; ++i) {
				WiFiDevice device = parcel.readParcelable(getClass().getClassLoader());
                list.addSorted(device);
			}
			return list;
		}

		@Override
		public ArrayWiFiDevice[] newArray(int size)
		{
			return new ArrayWiFiDevice[size];
		}

	};

}
