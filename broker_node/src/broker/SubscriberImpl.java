// Clase que implementa la interfaz remota Subscriber
package broker;
import java.rmi.RemoteException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.rmi.NoSuchObjectException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import pubsub.Subscriber;
import pubsub.SubscriberCallback;
import pubsub.Event;

class SubscriberImpl extends UnicastRemoteObject implements Subscriber  {
    public static final long serialVersionUID=1234567890L;
    UUID subUUID; // para facilitar depuración
    PubSubImpl ps; // para acceder a funcionalidad del servicio general
    // para notificar al subscriptor de creación y destrucción de temas
    transient SubscriberCallback scbk; 

    // Lista de temas a los que está suscrito este subscriptor
    private List<String> temasSubscrito;

    // Cola de eventos para este subscriptor
    private Queue<Event> colaEventos;

    //Flag que comprueba si el subscriptor hizo exit
    private boolean exit;

    public SubscriberImpl(PubSubImpl p, SubscriberCallback s) throws RemoteException {
        super();
        this.scbk = s;
        this.subUUID = UUID.randomUUID();
	    this.ps = p ;
        
        temasSubscrito = new ArrayList<>();
        colaEventos = new LinkedList<>();
        exit = false;
    }

    // METODOS AUXILIARES

    // Añadir evento a la cola del subscriptor
    public void addEvent(Event event) {
        colaEventos.add(event);
    }

    public void removeTopicFromSubscriber(String topic) {
        temasSubscrito.remove(topic);
    }

    // ---------------------

    public UUID getUUID() throws RemoteException {
      
        if(exit) throw new NoSuchObjectException("this subscriber has already finished");  
        return subUUID;
    }
    public int subscribe(String topic, boolean glob) throws RemoteException {
        
        if(exit) throw new NoSuchObjectException("this subscriber has already finished");  

        if(glob){
            
            //Contador de subbscripciones
            int numeroSubs = 0;

            //Crear el comparador usando patron recibido
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + topic);
            
            //Recorrer todos los temas en el sistema
            for(String nombreTema : ps.topicList()){
                
                //Si el tema coincide con el patron realizamos suscripcion
                if(matcher.matches(Paths.get(nombreTema))) numeroSubs += subscribe(nombreTema, false);
        
            }

            return numeroSubs;
        }

        // SI el tema no esta en los temas  o si el subscriptor ya esta subscrito al tema devuelve 0
        if (!ps.topicExists(topic) || temasSubscrito.contains(topic)) {
            return 0;
        }

        // Si puede subscribise al tema añade el tema a su lista de subscripciones
        Topic topicMap = ps.getTopic(topic);
        
        // Añadir el subscriptor a la lista del tema
        topicMap.addSubscriber(this);

        // Añadir el tema a la lista del subscriptor
        this.temasSubscrito.add(topic);

        return 1; //devileve numero de suscripciones realizadas si todo funciona 1

    }
    public Event getEvent() throws RemoteException {

        if(exit) throw new NoSuchObjectException("this subscriber has already finished");

        //Devolver primer evento de la cola y consumirlo
        return colaEventos.poll();// si cola vacia null
    }
    public Collection<String> topicListBySubscriber() throws RemoteException {

        if(exit) throw new NoSuchObjectException("this subscriber has already finished");

        return new ArrayList<>(temasSubscrito);
    }
    public boolean unsubscribe(String topic) throws RemoteException {
        
        if(exit) throw new NoSuchObjectException("this subscriber has already finished");

        // Si el tema no existe no puede desuscribirse de un tema que no existe
        // Si no esta subscrito a un tema no puede desubscribirse de un tema al que no esta subscrito
        if (!ps.topicExists(topic) || !temasSubscrito.contains(topic)) {
            return false;
        }
        
        // Obtener el tema
        Topic topicMap = ps.getTopic(topic);
        
        //Eliminar al subscriptor de la lista de subscriptores
        topicMap.removeSubscriber(this);

        //Eliminar el tema de la lista del subscriptor
        temasSubscrito.remove(topic);

        return true;

    }
    public void exit() throws RemoteException {
        if(exit) return;

        //Desubscribir al subscriptor de los temas a los que estaba subscrito
        for(String topic : new ArrayList<>(temasSubscrito)){  //copia para no modificar la lista original

            unsubscribe(topic);
        }

        //Eliminar de la lista de subscriptores
        ps.removeSubscriber(this);

        //Vacia cola de eventos pendientes
        colaEventos.clear();

        //Marcamos finalizado
        exit = true;
    }
}
