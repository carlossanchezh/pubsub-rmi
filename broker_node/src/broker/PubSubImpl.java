// Servidor que implementa la interfaz remota PubSub
package broker;
import java.rmi.RemoteException;
import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pubsub.Event;
import pubsub.PubSub;
import pubsub.Subscriber;
import pubsub.SubscriberCallback;

class PubSubImpl extends UnicastRemoteObject implements PubSub  {
    
    public static final long serialVersionUID=1234567890L;

    // Map para guardar los temas
    private Map<String, Topic> temas;

    // Lista para guardar los subscriptores
    private List<Subscriber> subscriptores;

    //Map para guardar los subscriptores y sus callbacks y poder notificar
    private Map<Subscriber, SubscriberCallback> callbacks;

    //Constructor para inicializar el map de temas
    public PubSubImpl() throws RemoteException {
        temas = new HashMap<>();
        subscriptores = new ArrayList<>();
        callbacks = new HashMap<>();

    }

    // METODOS AUXILIARES

    // Método para comprobar si existe un tema
    public synchronized boolean topicExists(String topic) {
        return temas.containsKey(topic);
    }

    // Método para obtener un tema
    public synchronized Topic getTopic(String topic) {
        return temas.get(topic);
    }

    // Método para eliminar un subscriptor
    public synchronized void removeSubscriber(Subscriber subscriber) {
        //Eliminamos sub de la lista general
        subscriptores.remove(subscriber);
        //Eliminar de su callback si lo tenia registrado
        if(callbacks.containsKey(subscriber)) callbacks.remove(subscriber);
    }

    // ---------------------

    public int getVersion() throws RemoteException { // ya programada
        return version;
    }
    public synchronized boolean createTopic(String topic) throws RemoteException {
        
        // Si el tema ya existe false
        if (temas.containsKey(topic)) {
            return false;
        }

        // Crear el nuevo tema y registaralo en el map detemas
        Topic newTema = new Topic();
        temas.put(topic, newTema);

        //Notificar a todos los subscriptores que tienen callback
        for (Subscriber subscriber : callbacks.keySet()) {
            
            SubscriberCallback callback = callbacks.get(subscriber);

            try {
                callback.topicAdded(topic);
            } catch (RemoteException e) {
                //captura excepcion en el broker por si por ejemplo el cliente se hubiera caido
            }
            
        }

        return true; //si ha creado el nuevo tema true

    }
    public synchronized Collection<String> topicList() throws RemoteException {
        // Devolver la lista de los nombres de los temas
        return new ArrayList<>(temas.keySet());
    }
    public synchronized boolean publish(Event ev) throws RemoteException {
        
        //Si el evento no contie un tema registrado en el map entonces false
        if(!temas.containsKey(ev.getTopic())) return false;

        //SI esta registrado en el map se añade a lacola de temas
        Topic topic = temas.get(ev.getTopic());
        topic.addEvent(ev);

        // Añadir evento a la cola de cada subscriptor suscrito al tema
        for (Subscriber subscriber : topic.getSubscribers()) {
    
            // Casting porque el método addEvent() no esta en la intrfaz de Subscriber,
            SubscriberImpl sub = (SubscriberImpl) subscriber;
            sub.addEvent(ev);
        
        }

        return true;
    }
    public synchronized Event consumeEvent(String topic) throws RemoteException {
        
        // Si el tema no existe se lanza excepción
        if (!temas.containsKey(topic)) {
            throw new NoSuchObjectException("topic does not exist");
        }

        // Obter y consumir el primer evento de la cola
        Topic topicQueue = temas.get(topic);
        Event event = topicQueue.pollEvent();

        return event; //Puede ser null si cola vacia

    }
    public synchronized Subscriber initSubscriber(SubscriberCallback c) throws RemoteException {
        
        //Crear nuevo subscriptor
        SubscriberImpl subscriber = new SubscriberImpl(this, c);

        //Añadir nuevo subscriptor a la lista de subscriptores
        subscriptores.add(subscriber);

        //Añadir subscriptor y su callback siac tiene al mapa de subscriptores y su callback
        if(c != null) callbacks.put(subscriber, c); 

        return subscriber;
    }
    public synchronized Collection<Subscriber> subscriberList() throws RemoteException {
        
        //Devoover la lista de subscriptores 
        return new ArrayList<>(subscriptores);
    }
    public synchronized Collection<Subscriber> subscriberListByTopic(String topic) throws RemoteException {
        
        // Comprobar que el tma existe si el tema no existe null
        if (!temas.containsKey(topic)) {
            return null;
        }

        // Obtener el tema y devolver su lista de subscriptores
        Topic topicTemas = temas.get(topic);
        return new ArrayList<>(topicTemas.getSubscribers());

    }
    public synchronized boolean deleteTopic(String topic) throws RemoteException {
        //Si el tema no existe no se puede eliminar
        if(!temas.containsKey(topic)) return false;

        // Obtener el tema y sus subscriptores
        Topic topicMap = temas.get(topic);
        List<Subscriber> subscriptoresTema = topicMap.getSubscribers();

        // Notificar y cancelar suscripción a cada subscriptor
        for (Subscriber subscriber : new ArrayList<>(subscriptoresTema)) { // Crear una copia para no modificar la lista mientras se recorre
            
            // Notificar que el tema ha sido eliminado (si tiene callback)
            SubscriberCallback callback = callbacks.get(subscriber);

            if (callback != null) {
        
                try {
                    
                    //Eliminar tema de callback
                    callback.topicRemoved(topic);
                
                } catch (RemoteException e) {
                    //captura excepcion en el broker por si por ejemplo el cliente se hubiera caido
                }
            
            }
            
            // Cancelar suscripción del subscriptor a este tema
            try {
                
                //Subscriptor se desubscribe del tema
                subscriber.unsubscribe(topic);
            
            } catch (RemoteException e) {
                //captura excepcion en el broker por si por ejemplo el cliente se hubiera caido
            }

        }

        //Eliminar tema
        temas.remove(topic);
        
        return true;
        
    }
    static public void main (String args[])  {
        if (args.length!=1) {
            System.err.println("Usage: PubSubImpl registryPortNumber");
            return;
        }
        try {
            PubSub ps = new PubSubImpl();
            Server.init(ps, args[0]);
        }
        catch (Exception e) {
            System.err.println("PubSubImpl exception: " + e.toString());
            System.exit(1);
        }
    }
}
