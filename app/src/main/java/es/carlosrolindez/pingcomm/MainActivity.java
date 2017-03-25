package es.carlosrolindez.pingcomm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
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
import android.widget.Toast;

import es.carlosrolindez.btcomm.BtDevice;
import es.carlosrolindez.btcomm.BtListenerManager;
import es.carlosrolindez.btsppcomm.BtSppClientSocket;
import es.carlosrolindez.btsppcomm.BtSppServerManager;
import es.carlosrolindez.rfcomm.RfCommManager;


public class MainActivity extends AppCompatActivity implements BtListenerManager.RfListener<BluetoothDevice,
                                                                BtListenerManager.BtEvent> {
    private static final String TAG = "MainActivity";
    private static final String DEVICE_LIST = "device_list";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ENABLE_DSC = 2;

    private enum ActivityState {NOT_SCANNING, SCANNING, CONNECTED}
    private final ActivityState activityState = ActivityState.NOT_SCANNING;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BtListenerManager mBtListenerManager = null;

    private BtSppServerManager mBtSppServerService = null;

    private CommFragment mCommFragment;

    private LocalBroadcastManager mLocalBroadcastManager;
    private BroadcastReceiver mReceiver;

    private ArrayBtDevice deviceList = null;
    private BtDeviceListAdapter deviceListAdapter = null;
    private ListView mListView = null;

    private MenuItem scanButton;
    private MenuItem mActionProgressItem;

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


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, getString(R.string.bt_not_available), Toast.LENGTH_LONG).show();
            finish();
        }

        if (savedInstanceState != null) {
            deviceList = savedInstanceState.getParcelable(DEVICE_LIST);
            if (deviceList==null) deviceList = new ArrayBtDevice();

        } else {
            deviceList = new ArrayBtDevice();
        }



        setProgressBar(ActivityState.NOT_SCANNING);

        fab = (FloatingActionButton) findViewById(R.id.pingButton);
        fab.setVisibility(View.INVISIBLE);
        mp = MediaPlayer.create(this, R.raw.ping);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                mp.start();
                if (mCommFragment.isSockedConnected()) {
                    mCommFragment.getSppCommManager().write("Ping");
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
                    endBtSppServerThread();
                    if (mCommFragment.isSockedConnected()) {
                        showConnected();
                    }
                } else if (intent.getAction().equals(RfCommManager.MESSAGE)) {
                    String readMessage = intent.getStringExtra(RfCommManager.message_content);
                    if (readMessage.equals("Ping")) mp.start();
                } else if (intent.getAction().equals(RfCommManager.STOPPED)) {
                    if (mCommFragment.getSppCommManager().isSocketConnected())
                        mCommFragment.getSppCommManager().closeSocket();
                    fab.setVisibility(View.INVISIBLE);
                    startBtSppServerThread();
                } else if (intent.getAction().equals(RfCommManager.CLOSED)) {
                    startBtSppServerThread();
                }
            }
        };

        mLocalBroadcastManager.registerReceiver(mReceiver, filter);

    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode != Activity.RESULT_OK) {
                    Toast.makeText(this, R.string.bt_not_enabled,Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;

            case REQUEST_ENABLE_DSC:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, R.string.bt_not_visible,Toast.LENGTH_SHORT).show();
                } else {
                    setProgressBar(ActivityState.SCANNING);
                }
                break;

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        else {
            startBtListeningManager();

            if (mCommFragment.isSockedConnected()) {
                showConnected();
            } else {
                startBtSppServerThread();
            }
        }
    }

    private void showConnected() {
        BluetoothDevice connectedDevice = mCommFragment.getSppCommManager().getConnectedDevice();
        for (BtDevice listDevice : deviceList)
            if (listDevice.mDevice.getAddress().equals(connectedDevice.getAddress())) {
                listDevice.deviceBonded= true;
                listDevice.deviceConnected = true;
                deviceListAdapter.notifyDataSetChanged();
                fab.setVisibility(View.VISIBLE);
                break;
            }
    }

    @Override
    protected void onPause() {
        super.onPause();
        endBtListenerManager();
        if (!mCommFragment.isSockedConnected()) {
            endBtSppServerThread();
        }

        for (BtDevice listDevice : deviceList)
            listDevice.deviceConnected = false;

        if (mBluetoothAdapter!=null) mBluetoothAdapter.cancelDiscovery();

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(DEVICE_LIST,deviceList);
    }


    private void startBtSppServerThread() {
        if (mBtSppServerService !=null) {
            return;
        }
        mBtSppServerService = new BtSppServerManager(mCommFragment.getSppCommManager());
        mBtSppServerService.startService();
    }


    private void endBtSppServerThread() {
        if (mBtSppServerService !=null) {
            mBtSppServerService.closeService();
            mBtSppServerService =  null;
        }
    }

    private void endBtListenerManager() {
        if (mBtListenerManager != null) {
            mBtListenerManager.closeService();
            mBtListenerManager = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Store instance of the menu item containing progress
        mActionProgressItem = menu.findItem(R.id.mActionProgress);
        scanButton = menu.findItem(R.id.bt_scan);
        // Extract the action-view from the menu item
  //      ProgressBar v =  (ProgressBar) MenuItemCompat.getActionView(mActionProgressItem);
        // Return to finish
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.bt_scan) {
            if (activityState==ActivityState.NOT_SCANNING) {

                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
                startActivityForResult(discoverableIntent, REQUEST_ENABLE_DSC);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void setProgressBar(ActivityState state) {

        switch (state) {
            case NOT_SCANNING:
            case CONNECTED:
                mBluetoothAdapter.cancelDiscovery();
                if (mActionProgressItem!=null)  mActionProgressItem.setVisible(false);
                if (scanButton!=null)           scanButton.setVisible(true);
                break;
            case SCANNING:
                mBluetoothAdapter.startDiscovery();
                if (mActionProgressItem!=null)  mActionProgressItem.setVisible(true);
                if (scanButton!=null)           scanButton.setVisible(false);
                break;
  /*          case CONNECTED:
                Log.e(TAG,"Connected");
                mBluetoothAdapter.cancelDiscovery();
                if (mActionProgressItem!=null)  mActionProgressItem.setVisible(false);
                if (scanButton!=null)           scanButton.setVisible(false);
                break;*/

        }
    }

    private void startBtListeningManager() {
        mBtListenerManager = new BtListenerManager(getApplication(),this);

        mListView = (ListView)findViewById(R.id.list);
        deviceListAdapter = new BtDeviceListAdapter(this, deviceList);
        mListView.setAdapter(deviceListAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainActivity.this.setProgressBar(ActivityState.CONNECTED);
                BtDevice device = (BtDevice)parent.getItemAtPosition(position);
                mCommFragment.getSppCommManager().stopSocket();

                if (device.mDevice.getBondState() != BluetoothDevice.BOND_BONDED)
                    device.mDevice.createBond();
                else if (!device.deviceConnected){
                    new BtSppClientSocket(mCommFragment.getSppCommManager(),device.mDevice).start();
                }
            }
        });

        mBtListenerManager.searchBtDevices();

    }

    public void addRfDevice(String name, BluetoothDevice device) {
        BtDevice newDevice = new BtDevice(name,device);

        for (BtDevice listDevice : deviceList)
            if (listDevice.getAddress().equals(newDevice.getAddress())) return;

        newDevice.deviceBonded=(newDevice.mDevice.getBondState()==BluetoothDevice.BOND_BONDED);
        deviceList.addSorted(newDevice);
        deviceListAdapter.notifyDataSetChanged();
    }

    public void notifyRfEvent(BluetoothDevice device,  BtListenerManager.BtEvent event) {
        switch (event) {
            case DISCOVERY_FINISHED:
                setProgressBar(ActivityState.NOT_SCANNING);
                break;

            case CONNECTED:

                for (BtDevice listDevice : deviceList)
                {
                    if (device.getAddress().equals(listDevice.getAddress())) {
                        listDevice.deviceBonded = true;
                        listDevice.deviceConnected = true;
                        break;
                    }
                }
                deviceListAdapter.notifyDataSetChanged();
                break;

            case DISCONNECTED:
                for (BtDevice listDevice : deviceList) {
                    if (device.getAddress().equals(listDevice.getAddress())) {
                        listDevice.deviceConnected = false;
                        break;
                    }
                }
                deviceListAdapter.notifyDataSetChanged();
                break;

            case BONDED:
                for (BtDevice listDevice : deviceList)
                {
                    if (device.getAddress().equals(listDevice.getAddress())) {
                        listDevice.deviceBonded = true;
                        break;
                    }
                }
                deviceListAdapter.notifyDataSetChanged();
                new BtSppClientSocket(mCommFragment.getSppCommManager(),device).start();
                break;

        }
    }
}
