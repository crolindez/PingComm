package es.carlosrolindez.pingcomm;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
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
import es.carlosrolindez.btcomm.bta2dpcomm.BtA2dpConnectionManager;


public class MainActivity extends AppCompatActivity implements BtListenerManager.RfListener<BluetoothDevice,BtListenerManager.BtEvent>,
                                                                BtA2dpConnectionManager.BtA2dpProxyListener {
    private static final String TAG = "MainActivity";
    private static final String DEVICE_LIST = "device_list";
    private static final int REQUEST_ENABLE_BT = 1;

    private enum ActivityState {NOT_SCANNING, SCANNING, CONNECTED}
    private final ActivityState activityState = ActivityState.NOT_SCANNING;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BtListenerManager mBtListenerManager = null;
    private BtA2dpConnectionManager mBtA2dpConnectionManager = null;

    private ArrayBtDevice deviceList;
    private BtDeviceListAdapter deviceListAdapter = null;
    private ListView mListView = null;

    private MenuItem scanButton;
    private MenuItem mActionProgressItem;

    private MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

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

        mBtA2dpConnectionManager = new BtA2dpConnectionManager(getApplication(),this);

 //       progressBar = (ProgressBar) findViewById(R.id.progress_spinner);

  /*      scanButton = (Button) this.findViewById(R.id.bt_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activityState==ActivityState.NOT_SCANNING) {
                    setProgressBar(ActivityState.SCANNING);
                }
            }
        });*/

        setProgressBar(ActivityState.NOT_SCANNING);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.pingButton);
        mp = MediaPlayer.create(this, R.raw.ping);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mp.start();
            }
        });


    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    startRfListening();
                } else {
                    Toast.makeText(this, R.string.bt_not_enabled,Toast.LENGTH_SHORT).show();
                    finish();
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
        else
            startRfListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if  (mBtListenerManager!=null) mBtListenerManager.closeService();

        for (BtDevice listDevice : deviceList)
            listDevice.deviceConnected = false;
        if (mBluetoothAdapter!=null) mBluetoothAdapter.cancelDiscovery();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if  (mBtA2dpConnectionManager!=null) mBtA2dpConnectionManager.closeManager();

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(DEVICE_LIST,deviceList);
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
            if (activityState==ActivityState.NOT_SCANNING)
                setProgressBar(ActivityState.SCANNING);
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

    private void startRfListening() {
        mBtListenerManager = new BtListenerManager(getApplication(),this);
        mBtA2dpConnectionManager.openManager();

        mListView = (ListView)findViewById(R.id.list);
        deviceListAdapter = new BtDeviceListAdapter(this, deviceList, mBtA2dpConnectionManager );
        mListView.setAdapter(deviceListAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainActivity.this.setProgressBar(ActivityState.CONNECTED);
                BtDevice device = (BtDevice)parent.getItemAtPosition(position);
                if (mBtA2dpConnectionManager!=null)
                    mBtA2dpConnectionManager.toggleBluetoothA2dp(device.mDevice);
            }
        });

        mBtListenerManager.knownBtDevices();
        mBtListenerManager.searchBtDevices();

    }

    public void addRfDevice(String name, BluetoothDevice device) {
        BtDevice newDevice = new BtDevice(name,device);

        for (BtDevice listDevice : deviceList)
            if (listDevice.getAddress().equals(newDevice.getAddress())) {
                deviceListAdapter.notifyDataSetChanged();
                listDevice.deviceName = name;
                return;
            }

        deviceList.addSorted(newDevice);
        deviceListAdapter.notifyDataSetChanged();
    }

    public void notifyRfEvent(BluetoothDevice device,  BtListenerManager.BtEvent event) {
        switch (event) {
            case DISCOVERY_FINISHED:
                setProgressBar(ActivityState.NOT_SCANNING);
                break;

/*            case CONNECTED:*/
            case DISCONNECTED:

                for (BtDevice listDevice : deviceList)
                {
                    if (device.getAddress().equals(listDevice.getAddress())) {
                        listDevice.setDeviceInProcess(false);
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
                        deviceListAdapter.notifyDataSetChanged();
                        if (mBtA2dpConnectionManager!=null) {
                            mBtA2dpConnectionManager.connectBluetoothA2dp(listDevice.mDevice);
                        }
                        else {
                            listDevice.setDeviceInProcess(false);
                        }
                        break;
                    }
                }
                deviceListAdapter.notifyDataSetChanged();
                break;

            case CHANGING:
                for (BtDevice listDevice : deviceList)
                {
                    if (device.getAddress().equals(listDevice.getAddress())) {
                        listDevice.deviceConnected = false;
                        listDevice.setDeviceInProcess(true);
                        break;
                    }
                }
                deviceListAdapter.notifyDataSetChanged();
                break;

        }
    }

    public void notifyBtA2dpEvent(BluetoothDevice device,  BtA2dpConnectionManager.BtA2dpEvent event) {


        switch (event) {
            case CONNECTED:
                for (BtDevice listDevice : deviceList)
                {
                    if (device.getAddress().equals(listDevice.getAddress())) {
                        listDevice.deviceConnected = true;
                        listDevice.setDeviceInProcess(false);
                        break;
                    }
                }
                deviceListAdapter.notifyDataSetChanged();
                break;

            case DISCONNECTED:
                for (BtDevice listDevice : deviceList)
                {
                    if (device.getAddress().equals(listDevice.getAddress())) {
                        listDevice.deviceConnected = false;
                        listDevice.setDeviceInProcess(false);
                        break;
                    }
                }
                deviceListAdapter.notifyDataSetChanged();
                break;

            case CHANGING:
                for (BtDevice listDevice : deviceList)
                {
                    if (device.getAddress().equals(listDevice.getAddress())) {
                        listDevice.deviceConnected = false;
                        listDevice.setDeviceInProcess(true);
                        break;
                    }
                }
                deviceListAdapter.notifyDataSetChanged();
                break;

        }


    }
}
