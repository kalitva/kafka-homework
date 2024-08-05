package ru.otus.kafka.tx.producer;

import java.util.HashMap;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class TxProducer {
    private static final String TOPIC_1 = "topic1";
    private static final String TOPIC_2 = "topic2";

    public static void main(String[] args) throws InterruptedException {
        var props = new HashMap<String, Object>() {{
            put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "Tx1");
        }};

        try (var producer = new KafkaProducer<String, String>(props)) {
            producer.initTransactions();

            producer.beginTransaction();
            for (int i = 0; i < 5; i++) {
                producer.send(new ProducerRecord<String, String>(TOPIC_1, "committed " + i));
                producer.send(new ProducerRecord<String, String>(TOPIC_2, "committed " + i));
                Thread.sleep(100);
            }
            producer.commitTransaction();

            producer.beginTransaction();
            for (int i = 0; i < 2; i++) {
                producer.send(new ProducerRecord<String, String>(TOPIC_1, "rolled back " + i));
                producer.send(new ProducerRecord<String, String>(TOPIC_2, "rolled back " + i));
                Thread.sleep(100);
            }
            producer.abortTransaction();
        }
    }
}
