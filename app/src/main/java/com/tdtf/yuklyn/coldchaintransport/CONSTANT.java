package com.tdtf.yuklyn.coldchaintransport;

/**
 * Created by Yuklyn on 2016/4/25.
 */
public class CONSTANT {
    /**
     * splashActivity
     */
    public static final int SHOW_TIME_MIN = 1000;
    /**
     * 网络用常量
     */
    public static final int UNCONNECTED = 0;
    public static final int CONNECTED = 1;
    public static final int VALID = 2;
    public static final int INVALID = 3;
    public static final int EMPTY = 4;
    public static final int ERROR_TIMEOUT = 5;
    public static final int ERROR_READ = 6;
    public static final int ERROR_IOE = 7;
    public static final int ERROR_SE = 8;

    public static final String IP = "115.28.176.10";
    public static final int PORT = 7108;
    //public static final String IP = "192.168.1.106";
    public static final int TIMEOUT = 10000;
    public static final int DOWNLOADWAITTIME = 2000;
    public static final String KEY = "wskls@key369";

    public static final int START_DOWNLOAD = 20;

    public static final int SID_LENGTH = 15;

    /**
     * 打印用常量，下载用常量
     */
    public static final int SUCCESS = 10;
    public static final int FAILED = 11;

    /**
     * 定位服务LocationService用常量
     */
    public static final int DURATION_LOCATE = 10000;
    public static final int DURATION_UPLOAD = 300000;
    public static final int DURATION_STOP = 5000;

    public static final String BROADCAST_LOCATION_SERVICE_TO_FRAGMENT = "Location information from Location Service to Location Fragment.";

    /**
     *Intent用常量
     */
    public static final String INTENT_ACTIVITY = "Activity";
    public static final String INTENT_SPLASHACTIVITY_TO_LOGIN = "SplashActivity";
    public static final String INTENT_SETFRAGMENT_TO_LOGIN = "SetFragment";
    public static final String INTENT_DEVICE_CHANGE = "Device changed";

    /**
     * SharedPreferences
     * currentSID
     * currentName
     * currentLocation
     * currentTime
     * startLocation
     * startTime
     */
    public static final String SHARED_PREFERENCE_NAME = "ColdChainTransport";
    public static final String START_LOCATION = "StartLocation";
    public static final String START_TIME = "StartTime";
    public static final String CURRENT_LOCATION = "CurrentLocation";
    public static final String CURRENT_TIME = "CurrentTime";
    public static final String CURRENT_DEVICE_SID = "CurrentSID";
    public static final String CURRENT_DEVICE_NAME = "CurrentName";

}
