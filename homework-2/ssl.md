### Настройка ssl

- Создать собственный центр авторизации (CA)
    ```shell
    openssl req -new -x509 -keyout ca-key -out ca-cert -days 365
    keytool -keystore server.truststore.jks -alias CARoot -importcert -file ca-cert
    keytool -keystore client.truststore.jks -alias CARoot -importcert -file ca-cert
    ```

- Создать SSL ключ и сертификат для брокера
    ```shell
    $ keytool -genkey \
    -keyalg RSA \
    -keystore server.keystore.jks \
    -keypass password \
    -alias mx \
    -validity 365 \
    -storetype pkcs12 \
    -storepass password \
    -dname "CN=mx,OU=Kafka,O=Otus,L=Moscow,ST=Moscow,C=RU"
    ```

- Подписать сертификат
    ```shell
    keytool -keystore server.keystore.jks -alias mx -certreq -file cert-file
    openssl x509 -req -CA ca-cert -CAkey ca-key -in cert-file -out cert-signed -days 365 -CAcreateserial -passin pass:password
    keytool -keystore server.keystore.jks -alias CARoot -importcert -file ca-cert
    keytool -keystore server.keystore.jks -alias mx -importcert -file cert-signed
    ```

- Настроить брокер
    - Копируем файл с настройками брокера `cp server_sasl.properties server_sasl_ssl.properties`
    - Отредактируем настройки
        ```
        listeners=SASL_PLAINTEXT://mx:9092,CONTROLLER://mx:9093 --> listeners=SASL_SSL://mx:9092,CONTROLLER://mx:9093
        security.inter.broker.protocol=SASL_PLAINTEXT --> security.inter.broker.protocol=SASL_SSL
        advertised.listeners=SASL_PLAINTEXT://mx:9092 --> advertised.listeners=SASL_SSL://mx:9092
        ```
    - И добавим
        ```properties
        ssl.keystore.location=/opt/kafka_2.13-3.7.0/private/server.keystore.jks
        ssl.keystore.password=password
        ssl.key.password=password
        ssl.truststore.location=/opt/kafka_2.13-3.7.0/private/server.truststore.jks
        ssl.truststore.password=password
        ssl.client.auth=requested
        ssl.endpoint.identification.algorithm=
        ```
- Настроить клиента
    - Копируем файла клиента `cp admin_sasl.properties admin_sasl_ssl.properties`
    - Добавим настройки
        ```properties
        ssl.truststore.location=/opt/kafka_2.13-3.7.0/private/client.truststore.jks
        ssl.truststore.password=password
        ```
- Проверяем
    ```shell
    $ bin/kafka-console-consumer.sh --topic test --from-beginning --bootstrap-server mx:9092 --consumer.config private/admin_sasl_ssl.properties
    Hello!
    I am Petya!
    Bye bye
    ```
