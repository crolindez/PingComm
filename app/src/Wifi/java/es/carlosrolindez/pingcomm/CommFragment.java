package es.carlosrolindez.pingcomm;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import es.carlosrolindez.wificomm.WiFiCommManager;
import es.carlosrolindez.wificomm.WiFiConstants;


public class CommFragment extends Fragment {
    public final static String TAG = "CommFragment";
    public String commName;

    private WiFiCommManager mWiFiCommManager = null;

    public CommFragment() {
        commName = WiFiConstants.getHexName();
    }

    public WiFiCommManager getWiFiCommManager() {
        return mWiFiCommManager;
    }

    public boolean isSockedConnected()  {
        if (mWiFiCommManager != null)
            return (mWiFiCommManager.isSocketConnected());
        return false;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mWiFiCommManager = new WiFiCommManager(getActivity().getApplicationContext());
    }
}
