// clase estática que contacta con el Registry para obtener la
// referencia remota al servicio

// DEBE RELLENAR EL MÉTODO init

package pubsubcln;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import pubsub.PubSub;

// no se puede instanciar ni derivar
public final class Client {
    private Client(){};

    static public PubSub init(String host, String port) throws RemoteException, NotBoundException {
        
            // Convertir el puerto a entero
            int puerto = Integer.parseInt(port);

            // Localizar el registry en el host y puerto especificados
            Registry registry = LocateRegistry.getRegistry(host, puerto);

            // Nombre bajo el que está registrado el servicio de Server
            String nombreServicio = "PubSub";

            // Buscar el servicio en el registry
            PubSub servicio = (PubSub) registry.lookup(nombreServicio);

            return servicio;
    }
}
