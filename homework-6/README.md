### Kafka Connect

- Запустить Kafka
```shell
cd /opt/kafka/
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties 
```

- Запустить PostgreSQL
```shell
sudo pg_createcluster 15 main --start
```

- Создать в PostgreSQL тестовую таблицу
```shell
echo 'create table test(id int, data varchar, modified_at timestamp);' | sudo -u postgres psql
```

- Настроить Debezium PostgreSQL CDC Source Connector
    - Скачаем коннектор отсюда https://www.confluent.io/hub/debezium/debezium-connector-postgresql
    - Создадим каталог `plugins` и распакуем туда содержимое архива
        ```shell
        cd ~/Downloads
        unzip debezium-debezium-connector-postgresql-2.5.4.zip
        sudo mv debezium-debezium-connector-postgresql-2.5.4 /opt/kafka/plugins
        ```
    - В файле `/config/connect-distributed.properties` пропишем путь к папке с плагинами
        ```properties
        plugin.path=/opt/kafka/plugins
        ```
    - Установим параметр постгрес `wal_level` и перезапустим
        ```shell
        echo 'alter system set wal_level to logical' | sudo -u postgres psql
        sudo pg_ctlcluster 15 main restart
        ```
    - Создадим файл `postgres-connect.json` со следующим содержимым:
        ```json
        {
            "name": "postgres-connector",
            "config": {
                "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
                "database.hostname" : "localhost",
                "database.port" : "5432",
                "database.user": "postgres",
                "database.password": "postgres",
                "database.dbname": "postgres",
                "table.whitelist": "public.test",
                "mode": "timestamp",
                "timestamp.column.name": "modified_at",
                "topic.prefix": "postgres",
                "tasks.max": "1",
                "plugin.name": "pgoutput"
              }
         }
         ```
- Запустить Kafka Connect
    - Запустим
        ```shell
        bin/connect-distributed.sh config/connect-distributed.properties 
        ```
    - Опубликуем коннектор
        ```shell
        curl -X POST --data-binary "@postgres-connect.json" -H "Content-Type: application/json" http://localhost:8083/connectors | json_pp
        ```
- Добавить записи в таблицу
    ```shell
    echo "insert into test values(1, 'hello world', now())" | sudo -u postgres psql
    ```
- Проверить, что записи появились в Kafka
    ```shell   
    $ bin/kafka-console-consumer.sh --topic postgres.public.test --from-beginning --bootstrap-server localhost:9092    
    {"schema":{"type":"struct","fields":[{"type":"struct","fields":[{"type":"int32","optional":true,"field":"id"}
    ...
    "payload":{"before":null,"after":{"id":1,"data":"hello world","modified_at":1723495805823904},
    ...
    ```
