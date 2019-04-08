import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

class ClientHandler extends Thread {
    final ObjectInputStream dis;
    ObjectOutputStream dos;
    final Socket s;


    // Constructor 
    public ClientHandler(Socket s, ObjectInputStream dis, ObjectOutputStream dos) {
        this.s = s;
        this.dis = dis;
        this.dos = dos;
    }

    @Override
    public void run() {

        String toreturn;
        Message received = null;
        try {
            received = (Message) dis.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                if (received.getPubSub().equals("BusInfoByPub")) {
                    while (true) {
                        received = (Message) dis.readObject();
                        Broker.HM.put(received.busline, received.data);
                        System.out.println("busline : " + received.busline + " data: " + received.data + " Broker" + Broker.portid);
                    }
                } else if (received.getPubSub().equals("InfoToSub")) {
                    if(Broker.leak){
                        dos.writeObject(new Message("Failure to Sub","Due to a problem we couldn't gather info for some BusLines",""));
                        dos.flush();
                    }
                    String Temp = "";
                    Message toSend;
                    while (true) {

                        toSend = new Message("Info to Sub", received.busline, Broker.HM.get(received.busline));
                        if (!Temp.equals(toSend.data)) {
                            dos.writeObject(toSend);
                            dos.flush();
                        }
                        Temp = toSend.data;

                    }

                } else if (received.getPubSub().equals("InnerBrokerCom")) {
                    // System.out.println("PHRE KWDIKO 3");
                    // System.out.println(received.busline);
                    Broker.notify(Integer.parseInt(received.busline));
                    break;
                } else if (received.getPubSub().equals("Failure")) {
                    Broker.leak=true;
                } else if (received.getPubSub().equals("NotifySub")) {
                    System.out.println("Mpike sto NotifySub");
                    Message toSend = new Message(Broker.Topics, Broker.IPPORT);
                    dos.writeObject(toSend);
                    System.out.println("Esteile sto NotifySub");
                    dos.flush();
                    break;
                } else if (received.getPubSub().equals("NotifyPub")) {
                    // System.out.println(received);
                    // System.out.println("TYPOU 4");
                    for (String key : Broker.IPPORT.keySet()) {
                        try {
                            Socket innercontact = new Socket(InetAddress.getByName("localhost"), Integer.parseInt(key));
                            ObjectOutputStream dos = new ObjectOutputStream(innercontact.getOutputStream());
                            dos.writeObject(new Message("InnerBrokerCom", received.data, " Should send the topics to this port"));
                            dos.flush();

                        } catch (Exception e) {
                        }

                    }
                    Broker.notify(Integer.parseInt(received.data));
                    break;
                }
                //System.out.println("kleinei to connection");
                s.close();

                // Ask user what he wants 
                //  dos.writeObject("What do you want?[Date | Time]..\n"+
                //          "Type Exit to terminate connection.");

                // receive the answer from client 


            } catch (Exception e) {
                continue;
            }
        }

        try {
            // closing resources 
            this.dis.close();
            this.dos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}