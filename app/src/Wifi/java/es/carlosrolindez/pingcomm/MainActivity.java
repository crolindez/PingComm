package es.carlosrolindez.pingcomm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.net.InetAddress;

import es.carlosrolindez.rfcomm.RfCommManager;
import es.carlosrolindez.wificomm.WiFiClientSocket;
import es.carlosrolindez.wificomm.WiFiDevice;
import es.carlosrolindez.wificomm.WiFiListenerManager;
import es.carlosrolindez.wificomm.WiFiServerManager;


public class MainActivity extends AppCompatActivity implements WiFiListenerManager.RfListener<WiFiDevice,
                                                                WiFiListenerManager.WiFiEvent> {
    private static final String TAG = "MainActivity";
    private static final String DEVICE_LIST = "device_list";

    private enum ActivityState {SCANNING, CONNECTED}
    private final ActivityState activityState = ActivityState.SCANNING;

    private WiFiListenerManager mWiFiListenerManager = null;

    private WiFiServerManager mWiFiServerService = null;

    private CommFragment mCommFragment;

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mReceiver;

    private ArrayWiFiDevice deviceList = null;
    private WiFiDeviceListAdapter deviceListAdapter = null;
    private ListView mListView = null;

    private MenuItem scanButton;
    private MenuItem mActionProgressItem;
    private MenuItem mName;

    private FloatingActionButton fab;

    private MediaPlayer mp;



    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        FragmentManager fm = getSupportFragmentManager();
        mCommFragment = (CommFragment) fm.findFragmentByTag(CommFragment.TAG);

        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        if (mCommFragment == null) {
            mCommFragment = new CommFragment();
            fm.beginTransaction().add(mCommFragment, CommFragment.TAG).commit();
        }

        if (savedInstanceState != null) {
            deviceList = savedInstanceState.getParcelable(DEVICE_LIST);
            if (deviceList==null) deviceList = new ArrayWiFiDevice();

        } else {
            deviceList = new ArrayWiFiDevice();
        }


        fab = (FloatingActionButton) findViewById(R.id.pingButton);
        fab.setVisibility(View.INVISIBLE);
        mp = MediaPlayer.create(this, R.raw.ping);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCommFragment.isSockedConnected()) {
                    mCommFragment.getWiFiCommManager().write("Ping");
                }
            }
        });


        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        // We are going to watch for interesting local broadcasts.
        IntentFilter filter = new IntentFilter();
        filter.addAction(RfCommManager.STARTED);
        filter.addAction(RfCommManager.MESSAGE);
        filter.addAction(RfCommManager.STOPPED);
        filter.addAction(RfCommManager.CLOSED);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(RfCommManager.STARTED)) {
                    Log.e(TAG,"STARTED");
                    endWiFiServerThread();
                    endWiFiListenerManager();
                    if (mCommFragment.isSockedConnected()) {
                        showConnected();
                    }
                } else if (intent.getAction().equals(RfCommManager.MESSAGE)) {
                    Log.e(TAG,"MESSAGE");
                    String readMessage = intent.getStringExtra(RfCommManager.message_content);
                    if (readMessage.equals("Ping")) mp.start();
                } else if (intent.getAction().equals(RfCommManager.STOPPED)) {
                    Log.e(TAG,"STOPPED");
                    if (mCommFragment.getWiFiCommManager().isSocketConnected())
                        mCommFragment.getWiFiCommManager().closeSocket();
                    showDisconnected();
                } else if (intent.getAction().equals(RfCommManager.CLOSED)) {
                    Log.e(TAG,"CLOSED");
                    showDisconnected();
                }
            }
        };

        mLocalBroadcastManager.registerReceiver(mReceiver, filter);

        mListView = (ListView)findViewById(R.id.list);
        deviceListAdapter = new WiFiDeviceListAdapter(this, deviceList);
        mListView.setAdapter(deviceListAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainActivity.this.setProgressBar(ActivityState.CONNECTED);
                WiFiDevice device = (WiFiDevice)parent.getItemAtPosition(position);
                for (WiFiDevice listDevice : deviceList) {
                    if (listDevice.deviceConnected){}
                    {
                        Log.e(TAG,"clicked to disconnect");
                        listDevice.deviceConnected = false;
                        deviceListAdapter.notifyDataSetChanged();
                        mCommFragment.getWiFiCommManager().stopSocket();
                        break;
                    }
                }

                new WiFiClientSocket(mCommFragment.getWiFiCommManager(),device).start();
            }
        });
    }



    @Override
    protected void onResume() {
        super.onResume();

        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(!wifi.isWifiEnabled())
            wifi.setWifiEnabled(true);

        if (mCommFragment.isSockedConnected()) {
            Log.e(TAG,"OnResume Show Connected");
            showConnected();
        } else {
            Log.e(TAG,"OnResume Show Disconnected");
            showDisconnected();
        }
    }


    private void showDisconnected() {
        setProgressBar(ActivityState.SCANNING);
        fab.setVisibility(View.INVISIBLE);
        for (WiFiDevice listDevice : deviceList)
            listDevice.deviceConnected = false;
        deviceListAdapter.notifyDataSetChanged();
        startWiFiServerThread();
        startWiFiListeningManager();
    }

    private void showConnected() {
        setProgressBar(ActivityState.CONNECTED);
        InetAddress connectedDevice = mCommFragment.getWiFiCommManager().getConnectedDevice();
        for (WiFiDevice listDevice : deviceList)
            if (listDevice.mDevice.getHostAddress().equals(connectedDevice.getHostAddress())) {
                listDevice.deviceConnected = true;
                listDevice.mPort = mCommFragment.getWiFiCommManager().getPort();
                deviceListAdapter.notifyDataSetChanged();
                fab.setVisibility(View.VISIBLE);
                break;
            }
    }

    @Override
    protected void onPause() {
        super.onPause();
        endWiFiListenerManager();
        if (!mCommFragment.isSockedConnected()) {
            endWiFiServerThread();
        }

        for (WiFiDevice listDevice : deviceList)
            listDevice.deviceConnected = false;

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(DEVICE_LIST,deviceList);
    }


    private void startWiFiServerThread() {
        if (mWiFiServerService !=null) {
            return;
        }
        mWiFiServerService = new WiFiServerManager(getApplicationContext(), mCommFragment.getWiFiCommManager());
        mWiFiServerService.startService();
    }


    private void endWiFiServerThread() {
        if (mWiFiServerService !=null) {
            mWiFiServerService.closeService();
            mWiFiServerService =  null;
        }
    }

    private void endWiFiListenerManager() {
        if (mWiFiListenerManager != null) {
            mWiFiListenerManager.closeService();
            mWiFiListenerManager = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mActionProgressItem = menu.findItem(R.id.mActionProgress);
        scanButton = menu.findItem(R.id.mScan);
        mName = menu.findItem(R.id.mName);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Store instance of the menu item containing progress
        Log.e(TAG,"OnPrepareOptionsMenu");


        if (mCommFragment.isSockedConnected()) {
            setProgressBar(ActivityState.CONNECTED);
        } else {
            setProgressBar(ActivityState.SCANNING);
        }

        mName.setTitle(mCommFragment.commName);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.mScan) {
            if (mCommFragment.isSockedConnected()) {
                Log.e(TAG,"Search a new device");
                mCommFragment.getWiFiCommManager().stopSocket();
                showDisconnected();
            }
        }

        return super.onOptionsItemSelected(item);
    }


    private void setProgressBar(ActivityState state) {

        switch (state) {
            case CONNECTED:
                Log.e(TAG,"CONNECTED");
                if (mActionProgressItem!=null)  mActionProgressItem.setVisible(false);
                if (scanButton!=null)           scanButton.setVisible(true);
                break;

            case SCANNING:
                Log.e(TAG,"SCANNING");
                if (mActionProgressItem!=null)  mActionProgressItem.setVisible(true);
                if (scanButton!=null)           scanButton.setVisible(false);
                break;
        }
    }

    private void startWiFiListeningManager() {
        mWiFiListenerManager = new WiFiListenerManager(getApplication(),this);

        mWiFiListenerManager.searchWiFiDevices();

    }

    public void addRfDevice(String name, WiFiDevice device) {

        for (WiFiDevice listDevice : deviceList)
            if (listDevice.mDevice.getHostName().equals(device.mDevice.getHostName())) {
                listDevice.deviceName = device.deviceName;
                listDevice.mPort=device.mPort;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        deviceListAdapter.notifyDataSetChanged();
                    }
                });
                return;
            }

        deviceList.addSorted(device);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceListAdapter.notifyDataSetChanged();
            }
        });

    }

    public void notifyRfEvent(WiFiDevice device,  WiFiListenerManager.WiFiEvent event) {
    }
}
