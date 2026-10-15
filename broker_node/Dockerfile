FROM eclipse-temurin:21-jdk

WORKDIR /app

# El contexto de build es la raíz copiamos desde ahí
COPY common       /app/common
COPY broker_node  /app/broker_node

# 1. Compilar common y crear common.jar
RUN cd /app/common && \
    javac -d bin src/pubsub/*.java && \
    cd bin && \
    jar cf ../common.jar pubsub/*.class

# 2. Copiar common.jar a broker_node
RUN cp /app/common/common.jar /app/broker_node/common.jar

# 3. Compilar el broker
RUN cd /app/broker_node && \
    javac -cp .:common.jar -d bin src/broker/*.java

# Puerto del registry RMI
EXPOSE 54321

# Directorio de trabajo desde el que se ejecuta
WORKDIR /app/broker_node

# Arrancar el broker.
# -Djava.rmi.server.hostname es necesario para que RMI funcione en Docker: el broker publica su nombre en el registry y los clientes lo resuelven por DNS.
ENTRYPOINT ["java", "-Djava.rmi.server.hostname=broker", "-cp", "bin:common.jar", "broker.PubSubImpl", "54321"]