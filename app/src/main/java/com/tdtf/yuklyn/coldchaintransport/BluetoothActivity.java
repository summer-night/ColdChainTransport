package com.tdtf.yuklyn.coldchaintransport;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by YukLyn on 2016/4/14.
 */
public class BluetoothActivity extends AppCompatActivity {
    private TextView tv_PairedDevice;
    private TextView tv_UnPairedDevice;
    private Switch sw_BlueTooth;
    private ProgressBar pb_Search;

    private BluetoothAdapter bluetoothAdapter;
    private PrintStream printStreamOut;

    private List<Map<String,Object>> pairedDeviceList;
    private List<Map<String,Object>> unPairedDeviceList;
    private BluetoothListAdapter pairedDeviceAdapter;
    private SimpleAdapter unPairedDeviceAdapter;

    private BroadcastReceiver broadcastReceiver;

    private String printInfo;
    private ProgressDialog dialog;

    static class MyHandler extends Handler{
        Context context;
        public MyHandler(Context context){
            this.context = context;
        }
        @Override
        public void handleMessage(Message msg){
            if(msg.what == CONSTANT.CONNECTED){
                new AsyncTask<Void, Void, Integer>() {
                    @Override
                    protected Integer doInBackground(Void... params) {
                        return ((BluetoothActivity)context).print();
                    }

                    @Override
                    protected void onPostExecute(Integer result) {
                        switch (result){
                            case CONSTANT.FAILED:
                                Toast.makeText(context,context.getString(R.string.connect_failed),Toast.LENGTH_SHORT).show();
                                break;
                            case CONSTANT.SUCCESS:
                                break;
                        }
                    }
                }.execute();
            }
        }
    }
    private MyHandler handler;

    @Override
    protected void onDestroy(){
        bluetoothAdapter.cancelDiscovery();
        BluetoothActivity.this.unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.bluetooth_layout);

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar)findViewById(R.id.activity_bluetooth_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        printInfo = getIntent().getStringExtra("printInfo");
        handler = new MyHandler(this);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int i;
                    for (i = 0; i < unPairedDeviceList.size(); i++)
                        if (unPairedDeviceList.get(i).containsValue(bluetoothDevice.getAddress()))
                            break;
                    if (i == unPairedDeviceList.size()) {
                        Map<String, Object> unPairedDeviceMap = new HashMap<>();
                        unPairedDeviceMap.put("name", bluetoothDevice.getName());
                        unPairedDeviceMap.put("address", bluetoothDevice.getAddress());
                        unPairedDeviceMap.put("device", bluetoothDevice);
                        unPairedDeviceList.add(unPairedDeviceMap);
                        unPairedDeviceAdapter.notifyDataSetInvalidated();
                    }
                }
                if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    pb_Search.setVisibility(View.INVISIBLE);
                }
            }
        };
        BluetoothActivity.this.registerReceiver(broadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        BluetoothActivity.this.registerReceiver(broadcastReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));

        pb_Search = (ProgressBar) findViewById(R.id.searchProgress);
        if (pb_Search != null)
            pb_Search.setVisibility(View.INVISIBLE);

        tv_PairedDevice = (TextView) findViewById(R.id.bondedDevice);
        pairedDeviceList = new ArrayList<>();
        pairedDeviceAdapter = new BluetoothListAdapter(this);
        ListView lv_PairedDevice = (ListView) findViewById(R.id.bondedDeviceList);
        if (lv_PairedDevice != null)
            lv_PairedDevice.setAdapter(pairedDeviceAdapter);

        tv_UnPairedDevice = (TextView) findViewById(R.id.unBondedDevice);
        unPairedDeviceList = new ArrayList<>();
        unPairedDeviceAdapter = new SimpleAdapter(BluetoothActivity.this, unPairedDeviceList, R.layout.item_list_bluetooth_device,
                new String[]{"name", "address"}, new int[]{R.id.tv_bluetoothDeviceName, R.id.tv_bluetoothDeviceAddress});
        ListView lv_UnPairedDevice = (ListView) findViewById(R.id.unBondedDeviceList);
        if (lv_UnPairedDevice != null)
            lv_UnPairedDevice.setAdapter(unPairedDeviceAdapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        sw_BlueTooth = (Switch) findViewById(R.id.bluetoothSwitch);
        sw_BlueTooth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    boolean result = bluetoothAdapter.enable();
                    if (result) {
                        while (!bluetoothAdapter.isEnabled()) {
                            int i = 0;
                            i += 1;
                            System.out.println(i);
                        }
                        sw_BlueTooth.setChecked(true);
                        getBondedDevice();
                        tv_PairedDevice.setText(R.string.paired_device);
                    } else
                        sw_BlueTooth.setChecked(false);
                } else {
                    bluetoothAdapter.disable();

                    pairedDeviceList.clear();
                    pairedDeviceAdapter.notifyDataSetInvalidated();
                    tv_PairedDevice.setText("");
                    unPairedDeviceList.clear();
                    unPairedDeviceAdapter.notifyDataSetInvalidated();
                    tv_UnPairedDevice.setText("");

                    pb_Search.setVisibility(View.INVISIBLE);
                }
            }
        });
        if (bluetoothAdapter.isEnabled()) {
            Toast.makeText(BluetoothActivity.this, R.string.bluetooth_adapter_is_open, Toast.LENGTH_SHORT).show();
            sw_BlueTooth.setChecked(true);
        } else {
            Toast.makeText(BluetoothActivity.this, R.string.bluetooth_adapter_is_close, Toast.LENGTH_SHORT).show();
            sw_BlueTooth.setChecked(false);
        }

        dialog = new ProgressDialog(BluetoothActivity.this);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMessage(getString(R.string.under_printing));
        dialog.setCancelable(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.getItem(0).setVisible(false);
        menu.getItem(1).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_refresh:
                if (bluetoothAdapter.isEnabled()) {
                    pb_Search.setVisibility(View.VISIBLE);
                    bluetoothAdapter.startDiscovery();
                    tv_UnPairedDevice.setText(R.string.found_device);
                }
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

    private void getBondedDevice(){
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        pairedDeviceList.clear();
        for(BluetoothDevice bluetoothDevice:pairedDevices){
            Map<String,Object> bondedDeviceMap = new HashMap<>();
            bondedDeviceMap.put("name",bluetoothDevice.getName());
            bondedDeviceMap.put("address",bluetoothDevice.getAddress());
            bondedDeviceMap.put("device",bluetoothDevice);
            pairedDeviceList.add(bondedDeviceMap);
        }
        pairedDeviceAdapter.notifyDataSetInvalidated();
    }

    public int connect(BluetoothDevice device){
        printStreamOut = null;
        try {
            UUID SERIAL_UUID = device.getUuids()[0].getUuid();
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SERIAL_UUID);
            if(socket != null){
                try{
                    socket.connect();
                    printStreamOut = new PrintStream(socket.getOutputStream());
                }catch (IOException e) {
                    return CONSTANT.FAILED;
                }
            }
        } catch (IOException e) {
            return CONSTANT.FAILED;
        }
        return CONSTANT.CONNECTED;
    }
    public int print(){
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(printStreamOut,"GBK"));
            String[] msg = printInfo.split("\n");
            int i;
            int rowCount = msg.length;
            dialog.setMax(rowCount);
            for (i = 0;i < rowCount;i++)
            {
                bufferedWriter.write(msg[i] + "\n");
                dialog.setProgress(i);
            }
            dialog.setProgress(rowCount);
            bufferedWriter.write("\n\n\n");
            bufferedWriter.flush();
        }
        catch (IOException e){
            dialog.dismiss();
            return CONSTANT.FAILED;
        }
        dialog.dismiss();
        return CONSTANT.SUCCESS;
    }

    private class BluetoothListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private Context mContext = null;

        public BluetoothListAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(mContext);
        }

        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return pairedDeviceList.get(arg0);
        }
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }
        public int getCount() {
            // TODO Auto-generated method stub
            return pairedDeviceList.size();
        }
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_list_bluetooth_device,null);
                holder = new ViewHolder();

                holder.tvMac = (TextView) convertView.findViewById(R.id.tv_bluetoothDeviceAddress);
                holder.tvNAME = (TextView) convertView.findViewById(R.id.tv_bluetoothDeviceName);
                holder.imgBtnPrint = (ImageButton) convertView.findViewById(R.id.imgBtn_print2);
                convertView.setTag(holder);
            }
            else{
                holder = (ViewHolder)convertView.getTag();
            }
            holder.tvMac.setText((String)pairedDeviceList.get(position).get("address"));
            holder.tvNAME.setText((String)pairedDeviceList.get(position).get("name"));
            holder.imgBtnPrint.setVisibility(View.VISIBLE);
            holder.imgBtnPrint.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothAdapter.cancelDiscovery();
                    pb_Search.setVisibility(View.INVISIBLE);
                    final BluetoothDevice device = (BluetoothDevice) pairedDeviceList.get(position).get("device");
                    dialog.setMax(100);
                    dialog.show();
                    dialog.setProgress(0);
                    new AsyncTask<Void, Void, Integer>() {
                        @Override
                        protected Integer doInBackground(Void... params) {
                            return connect(device);
                        }

                        @Override
                        protected void onPostExecute(Integer result) {
                            switch (result){
                                case CONSTANT.FAILED:
                                    dialog.dismiss();
                                    Toast.makeText(BluetoothActivity.this,getString(R.string.connect_failed),Toast.LENGTH_SHORT).show();
                                    break;
                                case CONSTANT.CONNECTED:
                                    handler.sendEmptyMessage(CONSTANT.CONNECTED);
                                    break;
                            }
                        }
                    }.execute();
                }
            });
            return convertView;
        }
    }
    public final class ViewHolder{
        public TextView tvMac;
        public TextView tvNAME;
        public ImageButton imgBtnPrint;
    }
}
