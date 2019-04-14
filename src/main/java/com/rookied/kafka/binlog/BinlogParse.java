package com.rookied.kafka.binlog;

import com.github.shyiko.mysql.binlog.BinaryLogClient;
import com.github.shyiko.mysql.binlog.BinaryLogFileReader;
import com.github.shyiko.mysql.binlog.event.*;
import com.github.shyiko.mysql.binlog.event.deserialization.EventDeserializer;

import java.io.File;
import java.io.IOException;

/**
 * @desciption:
 * @author: Demon
 * @version: 1.0 2019-04-09 14:01
 **/
@SuppressWarnings("all")
public class BinlogParse {
    public static void readFromMysql() throws Exception {
        BinaryLogClient client = new BinaryLogClient("localhost", 3306, "root", "root");
        EventDeserializer eventDeserializer = new EventDeserializer();
        /*eventDeserializer.setEventDataDeserializer(EventType.EXT_DELETE_ROWS,
                new ByteArrayEventDataDeserializer());*/
        eventDeserializer.setCompatibilityMode(
                EventDeserializer.CompatibilityMode.DATE_AND_TIME_AS_LONG,
                EventDeserializer.CompatibilityMode.CHAR_AND_BINARY_AS_BYTE_ARRAY
        );
        client.setEventDeserializer(eventDeserializer);
        client.registerEventListener(new BinaryLogClient.EventListener() {

            @Override
            public void onEvent(Event event) {
                EventData data = event.getData();
                if (data instanceof QueryEventData){
                    System.out.println(((QueryEventData)data).getSql());
                }else if (data instanceof TableMapEventData){
                    System.out.println("收到数据库DDL操作："+((TableMapEventData)data).getTable());
                }else if (data instanceof UpdateRowsEventData){
                    System.out.println("收到数据库DML操作update："+(data.toString()));
                }else if (data instanceof WriteRowsEventData){
                    System.out.println("收到数据库DML操作insert："+(data.toString()));
                }else if (data instanceof DeleteRowsEventData){
                    System.out.println("收到数据库DML操作delete："+(data.toString()));
                }
            }
        });
        client.connect();
    }

    public static void readFromLocal() throws IOException {
        File binlogFile = new File("E:\\MySql\\binlog\\binlog.000003");
        EventDeserializer eventDeserializer = new EventDeserializer();
        //eventDeserializer.setEventDataDeserializer(EventType.EXT_DELETE_ROWS,new ByteArrayEventDataDeserializer());
        eventDeserializer.setCompatibilityMode(
                EventDeserializer.CompatibilityMode.DATE_AND_TIME_AS_LONG,
                EventDeserializer.CompatibilityMode.CHAR_AND_BINARY_AS_BYTE_ARRAY
        );
        BinaryLogFileReader reader = new BinaryLogFileReader(binlogFile, eventDeserializer);
        try {
            for (Event event; (event = reader.readEvent()) != null; ) {
                EventData data = event.getData();
                if (data instanceof QueryEventData){
                    System.out.println(((QueryEventData)data).getSql());
                }else if (data instanceof TableMapEventData){
                    System.out.println("收到数据库DDL操作："+((TableMapEventData)data).getTable());
                }else if (data instanceof UpdateRowsEventData){
                    System.out.println("收到数据库DML操作update："+(data.toString()));
                }else if (data instanceof WriteRowsEventData){
                    System.out.println("收到数据库DML操作insert："+(data.toString()));
                }else if (data instanceof DeleteRowsEventData){
                    System.out.println("收到数据库DML操作delete："+(((DeleteRowsEventData) data).getRows()));
                }
            }
        } finally {
            reader.close();
        }
    }

    public static void main(String[] args) throws Exception {
        //readFromLocal();
        readFromMysql();
    }
}
