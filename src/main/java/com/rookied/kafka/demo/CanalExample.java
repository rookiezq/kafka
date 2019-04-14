package com.rookied.kafka.demo;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry.*;
import com.alibaba.otter.canal.protocol.Message;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * @desciption: Canal客户端
 * 实时监控mysql数据库的变化，对binlog的增量信息进行解析并放入kafka
 * @author: Demon
 * @version: 1.0 2019-04-09 16:49
 **/
@SuppressWarnings("all")
public class CanalExample {
    //用来获取binlog解析到的数据，因为从下面的逻辑是逐步获取，所以需要拼接
    static StringBuffer sb = null;
    //主函数传入topic和kafka地址这两个参数
    public static void main(String args[]) {
        //kafka生产者
        MyProducer myProducer = new MyProducer(args[0],args[1]);
        // 创建链接
        CanalConnector connector = CanalConnectors.newSingleConnector(new InetSocketAddress(args[1],
                11111), "example", "", "");
        int batchSize = 1000;
        int emptyCount = 0;
        try {
            connector.connect();
            connector.subscribe(".*\\..*");
            connector.rollback();
            //如果120s数据没有变化就停止
            int totalEmptyCount = 120;
            while (emptyCount < totalEmptyCount) {
                // 获取指定数量的数据
                Message message = connector.getWithoutAck(batchSize);
                long batchId = message.getId();
                int size = message.getEntries().size();
                //判断是否获取到数据
                if (batchId == -1 || size == 0) {
                    emptyCount++;
                    System.out.println("empty count : " + emptyCount);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                } else {
                    emptyCount = 0;
                    // System.out.printf("message[batchId=%s,size=%s] \n", batchId, size);
                    //解析message中的entries
                    printEntry(message.getEntries());
                    System.out.println(sb);
                    //将解析好的数据交给生产者
                    myProducer.produce(sb.toString());
                }

                connector.ack(batchId); // 提交确认
                // connector.rollback(batchId); // 处理失败, 回滚数据
            }

            System.out.println("empty too many times, exit");
        } finally {
            connector.disconnect();
            myProducer.producer.close();
        }
    }

    /**
     * 解析获取到的数据
     * @param entrys
     */
    private static void printEntry(List<Entry> entrys) {
        sb = new StringBuffer();
        for (Entry entry : entrys) {
            if (entry.getEntryType() == EntryType.TRANSACTIONBEGIN || entry.getEntryType() == EntryType.TRANSACTIONEND) {
                continue;
            }

            RowChange rowChage = null;
            try {
                rowChage = RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new RuntimeException("ERROR ## parser of eromanga-event has an error , data:" + entry.toString(),
                        e);
            }

            EventType eventType = rowChage.getEventType();
            //获取头信息 比如数据库名、表名
            String value1 = String.format("================&gt; binlog[%s:%s] , name[%s,%s] , eventType : %s",
                    entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),
                    entry.getHeader().getSchemaName(), entry.getHeader().getTableName(),
                    eventType);
            //System.out.println(value1);
            sb.append(value1+"\n");
            for (RowData rowData : rowChage.getRowDatasList()) {
                //判断增删改
                if (eventType == EventType.DELETE) {
                    printColumn(rowData.getBeforeColumnsList());
                } else if (eventType == EventType.INSERT) {
                    printColumn(rowData.getAfterColumnsList());
                } else {
                    //System.out.println("-------&gt; before");
                    sb.append("-------&gt; before\n");
                    printColumn(rowData.getBeforeColumnsList());
                    //System.out.println("-------&gt; after");
                    sb.append("-------&gt; after\n");
                    printColumn(rowData.getAfterColumnsList());
                }
            }
        }
    }

    //打印数据更改前后变化
    private static void printColumn(List<Column> columns) {
        for (Column column : columns) {
            String value2 = column.getName() + " : " + column.getValue() + "    update=" + column.getUpdated();
            //System.out.println(value2);
            sb.append(value2+"\n");
        }
    }
}
