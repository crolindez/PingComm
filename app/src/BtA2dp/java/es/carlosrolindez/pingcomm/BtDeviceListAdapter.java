package es.carlosrolindez.pingcomm;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import es.carlosrolindez.btcomm.BtDevice;


class BtDeviceListAdapter extends BaseAdapter {
	private static String TAG = "BtDeviceListAdapter";

	private final LayoutInflater inflater;
	private final ArrayBtDevice mBtDeviceList;

	
	public BtDeviceListAdapter(Context context, ArrayBtDevice deviceList)
	{
		mBtDeviceList = deviceList;
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public int getCount()
	{
		if (mBtDeviceList == null)
			return 0;
		else
			return mBtDeviceList.size();
	}
	
	@Override
	public Object getItem(int position)
	{
		if (mBtDeviceList == null)
			return 0;
		else			
			return mBtDeviceList.get(position);
	}
	
	@Override
	public long getItemId(int position)
	{
			return position;
	}
	

    @Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
	    if (mBtDeviceList == null)
	    	return null;

		final BtDevice device =  mBtDeviceList.get(position);
	    
		View localView = convertView;
	
		if (localView==null)
		{
			localView = inflater.inflate(R.layout.device_list_row, parent, false);
		}


		TextView deviceName = (TextView)localView.findViewById(R.id.device_name);
		TextView deviceAddress = (TextView)localView.findViewById(R.id.device_address);
		RelativeLayout layout = (RelativeLayout)localView.findViewById(R.id.device_list_layout);

		
		deviceName.setText(device.deviceName);
		deviceAddress.setText(device.getAddress());

		if (device.deviceConnected) {
			layout.setBackgroundResource(R.drawable.connected_selector);
		} else {
            layout.setBackgroundResource(R.drawable.notconnected_selector);
			if (device.deviceBonded) {
                deviceName.setTypeface(Typeface.DEFAULT_BOLD);
                deviceAddress.setTypeface(Typeface.DEFAULT_BOLD);
			} else {
                deviceName.setTypeface(Typeface.DEFAULT);
                deviceAddress.setTypeface(Typeface.DEFAULT);
			}
		}

		return localView;
	}


}
