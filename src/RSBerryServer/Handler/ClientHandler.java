package RSBerryServer.Handler;

import RSBerryServer.Client;
import RSBerryServer.Except.NoEmptySlot;
import RSBerryServer.Utility.Setting;

import java.io.IOException;
import java.net.Socket;

public class ClientHandler
{
    private static Client clients[] = new Client[Setting.getMaxPlayers()];

    public ClientHandler()
    {

    }

    public void process()
    {
        for (int i = 0; i < Setting.getMaxPlayers(); i++) {
            if (clients[i] == null) {
                continue;
            }

            // Process each of the clients
            clients[i].process();
        }
    }

    public static Client request(Socket socket) throws NoEmptySlot
    {
        int next_slot = nextAvailableSlot();
        clients[next_slot] = new Client(socket, next_slot);
        return clients[next_slot];
    }

    public static int nextAvailableSlot() throws NoEmptySlot
    {
        for (int i = 0; i < Setting.getMaxPlayers(); i++) {
            if (clients[i] == null) {
                return i;
            }
        }
        throw new NoEmptySlot("All the players slots a occupied!");
    }

    public static void kick(int slot)
    {
        try {
            clients[slot].socket.close();
            clients[slot] = null;
        } catch (IOException ioe) {
            //
        }
    }
}
