package com.tdtf.yuklyn.coldchaintransport.helper;

import android.content.Context;

import com.tdtf.yuklyn.coldchaintransport.CONSTANT;
import com.tdtf.yuklyn.coldchaintransport.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Created by YukLyn on 2016/4/25.
 * 用于判断设备SID是否合法
 */
public class NetWorkHelper {
    private Context context;
    public NetWorkHelper(Context context){
        this.context = context;
    }

    /**
     * 判断SID是否有效
     */
    public int deviceState(String SID,String IP,int PORT){
        String response;
        if(SID != null){
            try{
                Socket socket = new Socket();
                SocketAddress socAddress = new InetSocketAddress(IP,PORT);
                socket.connect(socAddress, CONSTANT.TIMEOUT);
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write((context.getString(R.string.correspond_start) + SID + context.getString(R.string.correspond_end)).getBytes("utf-8"));
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                char[] buffer = new char[1024];

                try{
                    long startTime = System.currentTimeMillis();
                    long currentTime;
                    while (true){
                        currentTime = System.currentTimeMillis();
                        if (currentTime - startTime >= CONSTANT.TIMEOUT)
                            break;
                        reader.read(buffer,0,1024);
                        if (String.valueOf(buffer).contains(context.getString(R.string.correspond_end)))
                            break;
                    }

                    response = String.valueOf(buffer);

                    if(!response.contains(context.getString(R.string.correspond_end)))
                        return CONSTANT.ERROR_TIMEOUT;
                    if(response.contains(context.getString(R.string.response_SID_valid))) {
                        if(socket.isConnected()){
                            socket.close();
                        }
                        return CONSTANT.VALID;
                    }
                    else{
                        if(socket.isConnected()){
                            socket.close();
                        }
                        return CONSTANT.INVALID;
                    }
                }catch (IOException e){
                    if(socket.isConnected()){
                        socket.close();
                    }
                    return CONSTANT.ERROR_READ;
                }
            }
            catch (SocketTimeoutException se){
                return CONSTANT.ERROR_TIMEOUT;
            }
            catch (SocketException se){
                return CONSTANT.ERROR_SE;
            }
            catch (IOException ie){
                return CONSTANT.ERROR_IOE;
            }
        }
        return CONSTANT.EMPTY;
    }
}
