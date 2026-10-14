# Sistema Editor/Subscriptor Distribuido

## Descripción

Sistema **editor/subscriptor** distribuido implementado con **Java RMI**, siguiendo el modelo clásico de comunicación desacoplada entre productores y consumidores de información.

El sistema permite a los **editores** publicar eventos en **temas** jerárquicos, los **subscriptores** pueden suscribirse a los **temas** que les interesen, recibir **notificaciones** dinámicas de creación y destrucción de temas, y consumir **eventos** en un modo alternativo de cola de mensajes. Entre ambos media un **broker** que actúa como intermediario entre **editores** y **subscriptores**.

El sistema está construido sobre **Java RMI**, lo que permite que el broker y los clientes se ejecuten en máquinas distintas y se comuniquen de forma transparente a través de la red.

## Arquitectura

El sistema sigue una **arquitectura cliente-servidor distribuida** con un componente central (el broker) y múltiples componentes cliente (editores, subscriptores y consumidores).

### Componentes del sistema

El sistema se organiza en torno a cuatro roles fundamentales editores, subscriptores, consumidores y broker que se relacionan a través de dos abstracciones clave: los temas y los eventos.

| Componente | Rol en el sistema |
|------------|-------------------|
| **Broker** | Intermediario central entre editores y subscriptores. |
| **Editor** | Cliente que publica eventos en un tema. |
| **Subscriptor** | Cliente que se suscribe a temas y recoge los eventos que le corresponden. |
| **Consumidor** | Cliente que consume eventos del broker en modo cola. |
| **Tema** | Agrupa eventos y subscriptores bajo un nombre jerárquico. |
| **Evento** | Unidad de información publicada. |
| **Notificación de tema** | Aviso a los subscriptores de la creación o destrucción de temas. |
| **Registro de servicios** | Permite localizar el broker por un nombre lógico. |

### Diseño

El diseño del sistema gira en torno al **broker**, que es el componente central y el único que mantiene estado. Los demás elementos, interfaces comunes y cliente existen para que el broker pueda ser utilizado de forma remota.

#### El broker

El **broker** es el corazón del sistema. Se encarga de gestionar los temas, almacenar los eventos y mantener el registro de subscriptores activos. Su implementación se reparte en cuatro clases del paquete `broker`:

| Clase | Responsabilidad |
|-------|-----------------|
| `PubSubImpl` | Implementa el servicio general. Es el broker propiamente dicho. |
| `SubscriberImpl` | Objeto remoto que representa a un subscriptor concreto. Es creado por el broker al darse de alta un cliente. |
| `Topic` | Representa un tema. Gestiona su cola de eventos y su lista de subscriptores. |
| `Server` | Clase de utilidad que registra el broker en el RMI Registry. |

**`PubSubImpl`** mantiene tres estructuras de datos principales:

- Un **mapa de temas** (`Map<String, Topic>`) que asocia cada nombre de tema con su objeto `Topic`.
- Una **lista de subscriptores** (`List<Subscriber>`) con todos los subscriptores activos.
- Un **mapa de callbacks** (`Map<Subscriber, SubscriberCallback>`) que asocia cada subscriptor con el objeto de callback que utilizó al darse de alta, para poder notificarle cambios en los temas.

Sobre esas estructuras, `PubSubImpl` implementa las operaciones del servicio general: crear y eliminar temas, publicar eventos, consumir eventos en modo cola, dar de alta subscriptores y consultar el estado del sistema. Todos los métodos son `synchronized`, ya que varios clientes pueden invocarlos de forma concurrente.

**`SubscriberImpl`** es un objeto remoto que el broker crea para cada cliente que se da de alta mediante `initSubscriber`. Representa la sesión de ese cliente en el sistema y mantiene:

- Un **UUID** que lo identifica de forma única.
- Una **lista de temas suscritos** (`List<String>`).
- Una **cola de eventos** (`Queue<Event>`) propia, donde el broker va depositando los eventos que le corresponden.
- Una referencia al **callback** del cliente, si expresó interés en recibir notificaciones de temas.
- Un **flag de finalización** que indica si el subscriptor ya ha hecho `exit`.

Mientras está activo, el broker puede invocar métodos sobre este objeto remoto para encolar eventos (`addEvent`), consultar sus suscripciones o darlo de baja. Una vez finalizado, cualquier llamada lanza `NoSuchObjectException`.

**`Topic`** es una clase de datos sencilla. Cada tema mantiene dos colecciones:

- Una **cola de eventos** (`Queue<Event>`), que da soporte al modo de operación de cola de mensajes.
- Una **lista de subscriptores** (`List<Subscriber>`), que permite al broker saber a quién debe encolar cada evento en el modo editor/subscriptor.

**`Server`** es una clase estática sin estado que se encarga de una única tarea: crear (o localizar) el RMI Registry en el puerto indicado y registrar el objeto `PubSubImpl` bajo el nombre lógico `PubSub`. A partir de ese momento, los clientes pueden localizar el servicio sin conocer su dirección de red.

#### El paquete común

El paquete `common` agrupa las **interfaces remotas** y la clase `Event`, que actúan como contrato entre el broker y los clientes y definen la API pública del sistema.

| Clase / interfaz | Responsabilidad |
|------------------|-----------------|
| `PubSub` | Operaciones generales del servicio. |
| `Subscriber` | Operaciones específicas de un subscriptor. |
| `SubscriberCallback` | Métodos que el broker invoca en el cliente para notificar cambios de temas. |
| `Event` | Clase serializable que representa un evento publicado. |

#### El cliente

El cliente se apoya en dos paquetes: `pubsubcln` y `pubsubapps`.

**`pubsubcln`** contiene la clase de utilidad `Client`, que localiza el servicio en el RMI Registry y devuelve al programa una referencia remota al broker. Es el punto de entrada común para todos los programas cliente.

| Clase | Rol | Descripción |
|-------|-----|-------------|
| `Client` | Utilidad | Localiza el servicio en el RMI Registry y devuelve una referencia remota al broker. |

**`pubsubapps`** agrupa los programas de prueba que ejercitan la API del sistema. Todos utilizan la misma clase `Client` para conectarse al broker, pero cada uno desempeña un rol distinto.

| Clase | Rol | Descripción |
|-------|-----|-------------|
| `Test` | Cliente interactivo | Cliente completo con menú que permite invocar todas las operaciones del sistema. |
| `TestPublisher` | Editor | Publica un evento en un tema cada 3 segundos de forma indefinida. |
| `TestSubscriber` | Subscriptor | Se suscribe a varios temas y recoge sus eventos cada 5 segundos. |
| `TestConsumer` | Consumidor | Consume eventos directamente del broker en modo cola cada 5 segundos. |
| `TestTopicCreate` | Gestor de temas | Crea temas, espera, y luego los destruye, notificando a los subscriptores interesados. |
| `TestTopicNotify` | Oyente de temas | Se da de alta con un callback y espera recibir notificaciones de creación y destrucción de temas. |
| `TestGlob` | Prueba de glob | Crea varios temas y comprueba la subscripción mediante patrones glob. |

De todos ellos, `Test` es el más completo y el único que se usa para validar todas las operaciones del broker y del subscriptor. Los demás son programas de ejemplo que ilustran el uso de la API desde distintos roles.

### Funcionamiento

A continuación se describe el funcionamiento del sistema, organizado en los flujos principales que lo componen: arranque y localización del servicio, creación y eliminación de temas, publicación de eventos, subscripción y recepción y finalización de un subscriptor.

#### Arranque y localización del servicio

El broker arranca con `PubSubImpl.main`, que crea una instancia de `PubSubImpl` y llama a `Server.init`. Esta clase crea (o localiza, si ya existía) el RMI Registry en el puerto indicado y registra el broker bajo el nombre lógico `PubSub`. A partir de ese momento, cualquier cliente puede localizarlo llamando a `Client.init(host, port)`, que devuelve una referencia remota al broker.

#### Creación de temas

Un cliente puede crear un tema llamando a `createTopic(tema)`. El broker comprueba primero si el tema ya existe; si es así, devuelve `false` y no hace nada más. En caso contrario:

1. Crea un nuevo objeto `Topic` y lo añade a su mapa de temas.
2. Recorre su mapa de callbacks y notifica a cada subscriptor interesado mediante `topicAdded(tema)`.

#### Eliminación de temas

Un cliente puede eliminar un tema llamando a `deleteTopic(tema)`. Si el tema no existe, el broker devuelve `false`. Si existe, realiza tres acciones:

1. Recorre la lista de subscriptores de ese tema y, para cada uno, notifica la baja mediante `topicRemoved(tema)` (si tiene callback registrado) y cancela su suscripción al tema.
2. Elimina el tema de su mapa de temas.
3. Devuelve `true`.

#### Publicación de eventos

Un editor invoca `publish(event)` sobre el broker. El broker realiza dos acciones:

1. Encola el evento en la **cola del tema** (`Topic.colaEventos`), para que pueda ser consumido en modo cola mediante `consumeEvent`.
2. Encola el evento en la **cola de cada subscriptor** suscrito a ese tema, para que pueda ser recogido en modo editor/subscriptor mediante `getEvent`.

#### Subscripción y recepción

Un cliente que quiere actuar como subscriptor llama primero a `initSubscriber(callback)`. El broker crea un nuevo `SubscriberImpl`, lo añade a su lista y, si se proporcionó un callback, lo registra en el mapa de callbacks. Después, el cliente invoca `subscribe(tema, glob)` sobre su subscriptor para indicar los temas que le interesan. Si `glob` es `true`, el subscriptor compara el patrón contra la lista actual de temas del sistema y se suscribe a todos los que coincidan.

Cuando el subscriptor quiere recoger eventos, invoca `getEvent()`, que devuelve y elimina el primer evento de su cola. Si la cola está vacía, devuelve `null`.

#### Finalización

Un subscriptor termina su sesión con `exit()`. El broker lo elimina de todas sus suscripciones y de su lista general. A partir de ese momento, cualquier llamada a los métodos del subscriptor lanza `NoSuchObjectException`, evitando usos indebidos.

### Tecnologías

- Lenguaje: **Java**
- Comunicación entre broker y clientes: **Java RMI**

## Estructura del proyecto

```plaintext
.
├── broker_node/                         # Implementación del broker
│   └── src/                     
│       └── broker/                      # Paquete del broker
│           ├── PubSubImpl.java          # Implementación del servicio general
│           ├── SubscriberImpl.java      # Implementación de los subscriptores
│           ├── Server.java              # Registro del servicio en RMI
│           └── Topic.java               # Gestión de un tema
│ 
├── client_node/                         # Clientes y utilidades
│   └── src/
│       ├── pubsubapps/                  # Programas de prueba
│       │   ├── Test.java                # Cliente interactivo completo
│       │   ├── TestConsumer.java        # Consumidor de ejemplo (modo cola)
│       │   ├── TestGlob.java            # Prueba de subscripción con glob
│       │   ├── TestPublisher.java       # Editor de ejemplo
│       │   ├── TestSubscriber.java      # Subscriptor de ejemplo
│       │   ├── TestTopicCreate.java     # Creador y destructor de temas
│       │   └── TestTopicNotify.java     # Receptor de notificaciones de temas
│       │
│       └── pubsubcln/                   # Utilidades del cliente
│           └── Client.java              # Localización del servicio en RMI
│
├── common/                              # Clases comunes a broker y cliente
│   └── src/
│       └── pubsub/                      # Paquete con las interfaces remotas
│           ├── Event.java               # Clase que representa un evento
│           ├── PubSub.java              # Interfaz general del servicio
│           ├── Subscriber.java          # Interfaz de los subscriptores
│           └── SubscriberCallback.java  # Interfaz de callbacks
│   
├── .gitignore                           # Archivos y carpetas ignorados por Git
├── INSTRUCTIONS.md                      # Instrucciones de instalación y ejecución del proyecto
└── README.md                            # Descripción del proyecto 
```

## Instalación y ejecución

Ver [INSTRUCTIONS.md](INSTRUCTIONS.md)
