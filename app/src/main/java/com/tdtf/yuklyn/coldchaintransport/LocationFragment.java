package com.tdtf.yuklyn.coldchaintransport;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.tdtf.yuklyn.coldchaintransport.helper.DatabaseHelper;
import com.tdtf.yuklyn.coldchaintransport.service.BDLocationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by YukLyn on 2016/4/15.
 */
public class LocationFragment extends Fragment {
    private View rootView;
    private ImageView imgV_BigNode;
    private Switch sw_Location;

    private static List<Map<String,String>> locationList;
    private LocationListAdapter locationListAdapter;

    private MainActivity mainActivity;

    private Animation animTwinkle;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    private DatabaseHelper databaseHelper;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        IntentFilter filter_Service = new IntentFilter();
        filter_Service.addAction(CONSTANT.BROADCAST_LOCATION_SERVICE_TO_FRAGMENT);

        mainActivity = (MainActivity)getActivity();
        databaseHelper = ((LocationApplication)mainActivity.getApplication()).databaseHelper;

        locationList = new ArrayList<>();
        Cursor cursor = databaseHelper.getReadableDatabase().query(getString(R.string.table_location),
                new String[]{"time","location"},null,null,null,null,null);
        while (cursor.moveToNext()){
            Map<String,String> location = new HashMap<>();
            location.put("time",cursor.getString(0));
            location.put("address",cursor.getString(1));
            locationList.add(location);
        }
        cursor.close();

        animTwinkle = AnimationUtils.loadAnimation(mainActivity, R.anim.twinkle);
        animTwinkle.setInterpolator(new LinearInterpolator());

        preferences = mainActivity.getSharedPreferences(CONSTANT.SHARED_PREFERENCE_NAME,Context.MODE_PRIVATE);
        editor = preferences.edit();
        editor.apply();

        bdLocationService = ((LocationApplication)mainActivity.getApplication()).bdLocationService;
        bdLocationService.registerListener(mListener);
        bdLocationService.setLocationOption(bdLocationService.getDefaultLocationClientOption());
    }

    @Override
    public View onCreateView(LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState){
        if(mainActivity.menu != null){
            mainActivity.menu.getItem(0).setVisible(true);
            mainActivity.menu.getItem(1).setVisible(false);
        }

        if(null == rootView){
            rootView = inflater.inflate(R.layout.fragment_location_layout,container,false);

            mainActivity.locationFragment = this;
            imgV_BigNode = (ImageView)rootView.findViewById(R.id.imageView_bigNode);
            sw_Location = (Switch)rootView.findViewById(R.id.switch_Location);
            sw_Location.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        if (animTwinkle != null) {
                            imgV_BigNode.setAnimation(animTwinkle);
                            imgV_BigNode.startAnimation(animTwinkle);
                        }
                        System.out.println("--------------bdLocationService.start--------------");
                        bdLocationService.start();
                        time = 0;
                        lastTime = 0;
                        timerHandle.postDelayed(LocationTimer,CONSTANT.DURATION_STOP);
                    } else {
                        if (animTwinkle != null) {
                            imgV_BigNode.clearAnimation();
                        }
                        bdLocationService.stop();System.out.println("--------------bdLocationService.stop--------------");
                        timerHandle.removeCallbacks(LocationTimer);
                    }
                }
            });
            locationListAdapter = new LocationListAdapter(mainActivity);
            ListView lv_Location = (ListView)rootView.findViewById(R.id.list_Location);
            lv_Location.setAdapter(locationListAdapter);
        }

        if(imgV_BigNode != null && sw_Location.isChecked()){
            imgV_BigNode.setAnimation(animTwinkle);
            imgV_BigNode.startAnimation(animTwinkle);
        }

        ViewGroup parent = (ViewGroup) rootView.getParent();
        if (parent != null)
        {
            parent.removeView(rootView);
        }

        return rootView;
    }

    @Override
    public void onDestroy(){
        imgV_BigNode.clearAnimation();
        if(sw_Location.isChecked()){
            bdLocationService.stop();
            timerHandle.removeCallbacks(LocationTimer);
            bdLocationService.unregisterListener(mListener);
        }
        super.onDestroy();
    }

    public void clearLocationInfo(){
        databaseHelper.getReadableDatabase().execSQL("delete from " + getString(R.string.table_location));
        locationList.clear();
        locationListAdapter.notifyDataSetInvalidated();
    }

    private class LocationListAdapter extends BaseAdapter{
        private LayoutInflater mInflater;
        private Context mContext = null;

        public LocationListAdapter(Context context) {
            mContext = context;
            mInflater = LayoutInflater.from(mContext);
        }

        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return locationList.get(arg0);
        }
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }
        public int getCount() {
            // TODO Auto-generated method stub
            return locationList.size();
        }
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_list_location,null);
                holder = new ViewHolder();

                holder.tvTimeHolder = (TextView) convertView.findViewById(R.id.location_list_item_time);
                holder.tvAddressHolder = (TextView) convertView.findViewById(R.id.location_list_item_location);
                holder.ivNodeHolder = (ImageView) convertView.findViewById(R.id.location_list_item_cycle);
                holder.vLineHolder = convertView.findViewById(R.id.location_list_item_line);
                convertView.setTag(holder);
            }
            else{
                holder = (ViewHolder)convertView.getTag();
            }
            holder.tvTimeHolder.setText(locationList.get(position).get("time"));
            holder.tvAddressHolder.setText(locationList.get(position).get("address"));
            return convertView;
        }
    }

    public final class ViewHolder{
        public TextView tvTimeHolder;
        public TextView tvAddressHolder;
        public View vLineHolder;
        public ImageView ivNodeHolder;
    }


    private BDLocationService bdLocationService;
    private long lastTime = 0;
    private int time = 0;

    private BDLocationListener mListener = new BDLocationListener() {

        @Override
        public void onReceiveLocation(BDLocation location) { System.out.println("--------------BDLocationListener.start--------------");
            // TODO Auto-generated method stub
            if(null != location && null == location.getAddrStr()){
                timerHandle.removeCallbacks(LocationTimer);
                bdLocationService.stop();
                bdLocationService.start();
                time = 0;
                timerHandle.postDelayed(LocationTimer,CONSTANT.DURATION_STOP);
                return;
            }
            long currentTime = System.currentTimeMillis();
            if (null != location &&
                    location.getLocType() != BDLocation.TypeServerError &&
                    currentTime - lastTime > 8000) {

                lastTime = System.currentTimeMillis();
                System.out.println("lastTime: " + lastTime);
                System.out.println("currentTime: " + currentTime);
                System.out.println("location: " + location.getAddrStr());
                System.out.println("latitude: " + Double.toString(location.getLatitude()));
                System.out.println("longitude: " + Double.toString(location.getLongitude()));
                String time = location.getTime().substring(0,16);
                /*Toast.makeText(mainActivity,"位置: " + location.getAddrStr() +
                        "\n纬度: " + Double.toString(location.getLatitude()) +
                        "\n经度: " + Double.toString(location.getLongitude()) +
                        "\nlocType: " + location.getLocType(),Toast.LENGTH_SHORT).show();*/

                if(locationList.size() == 0){
                    editor.putString(CONSTANT.START_LOCATION,location.getAddrStr());
                    editor.putString(CONSTANT.START_TIME,time);
                }
                editor.putString(CONSTANT.CURRENT_LOCATION,location.getAddrStr());
                editor.putString(CONSTANT.CURRENT_TIME,time);
                editor.apply();
                Map<String, String> locationListItem = new HashMap<>();
                locationListItem.put("time", time);
                locationListItem.put("address", location.getAddrStr());
                locationListItem.put("latitude", Double.toString(location.getLatitude()));
                locationListItem.put("longitude", Double.toString(location.getLongitude()));
                locationList.add(locationListItem);
                locationListAdapter.notifyDataSetInvalidated();

                databaseHelper.getReadableDatabase().execSQL("insert into " +
                                getString(R.string.table_location) +
                                "(SID,time,location,longitude,latitude) values(?,?,?,?,?)",
                        new String[]{preferences.getString(CONSTANT.CURRENT_DEVICE_SID,null),
                                time,location.getAddrStr(),
                                Double.toString(location.getLongitude()), Double.toString(location.getLatitude())});
                System.out.println("--------------BDLocationListener.end--------------");
            }
        }
    };

    Runnable LocationTimer = new Runnable() {
        @Override
        public void run() {
            time += CONSTANT.DURATION_STOP;
            /*if(time == CONSTANT.DURATION_UPLOAD){
                time = 0;
            }*/
            if(time == CONSTANT.DURATION_LOCATE){
                bdLocationService.start();
                System.out.println("--------------开始定位服务--------------");
                time = 0;
            }
            if(time == CONSTANT.DURATION_STOP){
                System.out.println("--------------停止定位服务--------------");
             bdLocationService.stop();
            }
            timerHandle.postDelayed(this,CONSTANT.DURATION_STOP);
        }
    };
    Handler timerHandle = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };
}
