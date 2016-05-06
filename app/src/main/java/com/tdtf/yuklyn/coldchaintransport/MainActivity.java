package com.tdtf.yuklyn.coldchaintransport;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTabHost;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.tdtf.yuklyn.coldchaintransport.helper.DatabaseHelper;

import java.util.ArrayList;

/**
 * Created by yuklyn on 2016/4/13.
 */
public class MainActivity extends AppCompatActivity {
    private static final int SDK_PERMISSION_REQUEST = 127;

    private LayoutInflater layoutInflater;
    private Class fragmentArray[] = {LocationFragment.class,DataFragment.class,SetFragment.class};
    private int tabImageArray[] = {R.drawable.tab_locate,R.drawable.tab_data,R.drawable.tab_set};
    private String tabTextArray[];
    private FragmentTabHost mTabHost;
    public DatabaseHelper databaseHelper;

    public Menu menu;

    public LocationFragment locationFragment;
    public DataFragment dataFragment;
    public SetFragment setFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar)findViewById(R.id.activity_main_toolbar);
        setSupportActionBar(toolbar);

        databaseHelper = ((LocationApplication)getApplication()).databaseHelper;

        initView();

        getPermission();
    }
    @Override
    protected void onDestroy(){
        if(databaseHelper != null){
            databaseHelper.close();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        menu.getItem(2).setVisible(false);
        this.menu = menu;
        if(mTabHost.getCurrentTab() == 0){
            menu.getItem(0).setVisible(true);
            menu.getItem(1).setVisible(false);
        }
        else if(mTabHost.getCurrentTab() == 1){
            menu.getItem(1).setVisible(true);
            menu.getItem(1).setVisible(false);
        }
        else {
            menu.getItem(0).setVisible(false);
            menu.getItem(1).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.btn_clear:
                AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.app_name).setIcon(R.drawable.ic_transport).setMessage(R.string.dialog_delete_info);
                builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        locationFragment.clearLocationInfo();
                        clearSharePreferenceInfo();
                    }
                }).setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                return true;
            case R.id.btn_print:
                dataFragment.sendPrintInfo();
                return true;
        }
        return false;
    }

    /**
     * 获取权限
     */
    @TargetApi(23)
    private void getPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayList<String> permissions = new ArrayList<>();
            /***
             * 定位权限为必须权限，用户如果禁止，则每次进入都会申请
             */
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            if (permissions.size() > 0) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), SDK_PERMISSION_REQUEST);
            }
        }
    }

    /**
     * 初始化主界面
     */
    private void initView(){
        tabTextArray = getResources().getStringArray(R.array.array_tab_text);

        layoutInflater = LayoutInflater.from(MainActivity.this);
        mTabHost = (FragmentTabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup(MainActivity.this,getSupportFragmentManager(),R.id.fragment_container);
        int count = fragmentArray.length;

        for(int i = 0; i < count; i++){
            TabHost.TabSpec tabSpec = mTabHost.newTabSpec(tabTextArray[i]).setIndicator(getTabItemView(i));
            mTabHost.addTab(tabSpec, fragmentArray[i], null);
        }
    }

    /**
     * 给Tab按钮设置图标和文字
     */
    private View getTabItemView(int index){
        View view = layoutInflater.inflate(R.layout.item_tab,null);

        ImageView imageView = (ImageView) view.findViewById(R.id.tab_item_img);
        imageView.setImageResource(tabImageArray[index]);

        TextView textView = (TextView) view.findViewById(R.id.tab_item_text);
        textView.setText(tabTextArray[index]);

        return view;
    }
    /**
     * 退出提示对话框
     */
    @Override
    public boolean onKeyDown(int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this).setTitle(R.string.app_name).setIcon(R.drawable.ic_transport).setMessage(R.string.dialog_info);
            builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.this.finish();
                }
            }).setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            return true;
        }
        return false;
    }

    /**
     * 清除SharePreference中：开始位置，开始时间，当前位置，当前时间
     */
    public void clearSharePreferenceInfo(){
        SharedPreferences preferences = getSharedPreferences(CONSTANT.SHARED_PREFERENCE_NAME,MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(CONSTANT.START_LOCATION);
        editor.remove(CONSTANT.START_TIME);
        editor.remove(CONSTANT.CURRENT_LOCATION);
        editor.remove(CONSTANT.CURRENT_TIME);
        editor.apply();
    }
}
