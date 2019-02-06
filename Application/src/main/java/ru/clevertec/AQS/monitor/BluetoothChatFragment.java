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

package ru.clevertec.AQS.monitor;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;

import org.w3c.dom.Text;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import ru.clevertec.AQS.common.AppStorage;
import ru.clevertec.AQS.common.logger.Log;
import ru.clevertec.AQS.monitor.protocol.in.DataTransfer;
import ru.clevertec.AQS.monitor.protocol.in.Status;
import ru.clevertec.AQS.monitor.protocol.service.BluetoothChatService;
import ru.clevertec.AQS.storage.DLog;
import ru.clevertec.AQS.storage.Database;

/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private LineChart mChart;
    private Button mPrevButton, mZoomMinusButton, mUpdateButton, mZoomPlusButton, mNextButton,mSyncButton, mReadButton;
    private TextView mStatusTextView;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    ChartSpan mChartSpan = ChartSpan.MINUTE;

    private long mChartEndSec;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }

        mChartEndSec = (System.currentTimeMillis() + TimeZone.getDefault().getRawOffset() + TimeZone.getDefault().getDSTSavings()) / 1000;

        setUpChartButtonHandlers();
        setUpCommandButtonHandlers();

        updateChart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mChart = (LineChart) view.findViewById(R.id.chart);

        mPrevButton = (Button) view.findViewById(R.id.btnPrev);
        mZoomMinusButton = (Button) view.findViewById(R.id.btnZoomMinus);
        mUpdateButton = (Button) view.findViewById(R.id.btnUpdate);
        mZoomPlusButton = (Button) view.findViewById(R.id.btnZoomPlus);
        mNextButton = (Button) view.findViewById(R.id.btnNext);

        mSyncButton = (Button) view.findViewById(R.id.btnSync);
        mReadButton = (Button) view.findViewById(R.id.btnRead);

        mStatusTextView = (TextView) view.findViewById(R.id.textViewStatus);
    }

    private void setUpChartButtonHandlers() {
        mUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateChart();
            }
        });
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChartEndSec -= mChartSpan.getGranularitySec()*2;
                updateChart();
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChartEndSec += mChartSpan.getGranularitySec()*2;
                updateChart();
            }
        });
        mZoomMinusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChartSpan = mChartSpan.getNext();
                updateChart();
            }
        });
        mZoomPlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mChartSpan = mChartSpan.getPrev();
                updateChart();
            }
        });

    }

    private boolean checkConnectionAndInform() {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;

    }
    private void setUpCommandButtonHandlers() {

        mSyncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkConnectionAndInform()) {
                    mChatService.sync();
                    Toast.makeText(getContext(), R.string.sent_sync, Toast.LENGTH_LONG).show();
                }
            }
        });

        mReadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkConnectionAndInform()) {
                    mChatService.readData();
                }
            }
        });
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Toast.makeText(getContext(), "Me:  " + writeMessage, Toast.LENGTH_LONG).show();
                    break;
                case Constants.MESSAGE_READ:
                    if (msg.obj instanceof Status) {
                        Status s = (Status) msg.obj;
                        // construct a string from the valid bytes in the buffer
                        String readMessage = String.format("%.2fC %.2f%% L%d", s.getData().getTemperature(), s.getData().getHumidity(), s.getLogIdx());
                        mStatusTextView.setText(readMessage);
                        AppStorage.setLastLogIndex(getContext(), s.getLogIdx());
                    } else if (msg.obj instanceof DataTransfer) {
                        DataTransfer d  = (DataTransfer)msg.obj;
                        //todo: it's better to move all data handling (including saving data and last indicies) to the service rather than keeping it in the UI
                        String readMessage = String.format("DLogs: %d-%d (%d)", d.getFromIdx(), d.getToIdx(), d.getDLogs().length);
                        Toast.makeText(getContext(), readMessage, Toast.LENGTH_LONG).show();
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
            case R.id.command_reset_local_storage: {
                mChatService.resetLocalStorage();
                return true;
            }
            case R.id.command_reset_sensor_storage: {
                mChatService.resetLocalStorage();
                mChatService.resetStorage();
                return true;
            }
        }
        return false;
    }

    public void updateChart() {
        XAxis xAxis = mChart.getXAxis();

        mChartEndSec = mChartSpan.adjustTimeSec(mChartEndSec);

        xAxis.setAxisMinimum(0.0F);
        xAxis.setAxisMaximum((float)mChartSpan.getSpanSec());

        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularityEnabled(true);
        xAxis.setGranularity(mChartSpan.getGranularitySec());
        xAxis.setValueFormatter(new DateTimeAxisValueFormatter(mChartSpan, mChartEndSec-mChartSpan.getSpanSec()));

        List<Entry> entriesL = new ArrayList<>();
        List<Entry> entriesR = new ArrayList<>();


        //todo: shound't be executed on the UI thread...
        List<DLog> dlogs = Database.getDatabase(getContext()).getDLogDao().getInRange(mChartEndSec-mChartSpan.getSpanSec(), mChartEndSec);
        for (ru.clevertec.AQS.storage.DLog dlog : dlogs) {
            float time = dlog.rtime - (mChartEndSec-mChartSpan.getSpanSec());
            entriesL.add(new Entry(time, dlog.temp));
            entriesR.add(new Entry(time, dlog.hum));
        }

        Description desc = new Description();

        desc.setText(new SimpleDateFormat("dd.MM.YYYY").format(new Date(mChartEndSec*1000L)));

        mChart.setDescription(desc);

        LineDataSet dataSetL = new LineDataSet(entriesL,"Temp");
        dataSetL.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSetL.setColor(Color.BLUE);
        dataSetL.setDrawCircles(false);

        LineDataSet dataSetR = new LineDataSet(entriesR, "Hum");
        dataSetR.setAxisDependency(YAxis.AxisDependency.RIGHT);
        dataSetR.setColor(Color.GREEN);
        dataSetR.setDrawCircles(false);

        LineData data = new LineData(dataSetL);
        data.addDataSet(dataSetR);

        mChart.setData(data);

        Legend legend = mChart.getLegend();
        legend.setEnabled(true);
        legend.setDrawInside(false);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);


        mChart.invalidate();
    }

    public class DateTimeAxisValueFormatter implements IAxisValueFormatter {

        private SimpleDateFormat dateFormat;

        private long baseTimeSec;

        public DateTimeAxisValueFormatter(ChartSpan span, long baseTimeSec) {
            switch (span) {
                case MINUTE:
                case FIVE_MINUTES:
                    dateFormat = new java.text.SimpleDateFormat("HH:mm:ss");
                    break;
                case HALF_HOUR:
                case HOUR:
                case EIGHT_HOURS:
                    dateFormat = new java.text.SimpleDateFormat("HH:mm");
                    dateFormat = new java.text.SimpleDateFormat("HH:mm");
                    break;
                case DAY:
                    dateFormat = new java.text.SimpleDateFormat("dd.MM HH");
                case WEEK:
                    dateFormat = new java.text.SimpleDateFormat("dd.MM");
                    break;
            }
            this.baseTimeSec = baseTimeSec;
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            // "value" represents the position of the label on the axis (x or y)
            return dateFormat.format(new Date(1000L*(baseTimeSec + (long)value)));
        }

    }

}
