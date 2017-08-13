# xmisao/mojix

FROM maven:3.5.0-jdk-8

ADD . /srv/mojix

WORKDIR /srv/mojix

RUN mvn install

EXPOSE 9661

CMD java -jar target/mojix-0.0.1-SNAPSHOT-jar-with-dependencies.jar
