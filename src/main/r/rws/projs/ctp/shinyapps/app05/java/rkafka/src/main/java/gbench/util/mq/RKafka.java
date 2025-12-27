package gbench.util.mq;

import org.apache.kafka.clients.consumer.*;
import java.time.Duration;
import java.util.*;

public class RKafka {

    /** 拉取一批消息返回 List<String> 给 R */
    public static List<String> poll(String bootstrapServers, String topic, String groupId) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
              "org.apache.kafka.common.serialization.StringDeserializer");
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
              "org.apache.kafka.common.serialization.StringDeserializer");
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        p.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "1000");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(p)) {
            consumer.subscribe(Collections.singletonList(topic));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
            List<String> ans = new ArrayList<>();
            records.forEach(r -> ans.add(r.value()));
            return ans;
        }
    }

    /* 如果需要 Producer，可继续加静态方法 */
}