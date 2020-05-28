package daniel;

import net.xqhs.flash.core.composite.CompositeAgent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

public class Serialization {
    public static void main(String[] args) {
        CompositeAgent agent = new CompositeAgentBuilder().build();

        ByteArrayOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(fos);
            out.writeObject(agent);
            out.close();
            System.out.println("The byte array is: ");
            System.out.println(fos);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String objStringBase64 = Base64.getEncoder().encodeToString(fos.toByteArray());
        System.out.println("The encoded string: ");
        System.out.println(objStringBase64);
        // read the object from file
        // save the object to file
        ByteArrayInputStream fis = null;
        ObjectInputStream in = null;
        try {
            fis = new ByteArrayInputStream(Base64.getDecoder().decode(objStringBase64));
            in = new ObjectInputStream(fis);
            agent = (CompositeAgent) in.readObject();
            in.close();
            System.out.println("Deserialized agent obj from string:");
            System.out.println(agent);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // TODO 2 noduri, 2 agenti, fiecare 1 shard care numara secundele
        // mutam un agent pe celalalt nod
    }
}
