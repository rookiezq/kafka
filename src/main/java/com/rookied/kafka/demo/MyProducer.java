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
