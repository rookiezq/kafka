# 使用Canal和Kafka对Mysql binlog解析实现增量同步

## 操作环境

> 系统：windows 10 & Centos 7.6
>
> kafka：2.11-0.10.2.0 & 2.11-0.10.2.2
>
> zookeeper：3.4.13 & 3.4.14
>
> mysql：5.7.23 & 5.7.25
>
> canal：1.1.13

## 前提

linux连接我使用的是Xshell6 如果在下载的时候网速很慢可以先自行下载好再上传到服务器

**墙裂推荐**：xshell实现window上传

```bash
yum  install lrzsz
```

安装完这个就可以直接进行拖动上传，效果如图。

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412205500.gif)

## Zookeeper

### 下载

#### windows

使用这个清华的镜像 [zookeeper](https://mirrors.tuna.tsinghua.edu.cn/apache/zookeeper/zookeeper-3.4.14/)

#### linux

```bash
wget https://mirrors.tuna.tsinghua.edu.cn/apache/zookeeper/zookeeper-3.4.14/zookeeper-3.4.14.tar.gz
```

### 安装

直接解压获得

#### windows

**路径**：E:\Zookeeper\zookeeper-3.4.13

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190411154314.png)

修改E:\Zookeeper\zookeeper-3.4.13\conf\zoo_sample.cfg文件名为zoo.cfg

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190411154626.png)

#### linux

```bash
mkdir /tmp/zookeeper
tar -zxvf zookeeper-3.4.14.tar.gz -C /tmp/zookeeper/
```

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412114113.png)

### 配置

#### windows

编辑**zoo.cfg**，修改如图两行

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190411154745.png)

> **注意**：1、zookeeper文件夹先手动创建好 
>
> ​		2、如图反斜杠要两个

#### linux

在/tmp/zookeeper下创建两个文件夹

```bash
cd /tmp/zookeeper
mkdir data log
```

将**conf/zoo_sample.cfg**改名为**zoo.cfg**

```bash
mv zoo_sample.cfg zoo.cfg
```

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412115922.png)

修改**zoo.cfg**  `vi zoo.cfg`

```properties
dataDir=/tmp/zookeeper/data
dataLogDir=/tmp/zookeeper/log
```

### 启动

#### windows

进入bin目录

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190411155113.png)

成功如图

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190411155454.png)

#### linux

```bash
./zkServer.sh start
```

成功如图，和windows有一些区别

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412121503.png)

## Kafka

kafka的原理我这里就不做介绍了，~~因为我自己都不会~~，只要知道他是一个中间件就行了，它提供一个生产者和一个消费者，生产者产生数据（**相当于value**）按照topic（**相当于key**）进行存储，消费者就根据topic进行获取，kafka就在两者之间起着中间件的作用。

### 下载

#### windows

官网下载 [kafka](http://kafka.apache.org/downloads)

#### linux

```bash
wget http://mirrors.hust.edu.cn/apache/kafka/0.10.2.2/kafka_2.11-0.10.2.2.tgz
```

~~官网的下载速度感人，4k/s~~这里换了一个源，用的是10.2.2（和10.2.0没啥区别），或者可以自行在官网下载好上传到服务器上，**请看前提**

> **注意**：建议下载和我一样的版本，新的会对一些api进行重写，导致一些不可描述的错误

### 安装

#### windows

和zookeeper一样直接解压就行

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190411160823.png)

#### linux

```bash
mkdir /tmp/kafka
tar -zxvf zookeeper-3.4.14.tar.gz -C /tmp/zookeeper/
```

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412115305.png)

### 配置

#### windows

编辑config/server.properties

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190411160951.png)

由于只是本地，所以只需要修改日志路径

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190411161214.png)

> **注意**：同样需要先创建logs文件夹

#### linux

在配置文件中加入这一行

```properties
host.name=远程服务器地址
```

> **注意**：这一步很重要，否则待会会连不上

### 启动

#### windows

进入kafka目录

```bash
bin/windows/kafka-server-start.bat config/server.properties
```

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190411162956.png)

这样启动了无法进行下面操作，除非再开一个终端窗口，下面介绍一下后台启动

##### 后台启动

```bash
bin/windows/kafka-server-start.bat config/server.properties 1>/dev/null  2>&1  &
```

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190411163630.png)

这样就启动成功了。

##### 相关命令

> **注意**：1、下面的topicName为自定义，相当于**key**
>
> ​		2、如果是linux 就直接在bin目录下执行.sh相关命令

##### 创建topic

```bash
bin/windows/kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topicName
```

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190411165515.png)

##### 创建生产者

```bash
bin/windows/kafka-console-producer.bat --broker-list localhost:9092 --topic topicName
```

##### 创建消费者

```bash
bin/windows/kafka-console-consumer.bat  --bootstrap-server localhost:9092 --topic topicName --from-beginning
```

生产者消费者创建完后的**效果图**

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190411170519.gif)

##### 查看topic

```bash
bin/windows/kafka-topics.bat --list --zookeeper localhost:2181
```

##### 查看topic详细信息

```bash
bin/windows/kafka-topics.bat --describe --zookeeper localhost:2181
```

##### 删除topic

```bash
bin/windows/kafka-topics.bat  --delete --zookeeper  localhost:2181 --topic topicName
```

> **注意**：删除topic需要在server.properties里进行配置，加一行
>
> ```properties
> #删除topic
> delete.topic.enable=true
> ```

#### linux

> **注意**：如果在配置文件中配置**host.name** 下面的所有的**localhost**都改为配置的主机地址！！！！否则会出现
>
> ```ba&amp;#39;sh
> ERROR Error when sending message to topic helloworld with key: null, value: 3 bytes with error: (org.apache.kafka.clients.producer.internals.ErrorLoggingCallback)
> org.apache.kafka.common.errors.TimeoutException: Failed to update metadata after 60000 ms.
> ```

##### 启动

-daemon为后台启动

```bash
bin/kafka-server-start.sh -daemon config/server.properties
```

##### 创建topic

```bash
bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic topicName
```

##### 创建生产者

```bash
bin/kafka-console-producer.sh --broker-list localhost:9092 --topic topicName
```

##### 创建消费者

```bash
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic topicName --from-beginning
```

##### 查看topic

```bash
bin/kafka-topics.sh --list --zookeeper localhost:2181
```

##### 查看topic详细信息

```bash
bin/kafka-topics.sh --describe --zookeeper localhost:2181
```

##### 删除topic

```bash
bin/kafka-topics.sh  --delete --zookeeper  localhost:2181 --topic topicName
```



## Mysql

Mysql 的安装步骤移步我的[博客](<https://www.rookied.com/zh-CN/mysql1.html>)

## binlog

binlog就是开启后相当于是能够实时监控mysql的变化情况，对增量信息会写入binlog日志文件。

> **声明**：这里开始介绍linux上的教程，~~主要是懒不想在windows上再复现，~~ linux & windows类似，

先连接mysql看binlog有没有开启

```mysql
show variables like '%log_bin%';
```

如图显示就是没有开启

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412102442.png)

编辑mysql的配置为文件 `my.cnf`

```bash
vi /etc/my.cnf
```

在[mysqld]下加入这三行

```properties
log-bin=mysql-bin #添加这一行就ok
binlog-format=ROW #选择row模式
server_id=1 #配置mysql replaction需要定义，不能和canal的slaveId重复
```

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412102846.png)

重启数据库

```bash
systemctl restart mysqld
```

> **注意：**如果这里启动失败，先重启一下服务器 `reboot`,再重启一下数据库,~~我就是因为没有重启服务器，找了一上午的错~~，如果数据库还是启动失败，自行google

再连接一下mysql看一下binlog是否开启

```mysql
show variables like '%log_bin%';
```

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412113000.png)

这样就是启动成功。

## Canal

Canal是阿里的一个基于数据库增量日志解析开源项目，提供增量数据订阅&消费，目前主要支持了mysql

canal的原理是基于mysql binlog技术，所以这里一定需要开启mysql的binlog写入功能，建议配置binlog模式为row，如何开启请看上文，其他原理可以参考canal的[github](<https://github.com/alibaba/canal>)

> **声明**：请看**binlog**开头的**声明**

### 准备

mysql为canal开启权限

```mysql
CREATE USER canal IDENTIFIED BY 'canal';  
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
FLUSH PRIVILEGES;
```

第一步结束后可能会出现下面的错误，没有请忽视

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412140313.png)

这是因为mysql不允许设置简单密码，依次执行下面的操作就行

```mysql
--密码强度检查等级
set global validate_password_policy=0;
--密码至少要包含的小写字母个数和大写字母个数。
set global validate_password_mixed_case_count=0;
--密码至少要包含的数字个数。
set global validate_password_number_count=3;
--密码至少要包含的特殊字符数。
set global validate_password_special_char_count=0;
--密码最小长度，参数默认为8，
set global validate_password_length=3;
```

完成后执行 `SHOW VARIABLES LIKE 'validate_password%';`如图显示

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412140533.png)

好了接着执行上面的为canal开启权限的操作就行了

### 下载

```bash
wget https://github.com/alibaba/canal/releases/download/canal-1.1.3/canal.deployer-1.1.3.tar.gz
```

~~速度同样感人~~，**请看前提**

### 安装

解压

```bash
mkdir /tmp/canal
tar -zxvf canal-1.1.3/canal.deployer-1.1.3.tar.gz  -C /tmp/canal
```

安装目录如下

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412143041.png)

### 配置

```bash
vi conf/example/instance.properties
```

~~其实本地单机测试不需要修改任何东西，我就是告诉一下在这里~~

### 启动

```sh
sh bin/startup.sh
```

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412143913.png)

说明启动成功

### 关闭

```shell
sh bin/stop.sh
```

### 开启端口

#### 11111

```bash
firewall-cmd --permanent --zone=public --add-port=11111/tcp
firewall-cmd --permanent --zone=public --add-port=11111/udp
firewall-cmd --reload
```

#### 9092

```bash
firewall-cmd --permanent --zone=public --add-port=9092/tcp
firewall-cmd --permanent --zone=public --add-port=9092/udp
firewall-cmd --reload
```

#### 2181

```
firewall-cmd --permanent --zone=public --add-port=2181/tcp
firewall-cmd --permanent --zone=public --add-port=2181/udp
firewall-cmd --reload
```

## java api

### pom

```xml
<!--canal-->
<dependency>
    <groupId>com.alibaba.otter</groupId>
    <artifactId>canal.client</artifactId>
    <version>1.1.0</version>
</dependency>
<!--kafka-->
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka_2.11</artifactId>
    <version>0.10.2.2</version>
</dependency>
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>0.10.2.2</version>
</dependency>
```



### Canal客户端

```java
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
```

### 生产者

```java
package com.rookied.kafka.demo;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

/**
 * @desciption: 生产者
 * @author: Demon
 * @version: 1.0 2019-04-10 10:20
 **/
public class MyProducer {
    public KafkaProducer<String, String> producer;

    private String topic;
    //public  String BOOTSTRAP_SERVERS = "localhost:9092";

    public MyProducer(String topic,String bootstrapServers) {
        this.topic = topic;
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers+":9092");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        producer = new KafkaProducer<>(props);
    }

    /**
     * 将canal传入的数据放入kafka
     * @param value 数据
     */
    public void produce(String value) {
            try {
                producer.send(new ProducerRecord<>(topic, value));
            } catch (Exception e) {
                e.printStackTrace();
            }

    }

    /*public static void main(String[] args) {
        new MyProducer().produce();
    }*/

}

```



### 消费者

消费者是为了测试用 如果有需求可以写

```java
package com.rookied.kafka.demo;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Properties;

/**
 * @desciption: 消费者
 * @author: Demon
 * @version: 1.0 2019-04-10 10:30
 **/
public class MyConsumer {
    //private static Logger log = Logger.getLogger(MyConsumer.class);

    private Consumer<String, String> consumer;

    //group_id随便取
    private static final String GROUP_ID = "0";
    //需要消费的topic
    private static final String TOPIC = "test3";
    //kafka地址
    private static final String BOOTSTRAP_SERVERS = "172.16.5.144:9092";

    private MyConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // 自动commit
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        // 自动commit的间隔
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumer = new KafkaConsumer<>(props);
    }

    private void consume() {
        // 可消费多个topic,组成一个list
        consumer.subscribe(Collections.singletonList(TOPIC));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(1000);
            for (ConsumerRecord<String, String> record : records) {
                //System.out.printf("offset = %d, key = %s, value = %s \n", record.offset(), record.key() record.value());
                //log.info(record.value());
                System.out.println(record.value());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args) {
        new MyConsumer().consume();
    }
}
```

如果遇到这样的错误

```java
java.net.ConnectException: Connection refused: no further information
    at sun.nio.ch.SocketChannelImpl.checkConnect(Native Method)
    at sun.nio.ch.SocketChannelImpl.finishConnect(SocketChannelImpl.java:739)
    at org.apache.kafka.common.network.PlaintextTransportLayer.finishConnect(PlaintextTransportLayer.java:51)
    at org.apache.kafka.common.network.KafkaChannel.finishConnect(KafkaChannel.java:73)
    at org.apache.kafka.common.network.Selector.pollSelectionKeys(Selector.java:323)
    at org.apache.kafka.common.network.Selector.poll(Selector.java:291)
    at org.apache.kafka.clients.NetworkClient.poll(NetworkClient.java:260)
    at org.apache.kafka.clients.producer.internals.Sender.run(Sender.java:236)
    at org.apache.kafka.clients.producer.internals.Sender.run(Sender.java:148)
    at java.lang.Thread.run(Thread.java:745)
```

先看上文kafka配置中linux那一步有没有做 就是在配置文件中加 `host.name=远程服务器地址`

如果还报错，就看看9092端口有没有开启

```bash
firewall-cmd --list-ports
```

如图就是开启

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412154212.png)

如果没有开启就执行

```bash
firewall-cmd --zone=public --add-port=9092/tcp --permanent
firewall-cmd --zone=public --add-port=9092/udp --permanent
firewall-cmd --reload
```

这样再查看端口就发现开启了，启动消费者不会报错了

## Maven打jar包

pom文件中的build->plugins下加如下一段

这个assembly插件的好处就是可以将这个包所需要的所有依赖都放进同一个jar包

```xml
<build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>2.5.5</version>
                <configuration>
                    <archive>
                        <manifest>
                            <!--这里指定主类-->
                            <mainClass>com.rookied.kafka.demo.CanalExample</mainClass>
                        </manifest>
                    </archive>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
</build>
```

如果项目中有多个主类，这时就需要指定一个主类进行执行，如上文<mainClass>中的与下图对应

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412191907.png)

直接执行 `mvn package` target中就会生成两个jar包，一个是普通的，一个是包含所有需要依赖的

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412192844.png)

## 测试

### 将打好的jar包放到服务器上

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412205500.gif)

把前面开的zookeeper、kafka、canal都关掉

`ps`查看进程 `kill -9 pid`杀死进程

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412205834.png)

### 从头启动

> **注意**：注意这里的服务器ip要和配置文件里的host.name一致

```bash
#zookeeper
bin/zkServer.sh start

#canal
bin/startup.sh

#kafka
bin/kafka-server-start.sh -daemon config/server.properties

#创建topic 
bin/kafka-topics.sh --create --zookeeper 服务器ip:2181 --replication-factor 1 --partitions 1 --topic 主题名

#创建消费者
bin/kafka-console-consumer.sh --bootstrap-server 服务器ip:9092 --topic 主题名 --from-beginning

#再开一个窗口，进入上传的jar包位置执行
java -jar kafka-1.0-jar-with-dependencies.jar 主题名 服务器ip
```

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412211827.png)

如图代表执行成功

去数据库中修改数据，注意观察消费者窗口和jar包窗口，这里我新增一条数据

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412212106.png)

对应的两个窗口分别这样显示

jar窗口

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412212206.png)

消费者窗口

![](https://demon-1258469613.cos.ap-shanghai.myqcloud.com/img/20190412212249.png)

二者显示的内容应该是一样的，好了测试完成，修改和删除自行测试。
