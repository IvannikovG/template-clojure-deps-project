FROM clojure:tools-deps
EXPOSE 7002 7002

COPY target/chinese-dictionary-1-standalone.jar /app.jar

CMD export RELEASE="rel" && java -jar /app.jar
