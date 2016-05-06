package com.tdtf.yuklyn.coldchaintransport;

/**
 * Created by yuklyn on 2016/4/17.
 */

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.tdtf.yuklyn.coldchaintransport.helper.DatabaseHelper;
import com.tdtf.yuklyn.coldchaintransport.helper.NetWorkHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetFragment extends Fragment{
    private ImageButton btn_AddSID;
    private TextView tv_currentSID;
    private TextView tv_currentName;
    private TextView tv_historyDevice;
    private View rootView;

    private List<Map<String,String>> deviceList;
    private DeviceListAdapter deviceListAdapter;

    private MainActivity mainActivity;
    private NetWorkHelper netWorkHelper;
    private DatabaseHelper databaseHelper;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private BroadcastReceiver broadcastReceiver_LoginActivity;

    private Animation animRotate;
    private Animation animTransRLL;
    private Animation animTransRL;
    private Animation animTransLRR;
    private Animation animTransLR;

    private String changeSID;
    private String changeName;


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        mainActivity = (MainActivity) getActivity();
        deviceList = new ArrayList<>();

        preferences = mainActivity.getSharedPreferences(CONSTANT.SHARED_PREFERENCE_NAME,Context.MODE_PRIVATE);
        editor = preferences.edit();
        editor.apply();

        netWorkHelper = ((LocationApplication)(mainActivity.getApplication())).netWorkHelper;
        databaseHelper = ((LocationApplication)mainActivity.getApplication()).databaseHelper;

        broadcastReceiver_LoginActivity = new MyBroadcastReceiver();
        IntentFilter filter_Login = new IntentFilter();
        filter_Login.addAction(CONSTANT.INTENT_DEVICE_CHANGE);
        mainActivity.registerReceiver(broadcastReceiver_LoginActivity, filter_Login);

        LinearInterpolator interpolator = new LinearInterpolator();
        animTransRLL = AnimationUtils.loadAnimation(mainActivity, R.anim.translaterll);
        animTransRLL.setInterpolator(interpolator);
        animTransRL = AnimationUtils.loadAnimation(mainActivity, R.anim.translaterl);
        animTransRL.setInterpolator(interpolator);
        animTransLRR = AnimationUtils.loadAnimation(mainActivity, R.anim.translatelrr);
        animTransLRR.setInterpolator(interpolator);
        animTransLR = AnimationUtils.loadAnimation(mainActivity, R.anim.translatelr);
        animTransLR.setInterpolator(interpolator);
    }
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(mainActivity.menu != null){
            mainActivity.menu.getItem(0).setVisible(false);
            mainActivity.menu.getItem(1).setVisible(false);
        }

        if(null == rootView)
        {
            rootView = inflater.inflate(R.layout.fragment_set_layout,container,false);

            mainActivity.setFragment = this;
            btn_AddSID = (ImageButton)rootView.findViewById(R.id.btnAddSID);
            btn_AddSID.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    animRotate = AnimationUtils.loadAnimation(mainActivity, R.anim.rotate);
                    animRotate.setInterpolator(new LinearInterpolator());
                    animRotate.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            Intent intent = new Intent(mainActivity,LoginActivity.class);
                            intent.putExtra(CONSTANT.INTENT_ACTIVITY,CONSTANT.INTENT_SETFRAGMENT_TO_LOGIN);
                            startActivity(intent);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {

                        }
                    });
                    if (animRotate != null) {
                        btn_AddSID.startAnimation(animRotate);
                    }
                }
            });

            tv_historyDevice = (TextView)rootView.findViewById(R.id.tv_historyDevice);
            tv_currentName = (TextView)rootView.findViewById(R.id.tv_currentName);
            tv_currentSID = (TextView)rootView.findViewById(R.id.tv_currentSID) ;
            setCurrentDeviceTextViewText();

            ListView lv_DeviceList = (ListView)rootView.findViewById(R.id.listView_SID);
            deviceListAdapter = new DeviceListAdapter(mainActivity);
            lv_DeviceList.setAdapter(deviceListAdapter);
            refreshDeviceList();
        }
        return rootView;
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(CONSTANT.INTENT_DEVICE_CHANGE)) {
                clearSharePreferenceInfo();
                setCurrentDeviceTextViewText();

                FragmentManager fm = mainActivity.getSupportFragmentManager();
                LocationFragment locationFragment = (LocationFragment)fm.findFragmentByTag(getString(R.string.locate));
                locationFragment.clearLocationInfo();

                refreshDeviceList();
            }
        }
    }


    @Override
    public void onDestroy(){
        super.onDestroy();
        mainActivity.unregisterReceiver(broadcastReceiver_LoginActivity);
    }

    public void setCurrentDeviceTextViewText(){
        tv_currentName.setText(getString(R.string.current_name));
        tv_currentName.append(preferences.getString(CONSTANT.CURRENT_DEVICE_NAME,getString(R.string.unknown_device)));
        tv_currentSID.setText(getString(R.string.current_SID));
        tv_currentSID.append(preferences.getString(CONSTANT.CURRENT_DEVICE_SID,getString(R.string.unknown_device)));
    }

    public void clearSharePreferenceInfo(){
        editor.remove(CONSTANT.START_LOCATION);
        editor.remove(CONSTANT.START_TIME);
        editor.remove(CONSTANT.CURRENT_LOCATION);
        editor.remove(CONSTANT.CURRENT_TIME);
        editor.apply();
    }

    public void refreshDeviceList(){
        deviceList.clear();
        Cursor cursor = databaseHelper.getReadableDatabase().query(getString(R.string.table_devices),
                new String[]{"SID","name"},null,null,null,null,null);
        String currentDeviceSID = preferences.getString(CONSTANT.CURRENT_DEVICE_SID,getString(R.string.unknown_device));
        while (cursor.moveToNext()){
            if(currentDeviceSID.equals(cursor.getString(0)))continue;
            Map<String,String> device = new HashMap<>();
            device.put("SID",cursor.getString(0));
            device.put("name",cursor.getString(1));
            deviceList.add(device);
        }
        cursor.close();
        deviceListAdapter.notifyDataSetInvalidated();
        if(deviceList.isEmpty()){
            tv_historyDevice.setVisibility(View.INVISIBLE);
        }else {
            tv_historyDevice.setVisibility(View.VISIBLE);
        }
    }

    private class DeviceListAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private Context mContext = null;

        public DeviceListAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(mContext);
        }

        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return deviceList.get(arg0);
        }
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }
        public int getCount() {
            // TODO Auto-generated method stub
            return deviceList.size();
        }
        public View getView(final int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_list_device,null);
                holder = new ViewHolder();

                holder.tvNAME = (TextView) convertView.findViewById(R.id.tv_deviceName);
                holder.tvSID = (TextView) convertView.findViewById(R.id.tv_deviceSID);
                holder.imgBtnSet = (ImageButton) convertView.findViewById(R.id.imgBtn_set);
                holder.imgBtnChange = (ImageButton) convertView.findViewById(R.id.imgBtn_change);
                holder.imgBtnDelete = (ImageButton) convertView.findViewById(R.id.imgBtn_delete);
                holder.progressBar = (ProgressBar) convertView.findViewById(R.id.progress_change);
                convertView.setTag(holder);
            }
            else{
                holder = (ViewHolder)convertView.getTag();
            }
            holder.tvSID.setText(deviceList.get(position).get("SID"));
            holder.tvNAME.setText(deviceList.get(position).get("name"));
            holder.progressBar.setVisibility(View.INVISIBLE);
            holder.imgBtnDelete.setVisibility(View.INVISIBLE);
            holder.imgBtnChange.setVisibility(View.INVISIBLE);

            holder.imgBtnSet.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(holder.imgBtnChange.getVisibility() == View.VISIBLE){
                        holder.imgBtnDelete.startAnimation(animTransLR);
                        holder.imgBtnChange.startAnimation(animTransLRR);
                        holder.imgBtnChange.setVisibility(View.INVISIBLE);
                        holder.imgBtnDelete.setVisibility(View.INVISIBLE);
                    }
                    else{
                        holder.imgBtnChange.setVisibility(View.VISIBLE);
                        holder.imgBtnDelete.setVisibility(View.VISIBLE);
                        holder.imgBtnDelete.startAnimation(animTransRL);
                        holder.imgBtnChange.startAnimation(animTransRLL);
                    }
                }
            });
            holder.imgBtnChange.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.progressBar.setVisibility(View.VISIBLE);
                    holder.imgBtnDelete.setVisibility(View.INVISIBLE);
                    holder.imgBtnChange.setVisibility(View.INVISIBLE);
                    holder.imgBtnSet.setVisibility(View.INVISIBLE);
                    changeSID = deviceList.get(position).get("SID");
                    changeName = deviceList.get(position).get("name");

                    new AsyncTask<String, Void, Integer>() {
                        @Override
                        protected Integer doInBackground(String... urls) {
                            return netWorkHelper.deviceState(changeSID,urls[0],CONSTANT.PORT);
                        }

                        @Override
                        protected void onPostExecute(Integer result) {
                            holder.progressBar.setVisibility(View.INVISIBLE);
                            holder.imgBtnSet.setVisibility(View.VISIBLE);
                            switch (result) {
                                case CONSTANT.INVALID:
                                    Toast.makeText(mainActivity,R.string.cannot_find_device, Toast.LENGTH_SHORT).show();
                                    break;
                                case CONSTANT.VALID:
                                    AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity).setTitle(R.string.app_name).setIcon(R.drawable.ic_transport).setMessage(R.string.change_device_info);
                                    builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            editor.putString("SID",changeSID);
                                            editor.putString("name",changeName);
                                            editor.apply();
                                            clearSharePreferenceInfo();
                                            setCurrentDeviceTextViewText();

                                            FragmentManager fm = mainActivity.getSupportFragmentManager();
                                            LocationFragment locationFragment = (LocationFragment)fm.findFragmentByTag(getString(R.string.locate));
                                            locationFragment.clearLocationInfo();

                                            refreshDeviceList();
                                        }
                                    }).setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    });
                                    AlertDialog alertDialog = builder.create();
                                    alertDialog.show();
                                    break;
                                case CONSTANT.ERROR_READ:
                                    Toast.makeText(mainActivity,getString(R.string.get_server_info_error),Toast.LENGTH_SHORT).show();
                                    break;
                                case CONSTANT.ERROR_TIMEOUT:
                                    Toast.makeText(mainActivity,getString(R.string.get_server_timeout_error),Toast.LENGTH_SHORT).show();
                                    break;
                                case CONSTANT.ERROR_SE:
                                    Toast.makeText(mainActivity,getString(R.string.get_server_info_error),Toast.LENGTH_SHORT).show();
                                    break;
                                case CONSTANT.ERROR_IOE:
                                    Toast.makeText(mainActivity,getString(R.string.get_server_info_error),Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }
                    }.execute(CONSTANT.IP);
                }
            });
            holder.imgBtnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    databaseHelper.getReadableDatabase().execSQL("delete from " + getString(R.string.table_devices) +
                    " where SID like " + deviceList.get(position).get("SID"));
                    refreshDeviceList();
                }
            });
            return convertView;
        }
    }

    public final class ViewHolder{
        public TextView tvSID;
        public TextView tvNAME;
        public ImageButton imgBtnChange;
        public ImageButton imgBtnDelete;
        public ImageButton imgBtnSet;
        public ProgressBar progressBar;
    }
}