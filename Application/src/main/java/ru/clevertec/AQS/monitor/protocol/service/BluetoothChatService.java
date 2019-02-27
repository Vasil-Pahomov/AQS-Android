/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.clevertec.AQS.monitor.protocol.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;

import ru.clevertec.AQS.common.AppStorage;
import ru.clevertec.AQS.common.UnixTimeUtils;
import ru.clevertec.AQS.common.logger.Log;
import ru.clevertec.AQS.monitor.Constants;
import ru.clevertec.AQS.monitor.protocol.DLog;
import ru.clevertec.AQS.monitor.protocol.in.DataTransfer;
import ru.clevertec.AQS.monitor.protocol.in.InCommand;
import ru.clevertec.AQS.monitor.protocol.in.InCommandFactory;
import ru.clevertec.AQS.monitor.protocol.in.Status;
import ru.clevertec.AQS.monitor.protocol.out.ReadData;
import ru.clevertec.AQS.monitor.protocol.out.ResetStorage;
import ru.clevertec.AQS.monitor.protocol.out.Sync;
import ru.clevertec.AQS.storage.Database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final Context mContext;
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private int mNewState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler) {
        mContext = context;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
        mHandler = handler;
    }

    /**
     * Update UI title according to the current state of the chat connection
     */
    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device, secure);
        mConnectThread.start();
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        Log.d(TAG, "connected, Socket Type:" + socketType);

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    public void sync() {
        write(new Sync().getBytes());
    }

    //todo: generalize these "syncronizes" to the ConnectedThread
    public void readData() {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.readData();
    }

    public void resetStorage() {
        write(new ResetStorage().getBytes());
    }

    public void resetLocalStorage() {
        ConnectedThread r;
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        r.resetLocalStorage();
        Database.getDatabase(mContext).getDLogDao().wipe();
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    public void exportToFile() {
        new ExportThread().start();
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
/*                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
                tmp = (BluetoothSocket) m.invoke(device, MY_UUID_INSECURE);*/

                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            MY_UUID_SECURE);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            MY_UUID_INSECURE);
                }
            } catch (Exception e) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                Log.e(TAG, "error connecting", e);
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //todo: think how to avoid such big memory allocation (needed to accumulate data for a day)
        private final byte[] mmRecBuf = new byte[300000];
        private int mmRecBufLen = 0;
        private long lastReadMillis;

        public ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
            lastReadMillis = System.currentTimeMillis();
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[10240];
            int bytes;

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    Log.d(TAG, String.format("received %d bytes: %s",bytes, Arrays.toString(Arrays.copyOf(buffer, bytes))));


                    if (mmRecBufLen != 0 && (System.currentTimeMillis() - lastReadMillis) > 2000) {
                        Log.w(TAG, String.format("receive timeout at buf=%d",mmRecBufLen));
                        //reading timeout - discard what was received previously and start over
                        mmRecBufLen = 0;
                    }

                    if (mmRecBufLen != 0) {
                        //there's something in the receive buffer - assume we're receiving the rest of the request
                        Log.d(TAG, String.format("appending at pos %d, total buffer length=%d",mmRecBufLen,bytes+mmRecBufLen));
                        System.arraycopy(buffer, 0, mmRecBuf, mmRecBufLen, bytes);
                        mmRecBufLen += bytes;
                    } else {
                        //look for signature in the data received and start receiving the command
                        for (int i = 0; i < bytes - 1; i++) {
                            if (buffer[i] == Constants.SIGN_1ST && buffer[i + 1] == Constants.SIGN_2ND) {
                                System.arraycopy(buffer, i + 2, mmRecBuf, 0, bytes - 2 - i);
                                mmRecBufLen = bytes - 2 - i;
                                Log.d(TAG, String.format("signature found at %d, buffer length=%d",i,mmRecBufLen));
                                break;
                            }
                        }
                        if (mmRecBufLen == 0) {
                            Log.w(TAG, String.format("signature not found, data=%s", Arrays.toString(Arrays.copyOf(buffer, bytes))));
                        }
                    }

                    if (mmRecBufLen >= 4)
                    {
                        //the beginning of the command has been received - handle it
                        if (handleCommand()) {
                            mmRecBufLen = 0;
                        }
                    }

                    lastReadMillis = System.currentTimeMillis();

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Tries to handle the command in the buffer
         * @return true, if the command has been handled someway, false otherwise (command hasn't been fully received yet)
         */
        private boolean handleCommand() {
            InCommand incmd = InCommandFactory.getCommand(mmRecBuf);
            if (incmd == null) {
                Log.w(TAG, String.format("Unknown command code: %d", mmRecBuf[0]));
                return true;
            }
            int commandLength = incmd.getCommandLength(mmRecBuf);
            if (incmd instanceof DataTransfer) {
                DataTransfer d = (DataTransfer)incmd;
                Log.d(TAG, String.format("Received data transfer %d-%d", d.getFromIdx(), d.getToIdx()));
            } else {
                Log.d(TAG, String.format("Received command %s, length=%d", incmd.getClass().getSimpleName(), commandLength));
            }
            if (mmRecBufLen > commandLength) {
                if (mmRecBufLen > (commandLength+1)) {
                    Log.w(TAG, String.format("Buffer exceeds command length by %d bytes, total buffer is %s", mmRecBufLen-commandLength, Arrays.toString(mmRecBuf)));
                }
                Log.d(TAG, String.format("Parsing command %s", incmd.getClass().getSimpleName()));
                incmd.Parse(mmRecBuf);
                processCommand(incmd);
                return true;
            } else {
                Log.d(TAG, String.format("Command still incomplete", incmd.getClass().getSimpleName()));
                return false;
            }
        }

        private void processCommand(InCommand incmd) {
            if (incmd instanceof DataTransfer) {
                saveData((DataTransfer)incmd);
            } else if (incmd instanceof Status) {
                Status statusCmd = (Status)incmd;
                AppStorage.setLastLogIndex(mContext, statusCmd.getLogIdx());
                Database db = Database.getDatabase(mContext);
                if (statusCmd.getLogIdx() == db.getDLogDao().getLastId() + 1) {
                    //we have all previous records stored and now receive the next, just store it
                    ru.clevertec.AQS.storage.DLog dlogst = new ru.clevertec.AQS.storage.DLog();
                    dlogst.id = statusCmd.getLogIdx();
                    dlogst.rtime = UnixTimeUtils.getCurrentUnixTime();
                    dlogst.fillValuesFromData(statusCmd.getData());
                    db.getDLogDao().insertAll(dlogst);
                    mHandler.obtainMessage(Constants.MESSAGE_DATA_UPDATED).sendToTarget();
                } else {
                    //we're missing records, request all we're missing
                    readData();
                }
            }

            mHandler.obtainMessage(Constants.MESSAGE_READ, incmd).sendToTarget();
        }

        public void readData()
        {
            int fromIdx = Database.getDatabase(mContext).getDLogDao().getLastId() + 1;
            int toIdx = AppStorage.getLastLogIndex(mContext);
/*            if (toIdx - fromIdx > 500) {
                toIdx = fromIdx + 500;
            }*/
            writeCommand(
                    new ReadData(fromIdx, toIdx).getBytes(),
                    String.format("Read data %d-%d sent", fromIdx, toIdx));
        }

        public void resetLocalStorage() {
            Database.getDatabase(mContext).getDLogDao().wipe();
        }

        private void saveData(DataTransfer d) {
            int timediff = 0;
            int prevssecs = -1;
            for (int i=d.getDLogs().length-1; i>=0; i--) {
                DLog dlog = d.getDLogs()[i];
                if (dlog.getRTime() != 0) {
                    int newtimediff = dlog.getRTime() - dlog.getSSecs();
                    if (timediff != 0 && Math.abs(newtimediff - timediff) > 10) {
                        Log.w(TAG, String.format("Timediff difference is too big for record at %d", i));
                    }
                    timediff = newtimediff;
                } else {
                    if (timediff == 0) {
                        if (prevssecs < 0) {
                            //if the very last record don't have real time, assume it's synced just now
                            Log.w(TAG, String.format("No timediff, assuming not syncronized at %d", i));
                            int offset = TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings();
                            int nowSec = (int) (System.currentTimeMillis() + offset) / 1000;
                            timediff = nowSec - timediff;
                        } else {
                            Log.w(TAG, String.format("No timediff at %d", i));
                        }
                    }
                    dlog.setRTime(dlog.getSSecs() + timediff);
                }
                if (prevssecs >= 0 && prevssecs > dlog.getSSecs()) {
                    //since we're going backwards through the records, the ssecs should typically be decreasing
                    //once it increases, this means we encounter on-off-on transition (that is, the sensor was turned off then back on)
                    //this resets ssecs to 0, and this means that our calculated timediff value is wrong, so discard it
                    timediff = 0;
                }
                prevssecs = dlog.getSSecs();
            }

            Database db = Database.getDatabase(mContext);
            for (int i=0; i<d.getDLogs().length; i++) {
                DLog dlog = d.getDLogs()[i];
                if (dlog.getRTime() >= 1548000000) { //do not write records with incorrect rtime
                    db.getDLogDao().insertAll(new ru.clevertec.AQS.storage.DLog().fillFromProtocol(d.getFromIdx() + i, dlog));
                }
            }

            Message msg = mHandler.obtainMessage(Constants.MESSAGE_DATA_UPDATED);
            mHandler.sendMessage(msg);
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void writeCommand(byte[] buffer, String infoMessage) {
            try {
                mmOutStream.write(buffer);
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, infoMessage.getBytes())
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during writing the command", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }

    }

    private class ExportThread extends Thread {

        @Override
        public void run() {
            List<ru.clevertec.AQS.storage.DLog> dlogs = Database.getDatabase(mContext).getDLogDao().getAll();
            File exst = Environment.getExternalStorageDirectory();
            File dir = new File(exst.getAbsolutePath()+ "/AQS/");
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.e(TAG, String.format("Directory %s not created for file", dir.getAbsolutePath()));
                    return;
                }
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat sdff = new SimpleDateFormat("yyyyMMddHHmmss");
            String filename = sdff.format(new Date());
            File file = new File(dir, filename.replace(' ','_'));
            try {
                file.createNewFile();
                FileOutputStream fOut = new FileOutputStream(file);
                OutputStreamWriter writer = new OutputStreamWriter(fOut);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString(Constants.TOAST, "Start exporting data");
                msg.setData(bundle);
                mHandler.sendMessage(msg);

                for (ru.clevertec.AQS.storage.DLog dlog : dlogs) {
                    String str = String.format("%s\t%.2f\t%.2f\t%d\t%d\t%d\t%d\t%d\r\n",
                            sdf.format(new Date(dlog.rtime * 1000L)),
                            dlog.temp,
                            dlog.hum,
                            dlog.co2,
                            dlog.tvoc,
                            dlog.pm1,
                            dlog.pm25,
                            dlog.pm10);
                    writer.append(str);
                }
                writer.close();
                fOut.flush();
                fOut.close();

                String smsg = String.format("Data exported to file %s in %s", filename, dir.getAbsolutePath());
                msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
                bundle = new Bundle();
                bundle.putString(Constants.TOAST, smsg);
                msg.setData(bundle);
                mHandler.sendMessage(msg);

            } catch (IOException e) {
                String errormsg = String.format("Can't write to file %s in %s", filename, dir.getAbsolutePath());
                Log.e(TAG, errormsg, e);
                Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
                Bundle bundle = new Bundle();
                bundle.putString(Constants.TOAST, errormsg);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }
        }

    }
}
