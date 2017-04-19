package es.carlosrolindez.btcomm.bta2dpcomm;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.List;


public class BtA2dpConnectionManager {
    private static final String TAG = "BtA2dpConnectionManager";

    public enum BtA2dpEvent {CONNECTED, DISCONNECTED}

    private BluetoothA2dp a2dpProxy;
    private BluetoothHeadset headsetProxy;

    private final BtA2dpProxyListener mBtA2dProxyListener;

    private Context mContext;


    public interface BtA2dpProxyListener {
        void notifyBtA2dpEvent(BluetoothDevice device, BtA2dpEvent event);
    }

    public BtA2dpConnectionManager(Context context, BtA2dpProxyListener listener) {
        mBtA2dProxyListener = listener;
        mContext = context;
    }

    public void openManager() {
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(mContext, mA2dpProfileListener, BluetoothProfile.A2DP);
        BluetoothAdapter.getDefaultAdapter().getProfileProxy(mContext, mHeadsetProfileListener, BluetoothProfile.HEADSET);
        mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED));
        mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED));
        mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED));
        mContext.registerReceiver(mReceiver, new IntentFilter(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED));
    }

    public void closeManager() {

        if (a2dpProxy!=null) {
            BluetoothAdapter.getDefaultAdapter().closeProfileProxy(BluetoothProfile.A2DP, a2dpProxy);
        }
        a2dpProxy = null;
        mContext.unregisterReceiver(mReceiver);
    }

    public void connectBluetoothA2dp(BluetoothDevice device) {

        if (a2dpProxy != null) {
            try {
                a2dpProxy.getClass()
                        .getMethod("connect", BluetoothDevice.class)
                        .invoke(a2dpProxy, device);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnectBluetoothA2dp() {

        if (a2dpProxy != null) {
            List<BluetoothDevice> a2dpConnectedDevices = a2dpProxy.getConnectedDevices();
            if (a2dpConnectedDevices.size() != 0) {
                for (BluetoothDevice device : a2dpConnectedDevices) {
                    try {
                        a2dpProxy.getClass()
                                .getMethod("disconnect", BluetoothDevice.class)
                                .invoke(a2dpProxy, device);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }



    private final BluetoothProfile.ServiceListener mA2dpProfileListener = new BluetoothProfile.ServiceListener() {

        public void onServiceConnected(int profile, BluetoothProfile proxy) {

            if (profile == BluetoothProfile.A2DP) {
                Log.e(TAG,"Proxy A2dp Connected");
                a2dpProxy = (BluetoothA2dp) proxy;

                //Just to see if some A2dp is still connected
                if (mBtA2dProxyListener != null) {
                    List<BluetoothDevice> a2dpConnectedDevices = a2dpProxy.getConnectedDevices();
                    if (a2dpConnectedDevices.size() != 0) {
                        for (BluetoothDevice device : a2dpConnectedDevices) {
                            mBtA2dProxyListener.notifyBtA2dpEvent(device, BtA2dpEvent.CONNECTED);
                        }
                    }
                }
            }
        }

        public void onServiceDisconnected(int profile) {

            if (profile == BluetoothProfile.A2DP) {
                Log.e(TAG,"Proxy A2dp Disconnected");
                if (mBtA2dProxyListener != null)
                    mBtA2dProxyListener.notifyBtA2dpEvent(null,BtA2dpEvent.DISCONNECTED);
            }
        }
    };

    private final BluetoothProfile.ServiceListener mHeadsetProfileListener = new BluetoothProfile.ServiceListener() {

        public void onServiceConnected(int profile, BluetoothProfile proxy) {

            if (profile == BluetoothProfile.HEADSET) {
                Log.e(TAG,"Proxy Headset Connected");
                headsetProxy = (BluetoothHeadset) proxy;

            }
        }

        public void onServiceDisconnected(int profile) {

            if (profile == BluetoothProfile.HEADSET) {
                Log.e(TAG,"Proxy Headset Disconnected");
            }
        }
    };


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context ctx, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device;
            Log.e(TAG, "receive intent for action : " + action);
            if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED);
                Log.e(TAG, "CONNECTION_STATE_CHANGED : " + state);

                if (state == BluetoothA2dp.STATE_CONNECTED) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mBtA2dProxyListener.notifyBtA2dpEvent(device, BtA2dpEvent.CONNECTED);
                    // Just in case the device tries to connect also the Headset profile
                    disconnectBluetoothHeadset(device);
                } else if (state == BluetoothA2dp.STATE_DISCONNECTED) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mBtA2dProxyListener.notifyBtA2dpEvent(device, BtA2dpEvent.DISCONNECTED);

                }
            } else if (action.equals(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_NOT_PLAYING);
                Log.e(TAG, "PLAYING_STATE_CHANGED : " + state);
 /*               if (state == BluetoothA2dp.STATE_PLAYING) {
                    Log.d(TAG, "A2DP start playing");
                    Toast.makeText(A2DPActivity.this, "A2dp is playing", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "A2DP stop playing");
                    Toast.makeText(A2DPActivity.this, "A2dp is stopped", Toast.LENGTH_SHORT).show();
                }*/
            } else if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_NOT_PLAYING);
                Log.e(TAG, "HEADSET CONNECTION_STATE_CHANGED : " + state);
                if (headsetProxy!=null) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    Log.e(TAG, "HEADSET trying to stop");
                    disconnectBluetoothHeadset(device);
                    mBtA2dProxyListener.notifyBtA2dpEvent(device, BtA2dpEvent.CONNECTED);
                }
            }
        }

    };

    public void connectBluetoothHeadset(BluetoothDevice device) {
        if (headsetProxy != null) {
            try {
                headsetProxy.getClass()
                        .getMethod("connect", BluetoothDevice.class)
                        .invoke(headsetProxy, device);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnectBluetoothHeadset(BluetoothDevice device) {
        if (headsetProxy != null) {

            try {
                headsetProxy.getClass()
                        .getMethod("disconnect", BluetoothDevice.class)
                        .invoke(headsetProxy, device);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}

