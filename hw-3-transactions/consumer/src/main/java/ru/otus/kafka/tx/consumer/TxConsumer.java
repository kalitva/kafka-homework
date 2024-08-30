package ru.otus.kafka.tx.consumer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TxConsumer {
    private static final Logger log = LoggerFactory.getLogger(TxConsumer.class);

    private static final String TOPIC_1 = "topic1";
    private static final String TOPIC_2 = "topic2";

    public static void main(String[] args) {
        var props = new HashMap<String, Object>() {{
            put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            put(ConsumerConfig.GROUP_ID_CONFIG, "TxConsumer1");
            put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        }};

        try (var consumer = new KafkaConsumer<String, String>(props)) {
            consumer.subscribe(List.of(TOPIC_1, TOPIC_2));

            while (true) {
                var records = consumer.poll(Duration.ofSeconds(1));
                records.forEach(r -> log.info(r.topic() + ": " + r.value()));
            }
        }
    }
}
