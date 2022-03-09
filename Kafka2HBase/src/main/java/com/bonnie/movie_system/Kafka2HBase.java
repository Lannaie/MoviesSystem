package com.bonnie.movie_system;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * created by Bonnie on 2022/3/6
 */
public class Kafka2HBase {
//    public static void main(String[] args) throws IOException, ParseException {
    public void start_kafka2HBase() throws IOException, ParseException {
        // 配置 Kafka 文件
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "hadoop002:9092, hadoop002:9093, hadoop002:9094");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "logFilter");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        // 创建消费者
        KafkaConsumer<String, String> kafkaConsumer = new KafkaConsumer<String, String>(props);

        // 订阅主题
        List<String> topics = Arrays.asList("fromKafkaLog");
        kafkaConsumer.subscribe(topics);

        // 配置 HBase 文件
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "hadoop002:2181,hadoop002:2182,hadoop002:2183");
        Connection connection = ConnectionFactory.createConnection(conf);

        // 存储HBase
        String table_name = "movies_agent_log";
        String cf = "info";

        // 连接 table
        Table table = connection.getTable(TableName.valueOf(table_name));

        // 消费数据
        try {
            while (true)
            {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(50);
                for (ConsumerRecord<String, String> record : records)
                {
                    System.out.println("Get: " + record.value());
                    String[] split = record.value().split("::");

//                    System.out.println("切分结果：");
                    String userId = split[0];
                    String movieId = split[1];
                    String grade = split[2];
                    String time = split[3];
                    long timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time).getTime();

//                    System.out.println("userId-" + userId + " movieId-" + movieId + " grade-" + grade + " time-" + time);
                    Put row = new Put(Bytes.toBytes(userId + "_" + movieId + "_" + timestamp));
                    row.addColumn(Bytes.toBytes("info"), Bytes.toBytes("userId"), Bytes.toBytes(userId));
                    row.addColumn(Bytes.toBytes("info"), Bytes.toBytes("movieId"), Bytes.toBytes(movieId));
                    row.addColumn(Bytes.toBytes("info"), Bytes.toBytes("grade"), Bytes.toBytes(grade));
                    row.addColumn(Bytes.toBytes("info"), Bytes.toBytes("time"), Bytes.toBytes(time));
                    table.put(row);
                }
            }
        }finally {
            table.close();
            connection.close();
            kafkaConsumer.close();
        }
    }
}
