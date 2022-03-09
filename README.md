## 1. 数据初始化
### 1.1 初始化日志
（1）启动Linux，并创建`${DATA_DIR}`目录来存储文件；  
（2）将`MoviesSystem/files`下的文件存储到`${DATA_DIR}`目录下；  
（3）在`${DATA_DIR}`目录下创建`agent.log`文件，具体命令如下：
```$xslt
touch agent.log
```
### 参数内容修改
```
（1）将`${DATA_DIR}`替换为具体的绝对路径；
```
### 1.2 初始化mysql
（1）输入账号密码登录mysql；
```$xslt
mysql -u${username} -p${password}
```
参数修改：
```
（1）将`${username}`替换为登录的用户名；
（2）将`${password}`替换为登录的密码。
```
（2）创建数据库 movie_information，并在该数据库下创建表 movies 和 top10Results；
```$xslt
drop database if exists movie_information;

create database movie_information;

use movie_information;

create table movies(
id varchar(5) NOT NULL PRIMARY KEY,
name varchar(100) NOT NULL,
tags varchar(100) DEFAULT NULL
);

create table top10Results(
movieid varchar(10) NOT NULL,
avg_score decimal(10,2) NOT NULL
);
```
（3）导入 movies.dat 的数据集到 movies 表中；
```$xslt
LOAD DATA LOCAL INFILE '${DATA_DIR}/movies.dat'
INTO TABLE movies1
FIELDS TERMINATED BY '::'
LINES TERMINATED BY '\n'
(id, name, tags);
```
参数修改：
```
将`${DATA_DIR}`替换为指定的路径。
```
### 1.3 初始化HBase
创建存储行为日志的表：
```$xslt
create 'movies_agent_log', 'info'
```
### 1.4 初始化hive
创建 HBase 表 movies_agent_log 的外部表 movies_agent_log 和 存储查询结果的表 top10Results：
```$xslt
create external table movies_agent_log
(
rowkey string,
userId string,
movieId string,
grade int,
date_time string
) stored by 'org.apache.hadoop.hive.hbase.HBaseStorageHandler'
with serdeproperties("hbase.columns.mapping"=":key, info:userId, info:movieId, info:grade, info:time")
tblproperties("hbase.table.name"="default:movies_agent_log");

create table top10Results(
movieid string,
avg_score decimal(10,2)
);
```
### 1.5 初始化Sqoop
（1）创建 sqoop 文件：
```$xslt
touch ${DATA_DIR}/hive2mysql.opt
```
（2）写入内容：
```$xslt
export
--connect
jdbc:mysql://hadoop002:3306/movie_information
--username
${username}
--password
${password}
--table
top10Results
--num-mappers
1
--export-dir ${HDFS_PATH}
--input-fields-terminated-by
"\001"
--input-null-string
'\\N'
--input-null-non-string
'\\N'
```
（3）参数修改：
```
（1）将 `${DATA_DIR}` 修改为保存该文件的路径；
（2）将 `${username}` 修改为登录mysql的用户名；
（3）将 `${password}` 修改为登录mysql的密码；
（4）将 `${HDFS_PATH}` 修改为hive创建的表 top10results 对应在hdfs上的路径，可以通过 `show create table top10results` 查看。
```

## 2. 启动集群
### 2.1 启动flume
（1）在 `${FLUME_HOME}/conf` 目录下创建文件；
```$xslt
touch log2kafka.conf
```
（2）在 `log2kafka.conf` 文件写入如下内容：
```$xslt
a1.sources = r1
a1.channels = c1

a1.sources.r1.type = TAILDIR
a1.sources.r1.positionFile = ${DATA_DIR}/flume/tail_dir.json
a1.sources.r1.filegroups = f1
a1.sources.r1.filegroups.f1 = ${DATA_DIR}/agent.log
a1.sources.r1.interceptors = i1
a1.sources.r1.interceptors.i1.type = com.bonnie.movie_system.flumeInterceptor$Builder

# a1.sinks.k1.type = logger

a1.channels.c1.type = org.apache.flume.channel.kafka.KafkaChannel
a1.channels.c1.kafka.bootstrap.servers = hadoop002:9092, hadoop002:9093, hadoop002:9094
a1.channels.c1.kafka.topic = fromKafkaLog
a1.channels.c1.kafka.consumer.group.id = logFilter

a1.sources.r1.channels = c1
# a1.sinks.k1.channel = c1
```
（3）在 `${FLUME_HOME}` 目录下启动 flume；
```$xslt
./bin/flume-ng agent -n a1 -c conf/ -f conf/log2kafka.conf -Dflume.root.logger=INFO, console
```
（4）参数修改：
```
（1）将 `${FLUME_HOME}` 修改为实际的 flume 安装目录；
（2）将 `${DATA_DIR}` 修改为实际的数据存储路径。
```
### 2.2 启动Zookeeper
（1）进入Zookeeper安装目录；
```$xslt
cd ${ZK_HOME}
```
（2）执行启动命令：
```$xslt
./bin/zkServer.sh start conf/zoo1.cfg
./bin/zkServer.sh start conf/zoo2.cfg
./bin/zkServer.sh start conf/zoo3.cfg
```
（3）参数修改：
```
将 `${ZK_HOME}` 修改为Zookeeper的实际安装目录。
```
### 2.3 启动kafka
（1）进入kafka的config目录；
```$xslt
cd ${KAFKA_HOME}/conf
```
（2）执行下述命令：
```$xslt
kafka-server-start.sh -daemon server-1.properties
kafka-server-start.sh -daemon server-2.properties
kafka-server-start.sh -daemon server-3.properties
```
（3）创建topic如下：
```$xslt
./bin/kafka-topics.sh --create --zookeeper ${ZK_CONNECTION} --replication-factor 2 --partitions 1 --topic fromKafkaLog
```
（3）参数修改：
```
（1）将 `${KAFKA_HOME}` 修改为kafka实际安装目录；
（2）将 `${ZK_CONNECTION}` 修改为实际zookeeper的连接参数 - 形如 localhost:2181。
```
### 2.4 启动Hadoop
执行如下命令：
```$xslt
start-all.sh
```
### 2.5 启动HBase
（1）进入HBase的安装目录；
（2）执行如下命令启动HBase：
```$xslt
./bin/start-hbase.sh
```
### 2.6 定期执行从hive查询结果到mysql的操作
（1）创建编写导出过程的文件；
```$xslt
touch ${DATA_DIR}/hive2mysql.sh
```
（2）编写具体的导出过程：
```$xslt
#!/bin/bash
# 执行 hive 语句更新结果数据
hive -e "
add jar ${DATA_DIR}/HiveUDTF-1.0-SNAPSHOT.jar;
create temporary function myudaf as 'com.bonnie.movie_system.myUDAF';

use movie_system;

insert overwrite table top10Results
select movieid, c.sum
from
(    select movieid, myudaf(grade) c
     from movies_agent_log
         group by movieid
) a
order by c.count desc, c.sum desc
limit 10;
"

# 清空MySQL表的数据
mysql -u${username} -p${password} -e "truncate table movie_information.top10Results;"

# 执行Sqoop导出数据到MySQL
/opt/pkg/sqoop/bin/sqoop --option-file hive2mysql.opt
```
（3）参数修改：
```
（1）将 `${DATA_DIR}` 修改为实际存储文件的路径；
（2）将 `${username}` 修改为登录mysql的用户名；
（3）将 `${password}` 修改为登录mysql的密码。
```

## 3. 启动 MoviesSystem 下的 showing module
### 3.1 启动 ShowingApplication.java
（1）点击 main 左侧的绿色三角形；
（2）打开`chrome`浏览器，输入`localhost:8080/index`；
（3）成功运行，隔一段时间刷新一次页面，排行榜会根据不断获取的结果做动态修改；
### 3.2 修改参数内容
```$xslt
# 修改 application.yml 下的内容：${HOSTNAME}为主机名，${MYSQL_ROOT}为mysql登录名称，${MYSQL_PASSWORD}为mysql登陆密码
spring:
  datasource:
    url: jdbc:mysql:///${HOSTNAME}:3306/movie_information?useUnicode=true&characterEncoding=utf-8&serverTimezone=UTC
    username: ${MYSQL_ROOT}
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.jdbc.Driver
```

至此，就可以成功实现实时获取数据进行计算。  
统计表格如下：
```$xslt
http://localhost:8080/index
```