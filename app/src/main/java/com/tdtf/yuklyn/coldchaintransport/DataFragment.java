package com.tdtf.yuklyn.coldchaintransport;

/**
 * Created by yuklyn on 2016/4/17.
 */

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import com.tdtf.yuklyn.coldchaintransport.helper.DatabaseHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

/**
 *
 */
public class DataFragment extends Fragment{
    private String downloadURL;

    private MainActivity mainActivity;

    private SwipeRefreshLayout swipeRefreshLayout;
    private WebView webView;
    private View rootView;

    private SharedPreferences preferences;

    private StringBuilder downloadData;
    private Handler handler;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

         handler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                if (msg.what == CONSTANT.SUCCESS){
                    webView.loadDataWithBaseURL(null, downloadData.toString(), "text/plain", "utf-8", null);
                }
                if (msg.what == CONSTANT.FAILED){
                    Toast.makeText(mainActivity,getString(R.string.download_error),Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (msg.what == CONSTANT.START_DOWNLOAD){
                    mainActivity.setFragment.refreshDeviceList();
                    mainActivity.setFragment.setCurrentDeviceTextViewText();
                }
            }
        };

        mainActivity = (MainActivity) getActivity();
        preferences = mainActivity.getSharedPreferences(CONSTANT.SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE);
        downloadData = new StringBuilder();
    }
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if(mainActivity.menu != null){
            mainActivity.menu.getItem(0).setVisible(false);
            mainActivity.menu.getItem(1).setVisible(true);
        }
        if(webView == null){
            rootView = inflater.inflate(R.layout.fragment_data_layout,null);

            mainActivity.dataFragment = this;
            swipeRefreshLayout = (SwipeRefreshLayout)rootView.findViewById(R.id.swipeRefreshLayout);
            swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    swipeRefreshLayout.setRefreshing(true);
                    sendTime();
                }
            });
            /**
             * 网页视图实现
             * 下拉刷新
             */
            webView = (WebView)rootView.findViewById(R.id.webView);
            webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
            webView.requestFocus();
            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    // TODO Auto-generated method stub
                    if (newProgress == 100)
                        swipeRefreshLayout.setRefreshing(false);
                    else
                        swipeRefreshLayout.setRefreshing(true);
                    super.onProgressChanged(view, newProgress);
                }
            });

            String test = "\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n              请下拉刷新数据";
            webView.loadDataWithBaseURL(null, test, "text/plain", "utf-8", null);
            //sendTime();
        }

        ViewGroup parent = (ViewGroup) rootView.getParent();
        if (parent != null)
        {
            parent.removeView(rootView);
        }
        return rootView;
    }

    private void sendTime(){
        new myAsyncTask().execute(CONSTANT.IP,
                preferences.getString(CONSTANT.CURRENT_DEVICE_SID,getString(R.string.unknown_device)),
                preferences.getString(CONSTANT.START_TIME,null),
                preferences.getString(CONSTANT.CURRENT_TIME,null));
    }

    /**
     * 下载服务器数据
     */
    private void downloadData(){
        try{
            downloadData.delete(0,downloadData.length());
            URL dataURL = new URL(downloadURL);
            InputStream inputStream = dataURL.openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String name;
            name = reader.readLine();
            if(name == null){
                handler.sendEmptyMessage(CONSTANT.FAILED);
                return;
            }
            DatabaseHelper databaseHelper = ((LocationApplication)mainActivity.getApplication()).databaseHelper;
            databaseHelper.getReadableDatabase().execSQL("update " + getString(R.string.table_devices) + " set name='" +
                    name + "' where SID=" + preferences.getString(CONSTANT.CURRENT_DEVICE_SID,getString(R.string.unknown_device)));

            SharedPreferences.Editor editor = preferences.edit();
            editor.apply();
            editor.putString(CONSTANT.CURRENT_DEVICE_NAME,name);
            editor.apply();
            if(mainActivity.setFragment != null){
                handler.sendEmptyMessage(CONSTANT.START_DOWNLOAD);
            }

            String line;
            line = name.substring(1,name.length());
            downloadData.append(line);
            downloadData.append("\n");
            downloadData.append(preferences.getString(CONSTANT.CURRENT_DEVICE_SID,getString(R.string.unknown_device)));
            downloadData.append("\n");
            while ((line = reader.readLine()) != null){
                downloadData.append(line);
                downloadData.append("\n");
            }
            inputStream.close();
            downloadData.append(getString(R.string.hint_start_location));
            downloadData.append(preferences.getString(CONSTANT.START_LOCATION,
                    getString(R.string.location_not_begin)));
            downloadData.append("\n");
            downloadData.append(getString(R.string.hint_current_location));
            downloadData.append(preferences.getString(CONSTANT.CURRENT_LOCATION,
                    getString(R.string.location_not_begin)));
            downloadData.append("\n\n\n\n");
            handler.sendEmptyMessage(CONSTANT.SUCCESS);
        }catch (MalformedURLException me){
            handler.sendEmptyMessage(CONSTANT.FAILED);
        }catch (IOException e){
            e.printStackTrace();
            handler.sendEmptyMessage(CONSTANT.FAILED);
        }
    }

    public void sendPrintInfo(){
        Intent intent = new Intent(mainActivity,BluetoothActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("printInfo",downloadData.toString());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private class myAsyncTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String... paras) {
            if (paras[1] != null && paras[2] != null && paras[3] != null) {
                String code = getString(R.string.correspond_start) +
                        paras[1] + "," +
                        paras[2] + "," +
                        paras[3] + CONSTANT.KEY;

                SocketChannel socketChannel = null;
                try {
                    Charset charset = Charset.forName("GBK");
                    Selector selector;
                    socketChannel = SocketChannel.open();
                    socketChannel.configureBlocking(false);
                    selector = Selector.open();
                    socketChannel.connect(new InetSocketAddress(paras[0], CONSTANT.PORT));
                    socketChannel.register(selector, SelectionKey.OP_CONNECT);
                    long startTime = System.currentTimeMillis();
                    long currentTime;
                    String content = "";
                    while (startTime != 0) {
                        currentTime = System.currentTimeMillis();
                        if (currentTime - startTime >= CONSTANT.TIMEOUT)
                            break;
                        if (content.contains(getString(R.string.correspond_end)))
                            break;
                        selector.select(1000);
                        Iterator iterator = selector.selectedKeys().iterator();
                        while (iterator.hasNext()) {
                            SelectionKey key = (SelectionKey) iterator.next();
                            iterator.remove();
                            if (key.isConnectable()) {
                                SocketChannel channel = (SocketChannel) key.channel();
                                if (channel.isConnectionPending()) {
                                    channel.finishConnect();
                                }
                                channel.configureBlocking(false);
                                channel.write(ByteBuffer.wrap((getString(R.string.correspond_start) +
                                        paras[1] + "," +
                                        paras[2] + "," +
                                        paras[3] + "," +
                                        md5(code) +
                                        getString(R.string.correspond_end)).getBytes("GBK")));
                                System.out.println("向服务器发送参数： " +
                                        getString(R.string.correspond_start) +
                                        paras[1] + "," +
                                        paras[2] + "," +
                                        paras[3] + "," +
                                        md5(code) +
                                        getString(R.string.correspond_end));
                                channel.register(selector, SelectionKey.OP_READ);
                            } else if (key.isReadable()) {
                                SocketChannel channel2 = (SocketChannel) key.channel();
                                channel2.configureBlocking(false);
                                ByteBuffer buffer = ByteBuffer.allocate(1024);
                                while (channel2.read(buffer) > 0) {
                                    channel2.read(buffer);
                                    buffer.flip();
                                    content += charset.decode(buffer);
                                }
                                channel2.register(selector, SelectionKey.OP_READ);
                            }
                        }
                    }
                    if (!content.contains(getString(R.string.correspond_end))) {
                        socketChannel.close();
                        return CONSTANT.ERROR_TIMEOUT;
                    }
                    if(content.contains("KEYERROR")){
                        socketChannel.close();
                        return CONSTANT.ERROR_READ;
                    }
                    if (content.contains(getString(R.string.response_SID_valid))) {
                        String[] infoTemp = content.split(",");
                        String[] info = infoTemp[1].split("#");
                        downloadURL = info[0];
                        socketChannel.close();
                        return CONSTANT.VALID;
                    }
                    else {
                        socketChannel.close();
                        return CONSTANT.INVALID;
                    }
                } catch (SocketTimeoutException se) {
                    try {
                        if(socketChannel != null)
                            socketChannel.close();
                    }catch (IOException e){
                        handler.sendEmptyMessage(CONSTANT.FAILED);
                    }
                    return CONSTANT.ERROR_TIMEOUT;
                } catch (SocketException se) {
                    try {
                        if(socketChannel != null)
                            socketChannel.close();
                    }catch (IOException e){
                        handler.sendEmptyMessage(CONSTANT.FAILED);
                    }
                    return CONSTANT.ERROR_SE;
                } catch (IOException ie) {
                    try {
                        if(socketChannel != null)
                            socketChannel.close();
                    }catch (IOException e){
                        handler.sendEmptyMessage(CONSTANT.FAILED);
                    }
                    return CONSTANT.ERROR_IOE;
                }
            }
            return CONSTANT.EMPTY;
        }
        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case CONSTANT.INVALID:
                    Toast.makeText(mainActivity,R.string.data_error, Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                    break;
                case CONSTANT.VALID:
                    Toast.makeText(mainActivity,getString(R.string.download_data),Toast.LENGTH_SHORT).show();
                    final long startTime = System.currentTimeMillis();
                    new Thread(){
                        public void run(){
                            long currentTime = System.currentTimeMillis();
                            while (currentTime - startTime <= CONSTANT.DOWNLOADWAITTIME)
                                currentTime = System.currentTimeMillis();
                            downloadData();
                        }
                    }.start();
                    break;
                case CONSTANT.EMPTY:
                    Toast.makeText(mainActivity,getString(R.string.location_not_begin),Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                    break;
                case CONSTANT.ERROR_READ:
                    Toast.makeText(mainActivity,getString(R.string.get_server_info_error),Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                    break;
                case CONSTANT.ERROR_TIMEOUT:
                    Toast.makeText(mainActivity,getString(R.string.get_server_timeout_error),Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                    break;
                case CONSTANT.ERROR_SE:
                    Toast.makeText(mainActivity,getString(R.string.get_server_info_error),Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                    break;
                case CONSTANT.ERROR_IOE:
                    Toast.makeText(mainActivity,getString(R.string.get_server_info_error),Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                    break;
            }
        }
    }

    public static String md5(String string) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Huh, MD5 should be supported?", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Huh, UTF-8 should be supported?", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }
}
