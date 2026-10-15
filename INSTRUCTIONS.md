# Instrucciones de instalación y ejecución

Este proyecto puede ejecutarse de dos formas: **en local** (con los requisitos necesarios instalados en tu máquina) o **con Docker** (sin necesidad de instalar nada más que Docker Desktop).

- Para la **ejecución local**, sigue las secciones [Requisitos local](#requisitos-local), [Instalación para ejecución local](#instalación-para-ejecución-local) y [Ejecución local](#ejecución-local).

- Para la **ejecución con Docker**, sigue las secciones [Requisitos con Docker](#requisitos-con-docker), [Instalación para ejecución con Docker](#instalación-para-ejecución-con-docker) y [Ejecución con Docker](#ejecución-con-docker).

- Para conocer las **operaciones del sistema**, consulta la sección [Uso del sistema](#uso-del-sistema) y los [Casos de uso](#casos-de-uso).

## Requisitos local

- **Java** 11 o superior

## Instalación para ejecución local

### 1. Clonar el repositorio

```bash
git clone https://github.com/carlossanchezh/pubsub-rmi.git
```

> Si no usas Git, descarga el `.zip` del proyecto y descomprímelo en una carpeta local.

### 2. Compilar el módulo `common`

**Linux / macOS**

```bash
cd common
javac -d bin src/pubsub/*.java
cd bin
jar cf ../common.jar pubsub/*.class
```

**Windows**

```powershell
cd common
javac -d bin src\pubsub\*.java
cd bin
jar cf ..\common.jar pubsub\*.class
```

### 3. Copiar `common.jar` en los módulos dependientes

**Linux / macOS**

```bash
cp common/common.jar broker_node/common.jar
cp common/common.jar client_node/common.jar
```

**Windows**

```powershell
copy common\common.jar broker_node\common.jar
copy common\common.jar client_node\common.jar
```

### 4. Compilar el broker

**Linux / macOS**

```bash
cd broker_node
javac -cp .:common.jar -d bin src/broker/*.java
```

**Windows**

```powershell
cd broker_node
javac -cp ".;common.jar" -d bin src\broker\*.java
```

### 5. Compilar el cliente

**Linux / macOS**

```bash
cd client_node
javac -cp .:common.jar -d bin src/pubsubcln/*.java src/pubsubapps/*.java
```

**Windows**

```powershell
cd client_node
javac -cp ".;common.jar" -d bin src\pubsubcln\*.java src\pubsubapps\*.java
```

## Ejecución local

### 1. Arrancar el broker

**Linux / macOS**

```bash
cd broker_node
java -cp bin:common.jar broker.PubSubImpl 54321
```

**Windows**

```powershell
cd broker_node
java -cp "bin;common.jar" broker.PubSubImpl 54321
```

**Salida esperada:**

```
Servicio registrado como 'PubSub'
```

> El broker se queda bloqueado escuchando. No cierres esta terminal.

### 2. Arrancar un cliente

En una nueva terminal:

**Linux / macOS**

```bash
cd client_node
java -cp bin:common.jar pubsubapps.Test localhost 54321
```

**Windows**

```powershell
cd client_node
java -cp "bin;common.jar" pubsubapps.Test localhost 54321
```

**Salida esperada:**

```
Introduzca operacion (Ctrl-D para terminar)
	getVersion|createTopic|topicList|publish|consumeEvent|initSubscriber|
	getUUID|subscriberList|subscribe|subscriberListByTopic|getEvent|
	subscribeGlob|topicListBySubscriber|unsubscribe|exit|deleteTopic
```

> Para probar el sistema con varios clientes, repite el paso anterior. Recuerda lanzar cada cliente en su propia terminal.

> Además del cliente Test interactivo se pueden ejecutar los programas de prueba con los argumentos requeridos

```bash
java -cp bin:common.jar pubsubapps.TestPublisher localhost 54321 tema1
java -cp bin:common.jar pubsubapps.TestSubscriber localhost 54321 tema1 tema2
java -cp bin:common.jar pubsubapps.TestConsumer localhost 54321 tema1
java -cp bin:common.jar pubsubapps.TestTopicCreate localhost 54321 tema1 tema2
java -cp bin:common.jar pubsubapps.TestTopicNotify localhost 54321
java -cp bin:common.jar pubsubapps.TestGlob localhost 54321 "/nivel1a/*/tema[12]" /nivel1a/nivel2a/tema1
```
> En Windows, sustituye `:` por `;` en el classpath (`bin;common.jar` en lugar de `bin:common.jar`).

### 3. Detener el sistema

- En cada cliente: `Ctrl+D` para salir.
- En el broker: `Ctrl+C` para detenerlo.

## Requisitos con Docker

- **Docker Desktop**

## Instalación para ejecución con Docker

Las imágenes del broker ([carlossanchezh/pubsub-rmi-broker](https://hub.docker.com/r/carlossanchezh/pubsub-rmi-broker)) y del cliente ([carlossanchezh/pubsub-rmi-client](https://hub.docker.com/r/carlossanchezh/pubsub-rmi-client)) están publicadas en Docker Hub, por lo que **no es necesario clonar el repositorio** ni construir nada. Basta con descargar las imágenes:

Imagen del broker:

```bash
docker pull carlossanchezh/pubsub-rmi-broker:latest
```

Imagen del cliente:

```bash
docker pull carlossanchezh/pubsub-rmi-client:latest
```

Si prefieres construir las imágenes en lugar de descargarlas de Docker Hub, deberás clonar el repositorio:

```bash
git clone https://github.com/carlossanchezh/pubsub-rmi
```

> Si no usas Git, descarga el `.zip` del proyecto y descomprímelo en una carpeta local.

Una vez clonado el repositorio tendrás que construir las imágenes a partir de los `Dockerfile` del proyecto:

Imagen del broker:

```bash
docker build -t carlossanchezh/pubsub-rmi-broker:latest -f broker_node/Dockerfile .
```

Imagen del cliente:

```bash
docker build -t carlossanchezh/pubsub-rmi-client:latest -f client_node/Dockerfile .
```

> En Windows usar `\` en lugar de `/` (broker_node\Dockerfile) y (client_node\Dockerfile)

## Ejecución con Docker

Lo primero será crear la red

```bash
docker network create pubsub-net
```

Esta red permite que los contenedores se vean entre sí por nombre (broker, cliente1, cliente2, ...).

> `pubsub-net` es un ejemplo de nombre para la red se puede usar cualquiera

### 2. Levantar el primer contenedor Broker

A partir de la imagen del broker descargada/construida previamente en la instalación:

```bash
docker run -it --rm --name broker --hostname broker --network pubsub-net -p 54321:54321 carlossanchezh/pubsub-rmi-broker:latest
```
> --hostname broker fija el nombre que el broker publica en RMI para que los clientes lo resuelvan por DNS en Docker.

La salida esperada será:

```
Servicio registrado como 'PubSub'
```

> El broker se queda bloqueado escuchando. No cierres esta terminal.
 
### 3. Levantar un segundo contenedor Cliente

En otra terminal distinta a partir de la imagen del cliente descargada/construida previamente en la instalación:

```bash
docker run -it --rm --network pubsub-net --name client1 --hostname client1 carlossanchezh/pubsub-rmi-client:latest
```

> `client1` es un ejemplo de nombre para el contenedor se puede usar cualquiera
>
> `client1` es un ejemplo de hostname para el contenedor se puede usar cualquiera

> --hostname client1 fija el nombre que RMI publica para que el broker pueda notificar al cliente por DNS en Docker. 

La salida esperada será:

```
Introduzca operacion (Ctrl-D para terminar)
	getVersion|createTopic|topicList|publish|consumeEvent|initSubscriber|
	getUUID|subscriberList|subscribe|subscriberListByTopic|getEvent|
	subscribeGlob|topicListBySubscriber|unsubscribe|exit|deleteTopic
```

> Para probar el sistema con varios clientes, repite el paso anterior. Recuerda lanzar cada cliente en su propia terminal.

### 4. Detener los contenedores

- En cada cliente: `Ctrl+D` para salir y detener el contenedor.
- En el broker: `Ctrl+C` para detenerlo y detener el contenedor.

> Al haber desplegado los contenedores con --rm al detenerese al contenedor se eliminará automaticamente

## Uso del sistema

El programa `Test` es el cliente interactivo que permite ejercitar todas las operaciones del sistema. A continuación se describe cada una de sus operaciones y cómo se usan.

### Operaciones del servicio general

| Operación | Descripción |
|-----------|-------------|
| `getVersion` | Devuelve la versión del servicio. | 
| `createTopic` | Crea un tema. Devuelve error si ya existe. | 
| `topicList` | Lista los temas existentes. | 
| `publish` | Publica un evento en un tema. | 
| `consumeEvent` | Consume el primer evento de un tema (modo cola). | 
| `initSubscriber` | Da de alta un subscriptor con callback. |
| `subscriberList` | Lista los subscriptores activos. |
| `subscriberListByTopic` | Lista los subscriptores de un tema. | 
| `deleteTopic` | Elimina un tema. | 

### Operaciones del subscriptor

Estas operaciones requieren que el subscriptor haya iniciado su sesión con `initSubscriber` previamente.

| Operación | Descripción | 
|-----------|-------------|
| `getUUID` | Devuelve el identificador del subscriptor. | 
| `subscribe` | Suscribe al subscriptor a un tema. | 
| `subscribeGlob` | Suscribe usando un patrón glob. | 
| `getEvent` | Recoge el siguiente evento del subscriptor. | 
| `topicListBySubscriber` | Lista los temas a los que está suscrito. | 
| `unsubscribe` | Cancela la suscripción a un tema. |
| `exit` | Finaliza la sesión del subscriptor. | 

## Casos de uso

### Operaciones del servicio general

#### `getVersion`

**Correcto:** El cliente se ha conectado correctamente al broker.

```
getVersion
getVersion ha devuelto: 1
```

**Error:** El cliente no encuentra el RMI Registry del broker. Suele ocurrir si el broker no se ha arrancado antes que el cliente.

```
getVersion
error de comunicación java.rmi.ConnectException: Connection refused to host: localhost
```

#### `createTopic`

**Correcto:** El tema no existía y se ha creado correctamente. Los subscriptores con callback registrado reciben la notificación.

```
createTopic
Introduzca el nombre del tema
tema1
Se ha creado el tema
```

**Error:** El tema ya existía en el broker. `createTopic` devuelve `false` y no se modifica nada.

```
createTopic
Introduzca el nombre del tema
tema1
Error al crear el tema
```

#### `topicList`

**Correcto:** Se devuelve una colección de temas.

```
topicList
Topic List 
  Topic tema1  Topic tema2
```

**Error:** No hay un caso de error propio. Siempre devuelve una colección (pudiendo estar vacía):

```
topicList
Topic List 
```

#### `publish`

**Correcto:** El tema existe y el evento se ha encolado tanto en la cola del tema (modo cola) como en las colas de los subscriptores (modo editor/subscriptor).

```
publish
Introduzca el nombre del tema
tema1
Se ha publicado el evento: topic = <tema1> content = <{número de evento=0}>
```

**Error:** El tema no existe, así que `publish` devuelve `false` y no se publica nada.

```
publish
Introduzca el nombre del tema
noexiste
Error al publicar el evento: topic = <noexiste> content = <{número de evento=0}>
```

#### `consumeEvent`

**Correcto:** Había al menos un evento en la cola del tema.

```
consumeEvent
Introduzca el nombre del tema
tema1
Se ha recibido el evento: topic = <tema1> content = <{número de evento=0}>
```

**Error:** El broker lanza `NoSuchObjectException` porque el tema no existe.

```
consumeEvent
Introduzca el nombre del tema
noexiste
recibida la excepción java.rmi.NoSuchObjectException: topic does not exist
```

**Error:** El tema existe pero no hay eventos en su cola. El método devuelve `null`.

```
consumeEvent
Introduzca el nombre del tema
tema1
No ha devuelto evento
```

#### `initSubscriber`

**Correcto:** Se ha creado un nuevo subscriptor en el broker, con su UUID único.

```
initSubscriber
Se ha iniciado el subscriptor fb487511-922b-4aa0-a436-7829505699ae
```

#### `subscriberList`

**Correcto:** Se devuelve una colección de subscriptores.

```
subscriberList
Subscriber List 
  Subscriber fb487511-922b-4aa0-a436-7829505699ae
  Subscriber 6e1ccf26-50bd-4284-a1a4-27bdcb605fa2
```

**Error:** No hay un caso de error propio. Siempre devuelve una colección (pudiendo estar vacía):

```
subscriberList
Subscriber List 
```

#### `subscriberListByTopic`

**Correcto:** Se devuelve una colección de subscriptores suscritos a ese tema.

```
subscriberListByTopic
Introduzca el nombre del tema
tema1
Subscriber List 
  Subscriber fb487511-922b-4aa0-a436-7829505699ae
```

**Error:** El tema no existe, así que el método devuelve `null`.

```
subscriberListByTopic
Introduzca el nombre del tema
noexiste
devuelve un valor nulo
```

#### `deleteTopic`

**Correcto:** El tema existía. Sus subscriptores han sido notificados con `topicRemoved` y sus suscripciones han sido canceladas.

```
deleteTopic
Introduzca el nombre del tema
tema1
Se ha eliminado el tema
```

**Error:** El tema **no existe**, así que `deleteTopic` devuelve `false`.

```
deleteTopic
Introduzca el nombre del tema
noexiste
Error al eliminar el tema
```

### Operaciones del subscriptor

Estas operaciones requieren que el subscriptor haya iniciado su sesión con `initSubscriber` previamente.

#### `getUUID`

**Correcto:** El subscriptor está activo.

```
getUUID
UUID fb487511-922b-4aa0-a436-7829505699ae
```

**Error:** El subscriptor no había iniciado su sesión (`initSubscriber`).

```
getUUID
no se ha dado de alta previamente como subscriptor usando initSubscriber
```

**Error:** El subscriptor ya había finalizado su sesión (`exit`), así que cualquier llamada a sus métodos lanza `NoSuchObjectException`.

```
getUUID
recibida la excepción java.rmi.NoSuchObjectException: this subscriber has already finished
```

#### `subscribe`

**Correcto:** El subscriptor se ha suscrito al tema, que existía y al que no estaba suscrito previamente.

```
subscribe
Introduzca el nombre del tema
tema1
ha devuelto el valor 1
```

**Error:** El tema no existe, así que no se realiza ninguna suscripción.

```
subscribe
Introduzca el nombre del tema
noexiste
ha devuelto el valor 0
```

**Error:** El subscriptor ya estaba suscrito a ese tema. No se duplica la suscripción.

```
subscribe
Introduzca el nombre del tema
tema1
ha devuelto el valor 0
```

**Error:** El subscriptor ya había finalizado su sesión (`exit`), así que cualquier llamada a sus métodos lanza `NoSuchObjectException`.

```
subscribe
Introduzca el nombre del tema
tema1
recibida la excepción java.rmi.NoSuchObjectException: this subscriber has already finished
```

#### `subscribeGlob`

**Correcto:** El patrón coincide con 2 temas existentes en el sistema, y el subscriptor se ha suscrito a ambos.

```
subscribeGlob
Introduzca el nombre del tema
/nivel1a/*/tema[12]
ha devuelto el valor 2
```

**Error:** Ningún tema del sistema coincide con el patrón.

```
subscribeGlob
Introduzca el nombre del tema
/patron/inexistente/*
ha devuelto el valor 0
```

**Error:** Si el patrón tiene una sintaxis glob incorrecta, `PathMatcher` lanza una excepción en el subscriptor, que se propaga al cliente:

```
recibida la excepción java.util.regex.PatternSyntaxException: ...
```

#### `getEvent`

**Correcto:** Había al menos un evento en la cola del subscriptor.

```
getEvent
Se ha recibido el evento: topic = <tema1> content = <{número de evento=0}>
```

**Error:** La cola del subscriptor está vacía. El método devuelve `null`.

```
getEvent
No ha devuelto evento
```

**Error:** El subscriptor ya había finalizado su sesión (`exit`), así que cualquier llamada a sus métodos lanza `NoSuchObjectException`.

```
getEvent
recibida la excepción java.rmi.NoSuchObjectException: this subscriber has already finished
```

#### `topicListBySubscriber`

**Correcto:** Se devuelve una colección de temas del subscriptor.

```
topicListBySubscriber
Topic List 
  Topic tema1  Topic tema2
```

**Error:** No hay un caso de error propio. Siempre devuelve una colección (pudiendo estar vacía):

```
topicListBySubscriber
Topic List 
```

#### `unsubscribe`

**Correcto:** El subscriptor estaba suscrito al tema y se ha cancelado la suscripción.

```
unsubscribe
Introduzca el nombre del tema
tema1
ha devuelto el valor true
```

**Error:** El tema no existe, así que no se puede cancelar una suscripción a él.

```
unsubscribe
Introduzca el nombre del tema
noexiste
ha devuelto el valor false
```

**Error:** El tema existe, pero el subscriptor no estaba suscrito a él.

```
unsubscribe
Introduzca el nombre del tema
tema1
ha devuelto el valor false
```

**Error:** El subscriptor ya había finalizado su sesión (`exit`), así que cualquier llamada a sus métodos lanza `NoSuchObjectException`.

```
unsubscribe
Introduzca el nombre del tema
tema1
recibida la excepción java.rmi.NoSuchObjectException: this subscriber has already finished
```

#### `exit`

**Correcto:** El subscriptor se da de baja de todos sus temas y se elimina de la lista del broker. No produce salida por pantalla.

```
exit
```

**Error:** Después de hacer `exit`, cualquier llamada a los métodos del subscriptor lanza `NoSuchObjectException`. Este comportamiento es el **esperado** y no un error del sistema.
