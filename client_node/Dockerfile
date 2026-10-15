FROM eclipse-temurin:21-jdk

WORKDIR /app

# El contexto de build es la raíz copiamos desde ahí
COPY common       /app/common
COPY client_node  /app/client_node

# 1. Compilar common y crear common.jar
RUN cd /app/common && \
    javac -d bin src/pubsub/*.java && \
    cd bin && \
    jar cf ../common.jar pubsub/*.class

# 2. Copiar common.jar a client_node
RUN cp /app/common/common.jar /app/client_node/common.jar

# 3. Compilar el cliente
RUN cd /app/client_node && \
    javac -cp .:common.jar -d bin src/pubsubcln/*.java src/pubsubapps/*.java

# Directorio de trabajo desde el que se ejecuta
WORKDIR /app/client_node

# Arranca el Cliente Test apuntando al broker por nombre.
# -Djava.rmi.server.hostname es necesario para que RMI funcione en Docker: el broker publica su nombre en el registry y los clientes lo resuelven por DNS.
ENTRYPOINT ["sh", "-c", "exec java -Djava.rmi.server.hostname=$(hostname) -cp bin:common.jar pubsubapps.Test broker 54321"]