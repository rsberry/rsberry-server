package RSBerryServer.Handler;

import RSBerryServer.Client;
import RSBerryServer.Except.NoEmptySlot;
import RSBerryServer.Utility.Setting;

import java.io.IOException;
import java.net.Socket;

public class ClientHandler
{
    private static Client clients[] = new Client[Setting.getMaxPlayers()];

    /**
     * Basically just a placehold constructor for now
     */
    public ClientHandler()
    {

    }

    /**
     * This gets called every 600ms
     * It'll be where all the main processing happens
     */
    public void process()
    {
        // For each slot
        for (int i = 0; i < Setting.getMaxPlayers(); i++) {
            // If the slot is empty
            if (clients[i] == null) {
                continue;
            }

            // Process the client in the slot
            clients[i].process();
        }
    }

    /**
     * The client connected, the server is requesting for them to join
     */
    public static Client request(Socket socket) throws NoEmptySlot
    {
        // Find the next empty slot. There might be none
        int next_slot = nextAvailableSlot();

        // Create a client class with the socket they're talking on and the slot they're in
        Client client = new Client(socket, next_slot);

        // Put the client in the slot
        clients[next_slot] = client;

        // Return the slot (which is basically the client)
        return clients[next_slot];
    }

    /**
     * This method is basically here to find slots that don't have players in
     */
    public static int nextAvailableSlot() throws NoEmptySlot
    {
        // For each slot
        for (int i = 0; i < Setting.getMaxPlayers(); i++) {
            // Is it empty?
            if (clients[i] == null) {
                // Return the slot number
                return i;
            }
        }
        // If got to the end without finding an empty slot. Throw a wobbly.
        throw new NoEmptySlot("All the players slots a occupied!");
    }

    public static void kick(int slot)
    {
        try {
            // TODO We might do emergency game saving here
            // Close the socket the client is talking on
            clients[slot].socket.close();
            // Empty the slot they're in
            clients[slot] = null;
        } catch (IOException ioe) {
            // Closing the socket will throw an exception. It's kind of okay. We don't need to do anything.
        }
    }

    /**
     * Basically go through all the slots and find one with a certain player name
     */
    public static int getClientByUsername(String username)
    {
        // For each slot
        for (int i = 0; i < clients.length; i++) {
            // Is the slot empty?
            if (clients[i] == null) {
                // We don't care then
                continue;
            }
            // Is the slot occupied?
            if (clients[i].isLoggedIn()) {
                // Intriguing... is the occupant the user we're looking for?
                if (clients[i].character.getUsername().equals(username)) {
                    // It is?! Return that ID
                    return i;
                }
            }
        }
        // The player wasn't in any slot...
        // TODO Maybe throw an exception here. But the thing is, it's not a breach on contract, they might not be logged in...
        return -1;
    }
}
