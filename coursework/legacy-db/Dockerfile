FROM postgres:15

RUN apt-get update && \
  apt-get install -y wget && \
  apt-get install -y unzip

RUN wget https://www.postgresqltutorial.com/wp-content/uploads/2019/05/dvdrental.zip && \
  unzip dvdrental.zip && \
  rm dvdrental.zip

ADD init.sql /docker-entrypoint-initdb.d
ADD restore.sh /docker-entrypoint-initdb.d

ENV POSTGRES_PASSWORD=postgres

EXPOSE 5432
