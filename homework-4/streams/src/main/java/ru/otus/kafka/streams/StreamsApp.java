package ru.otus.kafka.streams;

import java.time.Duration;
import java.util.Properties;
import org.apache.kafka.common.serialization.Serdes.StringSerde;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamsApp {
    private static final String EVENTS_TOPIC = "events";
    private static final int WINDOW_MINUTES = 5;

    private static final Logger log = LoggerFactory.getLogger(StreamsApp.class);

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, StringSerde.class);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, StringSerde.class);

        StreamsBuilder builder = new StreamsBuilder();
        builder.stream(EVENTS_TOPIC)
                .groupBy((k, v) -> v)
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(WINDOW_MINUTES)))
                .count(Materialized.as("events-count-store"))
                .toStream()
                .mapValues(String::valueOf)
                .foreach((k, v) -> log.info("Events in {} minutes: {}", WINDOW_MINUTES, v));

        @SuppressWarnings("resource")
        KafkaStreams kafkaStreams = new KafkaStreams(builder.build(), props);
        kafkaStreams.start();
    }
}
