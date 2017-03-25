package es.carlosrolindez.btsppcomm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;

import es.carlosrolindez.btcomm.BtConstants;
import es.carlosrolindez.rfcomm.RfServerManager;


public class BtSppServerManager extends RfServerManager<BluetoothSocket,BluetoothServerSocket> {
    private static final String TAG = "BtSppServerManager";

    private static final String CRService = "es.carlosrolindez.btcomm";

    public BtSppServerManager(BtSppCommManager commManager) {
        super(commManager);
    }

    public BluetoothServerSocket openServerSocket(){
        BluetoothServerSocket server;
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        try {
            server = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(CRService, BtConstants.SPP_UUID);
        } catch (IOException e) {
            e.printStackTrace();
            server = null;
        }
        return server;
    }

    public BluetoothSocket serviceAccept(BluetoothServerSocket server) {
        BluetoothSocket socket;
        try {
            socket = server.accept();
        } catch (IOException e) {
            e.printStackTrace();
            socket = null;
        }
        return socket;
    }

    public void closeServerSocket(BluetoothServerSocket server){
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
