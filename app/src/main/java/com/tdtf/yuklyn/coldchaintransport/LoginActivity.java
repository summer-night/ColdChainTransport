package com.tdtf.yuklyn.coldchaintransport;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.tdtf.yuklyn.coldchaintransport.helper.DatabaseHelper;
import com.tdtf.yuklyn.coldchaintransport.helper.NetWorkHelper;

/**
 * Created by YukLyn on 2016/4/12.
 */
public class LoginActivity extends AppCompatActivity {
    private String inputSID;
    private EditText et_SID;
    private ProgressBar pb_Login;
    private Button btn_Login;
    private DatabaseHelper databaseHelper;
    private NetWorkHelper netWorkHelper;
    private SharedPreferences preferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar)findViewById(R.id.activity_login_toolbar);
        setSupportActionBar(toolbar);
        if(getIntent().getStringExtra(CONSTANT.INTENT_ACTIVITY).equals(CONSTANT.INTENT_SETFRAGMENT_TO_LOGIN)) {
            ActionBar actionBar = getSupportActionBar();
            if(actionBar != null)
                actionBar.setDisplayHomeAsUpEnabled(true);
        }
        netWorkHelper = ((LocationApplication)getApplication()).netWorkHelper;
        databaseHelper = ((LocationApplication)getApplication()).databaseHelper;
        preferences = getSharedPreferences(CONSTANT.SHARED_PREFERENCE_NAME,MODE_PRIVATE);

        et_SID = (EditText)findViewById(R.id.et_SID);
        pb_Login = (ProgressBar)findViewById(R.id.pb_login);
        btn_Login = (Button)findViewById(R.id.btn_login);
        btn_Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (et_SID.getText().toString().length() != CONSTANT.SID_LENGTH) {
                    Toast.makeText(LoginActivity.this, R.string.please_input_correct_SID, Toast.LENGTH_SHORT).show();
                }
                else {
                    btn_Login.setVisibility(View.INVISIBLE);
                    pb_Login.setVisibility(View.VISIBLE);
                    inputSID = et_SID.getText().toString();

                    new MyAsyncTask() {
                    }.execute(CONSTANT.IP);
                }
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(databaseHelper != null){
            databaseHelper.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.getItem(0).setVisible(false);
        menu.getItem(1).setVisible(false);
        menu.getItem(2).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

    private class MyAsyncTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... urls) {
            return netWorkHelper.deviceState(inputSID,urls[0],CONSTANT.PORT);
        }

        @Override
        protected void onPostExecute(Integer result) {
            btn_Login.setVisibility(View.VISIBLE);
            pb_Login.setVisibility(View.INVISIBLE);
            switch (result) {
                case CONSTANT.INVALID:
                    Toast.makeText(LoginActivity.this, R.string.cannot_find_device, Toast.LENGTH_SHORT).show();
                    break;
                case CONSTANT.VALID:
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.apply();
                    editor.putString(CONSTANT.CURRENT_DEVICE_SID,inputSID);
                    editor.putString(CONSTANT.CURRENT_DEVICE_NAME,getString(R.string.unknown_device));
                    editor.apply();

                    /**每次登陆如果数据库没有此SID，则将此SID存到数据库*/
                    Cursor cursor = databaseHelper.getReadableDatabase().query(getString(R.string.table_devices),
                            new String[]{"SID"}, "SID like ?", new String[]{inputSID}, null, null, null);
                    if (!cursor.moveToFirst()) {
                        databaseHelper.getReadableDatabase().execSQL("insert into " +
                                        getString(R.string.table_devices) +
                                        "(SID,name) values(?,?)",
                                new String[]{inputSID,getString(R.string.unknown_device)});
                        cursor.close();
                    }
                    if(getIntent().getStringExtra(CONSTANT.INTENT_ACTIVITY).equals(CONSTANT.INTENT_SETFRAGMENT_TO_LOGIN)) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this).setTitle(R.string.app_name).setIcon(R.drawable.ic_transport).setMessage(R.string.change_device_info);
                        builder.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent intent = new Intent(CONSTANT.INTENT_DEVICE_CHANGE);
                                sendBroadcast(intent);
                                finish();
                            }
                        }).setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        });
                        AlertDialog alertDialog = builder.create();
                        alertDialog.show();
                    }
                    else {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    }
                    break;
                case CONSTANT.ERROR_READ:
                    Toast.makeText(LoginActivity.this,getString(R.string.get_server_info_error),Toast.LENGTH_SHORT).show();
                    break;
                case CONSTANT.ERROR_TIMEOUT:
                    Toast.makeText(LoginActivity.this,getString(R.string.get_server_timeout_error),Toast.LENGTH_SHORT).show();
                    break;
                case CONSTANT.ERROR_SE:
                    Toast.makeText(LoginActivity.this,getString(R.string.get_server_info_error),Toast.LENGTH_SHORT).show();
                    break;
                case CONSTANT.ERROR_IOE:
                    Toast.makeText(LoginActivity.this,getString(R.string.get_server_info_error),Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}
