package com.tdtf.yuklyn.coldchaintransport.service;

/**
 * Created by yuklyn on 2016/4/12.
 */

import android.content.Context;
import android.os.PowerManager;

import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

/**
 *
 * @author baidu
 *
 */
public class BDLocationService {
    private LocationClient client = null;
    private LocationClientOption mOption,DIYOption;
    private Object objLock = new Object();
    private PowerManager.WakeLock wakeLock;
    private Context locationContext;

    /***
     *
     * @param locationContext
     */
    public BDLocationService(Context locationContext){
        this.locationContext = locationContext;
        synchronized (objLock) {
            if(client == null){
                client = new LocationClient(locationContext);
                client.setLocOption(getDefaultLocationClientOption());
            }
        }
    }

    /***
     *
     * @param listener
     * @return
     */

    public boolean registerListener(BDLocationListener listener){
        boolean isSuccess = false;
        if(listener != null){
            client.registerLocationListener(listener);
            isSuccess = true;
        }
        return  isSuccess;
    }

    public void unregisterListener(BDLocationListener listener){
        if(listener != null){
            client.unRegisterLocationListener(listener);
        }
    }

    /***
     *
     * @param option
     * @return isSuccessSetOption
     */
    public boolean setLocationOption(LocationClientOption option){
        boolean isSuccess = false;
        if(option != null){
            if(client.isStarted())
                client.stop();
            DIYOption = option;
            client.setLocOption(option);
            isSuccess = true;
        }
        return isSuccess;
    }

    public LocationClientOption getOption(){
        return DIYOption;
    }
    /***
     *
     * @return DefaultLocationClientOption
     */
    public LocationClientOption getDefaultLocationClientOption(){
        if(mOption == null){
            mOption = new LocationClientOption();
            mOption.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
            mOption.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系，如果配合百度地图使用，建议设置为bd09ll;
            mOption.setScanSpan(0);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
            mOption.setOpenGps(true);
            mOption.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
            mOption.setIsNeedLocationDescribe(true);//可选，设置是否需要地址描述
            mOption.setNeedDeviceDirect(false);//可选，设置是否需要设备方向结果
            mOption.setLocationNotify(false);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
            mOption.setIgnoreKillProcess(true);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
            mOption.setIsNeedLocationDescribe(false);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
            mOption.setIsNeedLocationPoiList(false);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
            mOption.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        }
        return mOption;
    }

    public void start(){
        acquireWakeLock();
        synchronized (objLock) {
            if(client != null && !client.isStarted()){
                client.start();
            }
        }
    }
    public void stop(){
        releaseWakeLock();
        synchronized (objLock) {
            if(client != null && client.isStarted()){
                client.stop();
            }
        }
    }

    private void acquireWakeLock() {
        if(null == wakeLock){
            PowerManager powerManager = (PowerManager) locationContext.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK |
                    PowerManager.ON_AFTER_RELEASE,getClass().getCanonicalName());
            if(null != wakeLock) {
                wakeLock.acquire();
            }
        }
    }

    private void releaseWakeLock() {
        if (null != wakeLock && wakeLock.isHeld()) {
            wakeLock.release();
            wakeLock = null;
        }
    }
}