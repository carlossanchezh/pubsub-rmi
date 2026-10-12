// clase estática que contacta con el Registry para dar de alta el servicio
package broker;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import pubsub.PubSub;

// no se puede instanciar ni derivar
public final class Server {
    private Server(){};

    static void init(PubSub pubsub, String port) throws RemoteException {

            // Convertir el puerto a entero
            int puerto = Integer.parseInt(port);

            // Crear el registro RMI en el puerto especificado (si no existe)
            Registry registry;
            
            try {

                // Se intenta crear el registry
                registry = LocateRegistry.createRegistry(puerto);

            } catch (RemoteException e) {
                
                // Si ya existe, lo obtenemos
                registry = LocateRegistry.getRegistry(puerto);
            }

            // Pner nombre bajo el que se registrará el servicio
            String nombreServicio = "PubSub";

            // Registrar el objeto en el registry
            registry.rebind(nombreServicio, pubsub);

            System.out.println("Servicio registrado como '" + nombreServicio + "'");

    }
}
