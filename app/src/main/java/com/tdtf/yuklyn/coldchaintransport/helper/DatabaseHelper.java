package com.tdtf.yuklyn.coldchaintransport.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by YukLyn on 2016/4/22.
 * 用于创建数据库及表
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    final String CREATE_TABLE_DEVICES = "create table " +
            "devices" +
            "(_id integer primary key autoincrement," +
            "SID varchar(20)," +
            "name varchar(50))";
    final String CREATE_TABLE_LOCATION = "create table " +
            "location" +
            "(_id integer primary key autoincrement," +
            "SID varchar(20)," +
            "time varchar(20)," +
            "location varchar(50)," +
            "longitude varchar(50)," +
            "latitude varchar(50))";

    public DatabaseHelper(Context context, String name, int version){
        super(context,name,null,version);
    }
    @Override
    public void onCreate(SQLiteDatabase database){
        database.execSQL(CREATE_TABLE_DEVICES);
        database.execSQL(CREATE_TABLE_LOCATION);
    }
    @Override
    public void onUpgrade(SQLiteDatabase database,int oldVersion,int newVersion){

    }
}
