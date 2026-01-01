# 工作目录选择
file.choose()|>dirname()|>setwd()

# > getwd()
# [1] "F:/slicef/ws/gitws/malonylcoa/src/main/r/rws/projs/ctp/shinyapps/app05/java"
# > 
# > list.files("./rkafka/target")
# [1] "classes"                          "generated-sources"               
# [3] "maven-archiver"                   "maven-status"                    
# [5] "original-rkafka-0.1-SNAPSHOT.jar" "rkafka-0.1-SNAPSHOT.jar"      

# 加载 rJava
library(rJava)

# JVM初始化
.jinit()

# 加入jar文件
.jaddClassPath("./rkafka/target/rkafka-0.1-SNAPSHOT.jar")

# /opt/sliced/develop/kafka/4.0.0/kafka_2.13-4.0.0/bin
# 测试读取数据内容（确保可以读取到数据内容）
# kafka-console-consumer.sh --bootstrap-server 192.168.1.41:9092  --topic test_cxx_ctp_topic --from-beginning --max-messages 10

# 读取消息行情
msgs <- .jcall("gbench/util/mq/RKafka", "Ljava/util/List;", "poll", "192.168.1.41:9092",  "test_cxx_ctp_topic",  "rjava-group")
cat("收到", length(msgs), "条\n")

# 3. 逐条解析 JSON
## 1. 构造任意空数组模板
objArray <- .jarray(list(), "java/lang/Object")   # Object[]

## 2. 调 toArray(Object[])
objVec <- .jcall(msgs, "[Ljava/lang/Object;", "toArray", objArray)

## 3. 逐个强转为 String 再解析
jsonVec <- sapply(objVec, function(x) {
  if (is.jnull(x)) return(NA_character_)
  .jcall(x, "S", "toString")          # 相当于 (String) x
})

## 4. 解析 JSON
ticks <- lapply(jsonVec, jsonlite::fromJSON)

# 4. 转成 data.frame（统一字段）
df <- data.table::rbindlist(ticks, fill = TRUE)

# 显示信息
print(df[, .(InstrumentID, LastPrice, Volume, UpdateTime)])
