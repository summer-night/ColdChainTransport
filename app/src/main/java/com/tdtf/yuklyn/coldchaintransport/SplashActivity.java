package com.tdtf.yuklyn.coldchaintransport;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.tdtf.yuklyn.coldchaintransport.helper.NetWorkHelper;

/**
 * Created by YukLyn on 2016/4/19.
 */
public class SplashActivity extends Activity {
    private NetWorkHelper netWorkHelper;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_layout);

        netWorkHelper = ((LocationApplication)getApplication()).netWorkHelper;
        preferences = getSharedPreferences(CONSTANT.SHARED_PREFERENCE_NAME,MODE_PRIVATE);

        new myAsyncTask() {
        }.execute(CONSTANT.IP);
    }

    /**
     * 判断当前是否有网络，若无网络则不能进入程序
     * @return
     * CONNECTED 有网络
     * UNCONNECTED 无网络
     */
    private int networkState() {
        ConnectivityManager manager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if(info != null)
            return CONSTANT.CONNECTED;
        return CONSTANT.UNCONNECTED;
    }

    /**
     * 连接网络，上传SID号
     * 返回公司名
     */
    private class myAsyncTask extends AsyncTask<String, Void, Integer>{
        @Override
        protected Integer doInBackground(String... urls) {
            long startTime = System.currentTimeMillis();
            int result;
            result = networkState();
            if(CONSTANT.UNCONNECTED == result)
            {
                long loadingTime = System.currentTimeMillis() - startTime;
                if (loadingTime < CONSTANT.SHOW_TIME_MIN) {
                    try {
                        Thread.sleep(CONSTANT.SHOW_TIME_MIN - loadingTime);
                    } catch (InterruptedException e) {
                        Toast.makeText(SplashActivity.this,e.toString(),Toast.LENGTH_SHORT).show();
                    }
                }
                return result;
            }
            String lastSID = preferences.getString(CONSTANT.CURRENT_DEVICE_SID,null);
            result = netWorkHelper.deviceState(lastSID,urls[0],CONSTANT.PORT);
            long loadingTime = System.currentTimeMillis() - startTime;
            if (loadingTime < CONSTANT.SHOW_TIME_MIN) {
                try {
                    Thread.sleep(CONSTANT.SHOW_TIME_MIN - loadingTime);
                } catch (InterruptedException e) {
                    Toast.makeText(SplashActivity.this,e.toString(),Toast.LENGTH_SHORT).show();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result){
                case CONSTANT.UNCONNECTED:
                    AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this).
                            setTitle(R.string.app_name).
                            setIcon(R.drawable.ic_transport).
                            setMessage(R.string.no_connection);
                    builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            SplashActivity.this.finish();
                        }
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                    break;
                case CONSTANT.INVALID:
                    Toast.makeText(SplashActivity.this,getString(R.string.SID_invalid),Toast.LENGTH_SHORT).show();
                    Intent intent_invalid = new Intent(SplashActivity.this, LoginActivity.class);
                    intent_invalid.putExtra(CONSTANT.INTENT_ACTIVITY,CONSTANT.INTENT_SPLASHACTIVITY_TO_LOGIN);
                    startActivity(intent_invalid);
                    finish();
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    break;
                case CONSTANT.EMPTY:
                    Intent intent_empty = new Intent(SplashActivity.this, LoginActivity.class);
                    intent_empty.putExtra(CONSTANT.INTENT_ACTIVITY,CONSTANT.INTENT_SPLASHACTIVITY_TO_LOGIN);
                    startActivity(intent_empty);
                    finish();
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    break;
                case CONSTANT.VALID:
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    break;
                case CONSTANT.ERROR_READ:
                    Toast.makeText(SplashActivity.this,getString(R.string.get_server_info_error),Toast.LENGTH_SHORT).show();
                    break;
                case CONSTANT.ERROR_TIMEOUT:
                    Toast.makeText(SplashActivity.this,getString(R.string.get_server_timeout_error),Toast.LENGTH_SHORT).show();
                    break;
                case CONSTANT.ERROR_SE:
                    Toast.makeText(SplashActivity.this,getString(R.string.get_server_info_error),Toast.LENGTH_SHORT).show();
                    break;
                case CONSTANT.ERROR_IOE:
                    Toast.makeText(SplashActivity.this,getString(R.string.get_server_info_error),Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}


