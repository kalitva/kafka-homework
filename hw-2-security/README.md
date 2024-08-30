### Развернуть Kafka с KRaft и настроить безопасность

1. Запустить Kafka с Kraft:

    - Сгенерировать UUID кластера
        ```shell
        KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
        ```

    - Отформатировать папки для журналов
        ```shell
        bin/kafka-storage.sh format -t $KAFKA_CLUSTER_ID -c config/kraft/server.properties
        ```

    - Запустить брокер
        ```shell
        bin/kafka-server-start.sh config/kraft/server.properties
        ```

2. Настроить аутентификацию SASL/PLAIN. Создать трёх пользователей с произвольными именами.

    - Создадим файл `kafka_jaas.conf` с данными авторизации со следующим содержимым:
        ```
        KafkaServer {
          org.apache.kafka.common.security.plain.PlainLoginModule required
          username="kafka"
          password="kafka-secret"
          user_admin="admin-secret"
          user_vasya="vasya-secret"
          user_petya="petya-secret"
          user_masha="masha-secret";
        };

        KafkaClient {
          org.apache.kafka.common.security.plain.PlainLoginModule required
          username="vasya"
          password="vasya-secret";
        };
        ```

    - Копируем файл с настройками брокера
        ```shell
        cp config/kraft/server.properties config/kraft/server_sasl.properties
        ```

    - В этом файле необходимо найти и закомментировать строку `inter.broker.listener.name=PLAINTEXT`,
    затем добавить настройки:
        ```properties
        listeners=SASL_PLAINTEXT://mx:9092,CONTROLLER://mx:9093
        security.inter.broker.protocol=SASL_PLAINTEXT
        sasl.mechanism.inter.broker.protocol=PLAIN
        sasl.enabled.mechanisms=PLAIN
        advertised.listeners=SASL_PLAINTEXT://mx:9092
        ```

    - При запуске брокера нужно будет указать путь к файлу `kafka_jaas.conf`
        ```shell
        KAFKA_OPTS="-Djava.security.auth.login.config=/opt/kafka_2.13-3.7.0/private/kafka_jass.conf" \
          bin/kafka-server-start.sh config/kraft/server_sasl.properties
        ```
3. Настроить авторизацию. Создать топик. Первому пользователю выдать права на запись в этот топик.
Второму пользователю выдать права на чтение этого топика. Третьему пользователю не выдавать никаких
прав на этот топик.

    - В файл `server_sasl.properties` добавим следующие настройки:
        ```properties
        authorizer.class.name=org.apache.kafka.metadata.authorizer.StandardAuthorizer
        super.users=User:admin
        allow.everyone.if.no.acl.found=true
        ```

    - Создадим файлы с кредами пользователей. Здесь данные для входа пользователю `admin`, также
    нужно создать файлы по такому шаблону для пользователей `vasya`, `petya` и `masha` (логины и
    пароли для этих пользователей уже заданы в `kafka_jaas.conf`)

        ```properties
        sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="admin-secret";
        security.protocol=SASL_PLAINTEXT
        sasl.mechanism=PLAIN
        ```

    - Создадим топик `test`
        ```shell
        bin/kafka-topics.sh --topic test --create --bootstrap-server mx:9092 --command-config private/admin_sasl.properties
        ```

    - Выдадим права на топик: Васе на чтение, Пете на запись (Маше права выдавать не будем)
        ```shell
        bin/kafka-acls.sh --bootstrap-server mx:9092 \
            --add \
            --allow-principal User:vasya \
            --operation read \
            --topic test \
            --command-config private/admin_sasl.properties

        bin/kafka-acls.sh --bootstrap-server mx:9092 \
            --add \
            --allow-principal User:petya \
            --operation write \
            --topic test \
            --command-config private/admin_sasl.properties
        ```


4. От имени каждого пользователя выполнить команды:
    - Получить список топиков
        - Петя:
            ```shell
            $ bin/kafka-topics.sh --list --bootstrap-server mx:9092 --command-config private/petya_sasl.properties
            __consumer_offsets
            test
            ```

        - Вася:
            ```shell
            $ bin/kafka-topics.sh --list --bootstrap-server mx:9092 --command-config private/vasya_sasl.properties
            __consumer_offsets
            test
            ```

        - Маша:
            ```shell
            $ bin/kafka-topics.sh --list --bootstrap-server mx:9092 --command-config private/masha_sasl.properties
            __consumer_offsets
            ```
    - Записать сообщения в топик
        - Петя:
            ```shell
            $ bin/kafka-console-producer.sh --topic test --bootstrap-server mx:9092 --producer.config private/petya_sasl.properties
            >Hello!
            >I am Petya!
            >Bye bye
            ```

        - Вася:
            ```shell
            $ bin/kafka-console-producer.sh --topic test --bootstrap-server mx:9092 --producer.config private/vasya_sasl.properties
            >Hello
            >[2024-07-03 22:20:04,360] ERROR Error when sending message to topic test with key: null, value: 5 bytes with error: (org.apache.kafka.clients.producer.internals.ErrorLoggingCallback)
            org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [test]
            ```

        - Маша
            ```shell
            $ bin/kafka-console-producer.sh --topic test --bootstrap-server mx:9092 --producer.config private/masha_sasl.properties
            >Hello
            [2024-07-03 22:22:32,590] WARN [Producer clientId=console-producer] Error while fetching metadata with correlation id 5 : {test=TOPIC_AUTHORIZATION_FAILED} (org.apache.kafka.clients.NetworkClient)
            [2024-07-03 22:22:32,594] ERROR [Producer clientId=console-producer] Topic authorization failed for topics [test] (org.apache.kafka.clients.Metadata)
            [2024-07-03 22:22:32,596] ERROR Error when sending message to topic test with key: null, value: 5 bytes with error: (org.apache.kafka.clients.producer.internals.ErrorLoggingCallback)
            org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [test]
            ```

    - Прочитать сообщения из топика
        - Петя:
            ```shell
            $ bin/kafka-console-consumer.sh --topic test --from-beginning --bootstrap-server mx:9092 --consumer.config private/petya_sasl.properties
            [2024-07-03 22:24:57,283] WARN [Consumer clientId=console-consumer, groupId=console-consumer-95745] Not authorized to read from partition test-0. (org.apache.kafka.clients.consumer.internals.FetchCollector)
            [2024-07-03 22:24:57,287] ERROR Error processing message, terminating consumer process:  (kafka.tools.ConsoleConsumer$)
            org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [test]
            Processed a total of 0 messages
            ```
        - Вася
            ```shell
            $ bin/kafka-console-consumer.sh --topic test --from-beginning --bootstrap-server mx:9092 --consumer.config private/vasya_sasl.properties
            Hello!
            I am Petya!
            Bye bye
            ```

        - Маша
            ```shell
            $ bin/kafka-console-consumer.sh --topic test --from-beginning --bootstrap-server mx:9092 --consumer.config private/masha_sasl.properties
            [2024-07-03 22:27:14,199] WARN [Consumer clientId=console-consumer, groupId=console-consumer-59745] Error while fetching metadata with correlation id 2 : {test=TOPIC_AUTHORIZATION_FAILED} (org.apache.kafka.clients
            .NetworkClient)
            [2024-07-03 22:27:14,201] ERROR [Consumer clientId=console-consumer, groupId=console-consumer-59745] Topic authorization failed for topics [test] (org.apache.kafka.clients.Metadata)
            [2024-07-03 22:27:14,202] ERROR Error processing message, terminating consumer process:  (kafka.tools.ConsoleConsumer$)
            org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [test]
            Processed a total of 0 messages
            ```

[Настройка ssl](./ssl.md)
