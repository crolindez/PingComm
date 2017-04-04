package es.carlosrolindez.wificomm;


import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

import es.carlosrolindez.rfcomm.RfListenerManager;



public class WiFiListenerManager extends RfListenerManager<WiFiDevice,WiFiListenerManager.WiFiEvent> {
    private static final String TAG = "WiFiListenerManager";

    public enum WiFiEvent { CONNECTED, DISCONNECTED }

    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;


    public WiFiListenerManager(Context context, RfListener<WiFiDevice,WiFiListenerManager.WiFiEvent> listener) {
        super(listener);
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void searchWiFiDevices() {
        initializeDiscoveryListener();
        Log.e(TAG,"searchWiFiDevices");
        mNsdManager.discoverServices(WiFiConstants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }


    public void closeService() {
        Log.e(TAG,"closeService");
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public void initializeDiscoveryListener() {

        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
                Log.e(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(final NsdServiceInfo service) {
 //               Log.e(TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals(WiFiConstants.SERVICE_TYPE)) {
//                    Log.e(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(WiFiConstants.SERVICE_NAME + WiFiConstants.getHexName())) {
//                    Log.e(TAG, "Same machine: " + WiFiConstants.SERVICE_NAME + WiFiConstants.getHexName());
                } else if (service.getServiceName().contains(WiFiConstants.SERVICE_NAME)){
                    Log.e(TAG, "Machine found : " + service.getServiceName());
                    mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                            Log.e(TAG, "Resolve Failed: " + serviceInfo);
                        }
                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            String name = serviceInfo.getServiceName();
                            Log.e(TAG, "Service Resolved: " + name + " " + serviceInfo.getHost().getHostName()+ ":"+serviceInfo.getPort());
                            WiFiDevice device = new WiFiDevice( name.substring(name.length()-4), serviceInfo.getHost(), serviceInfo.getPort());
                            mRfListener.addRfDevice(name.substring(name.length()-4), device);
                        }
                    });
                }
            }



            @Override
            public void onServiceLost(NsdServiceInfo service) {
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.e(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

}


