package com.rookied.kafka.binlog;

import com.google.code.or.OpenReplicator;
import com.google.code.or.binlog.BinlogEventListener;
import com.google.code.or.binlog.BinlogEventV4;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * @desciption:
 * @author: Demon
 * @version: 1.0 2019-04-09 17:26
 **/
public class openReplicator {
    final static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) throws Exception {
        OpenReplicator or = new OpenReplicator();
        or.setUser("root");
        or.setPassword("root");
        or.setHost("localhost");
        or.setPort(3306);
        or.setServerId(6789);
        or.setBinlogPosition(4);
        or.setBinlogFileName("binlog.000003");
        or.setBinlogEventListener(new BinlogEventListener() {
            @Override
            public void onEvents(BinlogEventV4 event) {
                // your code goes here
            }
        });
        or.start();

        System.out.println("press 'q' to stop");

        for(String line = br.readLine(); line != null; line = br.readLine()) {
            if (line.equals("q")) {
                //or.stop();
                break;
            }
        }
    }
}
