package es.carlosrolindez.wificomm;


import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;

import es.carlosrolindez.rfcomm.RfListenerManager;



public class WiFiListenerManager extends RfListenerManager<WiFiDevice,WiFiListenerManager.WiFiEvent> {
    private static final String TAG = "WiFiListenerManager";

    public enum WiFiEvent { CONNECTED, DISCONNECTED }

    private final NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;


    public WiFiListenerManager(Context context, RfListener<WiFiDevice,WiFiListenerManager.WiFiEvent> listener) {
        super(listener);
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void searchWiFiDevices() {
        initializeDiscoveryListener();
        mNsdManager.discoverServices(WiFiConstants.SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }


    public void closeService() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public void initializeDiscoveryListener() {

        mDiscoveryListener = new NsdManager.DiscoveryListener() {

            @Override
            public void onDiscoveryStarted(String regType) {
            }

            @Override
            public void onServiceFound(final NsdServiceInfo service) {
 //               Log.e(TAG, "Service discovery success " + service);
                if (!service.getServiceType().equals(WiFiConstants.SERVICE_TYPE)) {
//                    Log.e(TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(WiFiConstants.SERVICE_NAME + WiFiConstants.getHexName())) {
//                    Log.e(TAG, "Same machine: " + WiFiConstants.SERVICE_NAME + WiFiConstants.getHexName());
                } else if (service.getServiceName().contains(WiFiConstants.SERVICE_NAME)){
                    mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                        @Override
                        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                        }
                        @Override
                        public void onServiceResolved(NsdServiceInfo serviceInfo) {
                            String name = serviceInfo.getServiceName();
                            WiFiDevice device = new WiFiDevice( name.substring(name.length()-4), serviceInfo.getHost(), serviceInfo.getPort());
                            mRfListener.addRfDevice(name.substring(name.length()-4), device);
                        }
                    });
                }
            }



            @Override
            public void onServiceLost(NsdServiceInfo service) {
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                mNsdManager.stopServiceDiscovery(this);
            }
        };
    }

}


