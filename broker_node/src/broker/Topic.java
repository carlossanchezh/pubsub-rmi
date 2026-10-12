// clase Topic
package broker;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import pubsub.Event;
import pubsub.Subscriber;

class Topic {

    //Cola apra guardar los evenetos
    private Queue<Event> colaEventos;

    // Lista de subscriptres suscritos al tema
    private List<Subscriber> subscriptores;

    //COnstructor para inicializar la cola y la lista 
    public Topic() {
        colaEventos = new LinkedList<>();
        subscriptores = new ArrayList<>();
    }

    // Añadir evento a la cola
    public void addEvent(Event evento) {
        colaEventos.add(evento);
    }

    // Sacar evento de la cola 
    public Event pollEvent() {
        return colaEventos.poll();
    }

    // Añadir subscriptor
    public void addSubscriber(Subscriber subscriber) {
        
        subscriptores.add(subscriber);
        
    }

    // Eliminar subscriptor
    public void removeSubscriber(Subscriber subscriber) {
        subscriptores.remove(subscriber);
    }

    // Obtener lista de subscriptores
    public List<Subscriber> getSubscribers() {
        return new ArrayList<>(subscriptores);
    }
}
