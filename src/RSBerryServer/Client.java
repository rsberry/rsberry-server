package RSBerryServer;

import RSBerryServer.Except.*;
import RSBerryServer.Model.Character;
import RSBerryServer.Handler.ClientHandler;
import RSBerryServer.Legacy.Cryption;
import RSBerryServer.Legacy.Stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client
{
    // Login responses
    private static final int LOGIN_RESPONSE = 0;

    // Login types
    // TODO, I think this is a packet id so we might be able to merge the whole logging in process with generic packet handling
    private static final int LOGIN_TYPE_NEW = 16;
    private static final int LOGIN_TYPE_RECONNECT = 18;

    // Return codes
    private static final int RETURN_CODE_WAIT_2_SECONDS = 1;
    private static final int RETURN_CODE_SUCCESS = 2;
    private static final int RETURN_CODE_INVALID_USERNAME_PASSWORD = 3;
    private static final int RETURN_CODE_ACCOUNT_DISABLED = 4;
    private static final int RETURN_CODE_ALREADY_ONLINE = 5;
    private static final int RETURN_CODE_GAME_UPDATED = 6;
    private static final int RETURN_CODE_WORLD_FULL = 7;
    private static final int RETURN_CODE_LOGIN_SERVER_OFFLINE = 8;
    private static final int RETURN_CODE_LOGIN_LIMIT_EXCEEDED = 9; // If you connect from the same address too many times
    private static final int RETURN_CODE_INVALID_SESSION_ID = 10;
    private static final int RETURN_CODE_SESSION_REJECTED = 11;
    private static final int RETURN_CODE_MEMBERS_ONLY = 12;
    private static final int RETURN_CODE_COULD_NOT_COMPLETE_LOGIN = 13;
    private static final int RETURN_CODE_UPDATING = 14;
    private static final int RETURN_CODE_LOGIN_ATTEMPTS_EXCEEDED = 16; // You've tried to log in too much, prevent brute forcing
    private static final int RETURN_CODE_MEMBERS_ONLY_AREA = 17;
    private static final int RETURN_CODE_INVALID_LOGIN_SERVER = 20;
    private static final int RETURN_CODE_ONLY_JUST_LEFT_ANOTHER_WORLD = 21;

    // List of packet sizes. Lifted from Moparscape
    // TODO I'm not 100% but there's almost certainly a nicer way to store these. This is an eyesore and a code smell.
    public static final int PACKET_SIZES[] = {
        0, 0, 0, 1, -1, 0, 0, 0, 0, 0, //0
        0, 0, 0, 0, 8, 0, 6, 2, 2, 0,  //10
        0, 2, 0, 6, 0, 12, 0, 0, 0, 0, //20
        0, 0, 0, 0, 0, 8, 4, 0, 0, 2,  //30
        2, 6, 0, 6, 0, -1, 0, 0, 0, 0, //40
        0, 0, 0, 12, 0, 0, 0, 0, 8, 0, //50
        0, 8, 0, 0, 0, 0, 0, 0, 0, 0,  //60
        6, 0, 2, 2, 8, 6, 0, -1, 0, 6, //70
        0, 0, 0, 0, 0, 1, 4, 6, 0, 0,  //80
        0, 0, 0, 0, 0, 3, 0, 0, -1, 0, //90
        0, 13, 0, -1, 0, 0, 0, 0, 0, 0,//100
        0, 0, 0, 0, 0, 0, 0, 6, 0, 0,  //110
        1, 0, 6, 0, 0, 0, -1, 0, 2, 6, //120
        0, 4, 6, 8, 0, 6, 0, 0, 0, 2,  //130
        0, 0, 0, 0, 0, 6, 0, 0, 0, 0,  //140
        0, 0, 1, 2, 0, 2, 6, 0, 0, 0,  //150
        0, 0, 0, 0, -1, -1, 0, 0, 0, 0,//160
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  //170
        0, 8, 0, 3, 0, 2, 0, 0, 8, 1,  //180
        0, 0, 12, 0, 0, 0, 0, 0, 0, 0, //190
        2, 0, 0, 0, 0, 0, 0, 0, 4, 0,  //200
        4, 0, 0, 0, 7, 8, 0, 0, 10, 0, //210
        0, 0, 0, 0, 0, 0, -1, 0, 6, 0, //220
        1, 0, 0, 0, 6, 0, 6, 8, 1, 0,  //230
        0, 4, 0, 0, 0, 0, -1, 0, -1, 4,//240
        0, 0, 6, 6, 0, 0, 0            //250
    };

    public Socket socket = null; // Container for socket

    // IO stuff, I sleep well not knowing precisely how this works
    private InputStream in = null;
    private OutputStream out = null;
    private Stream inStream = null;
    private Stream outStream = null;
    // Encryption/decryption for the above
    private Cryption inStreamDecryption = null;
    private Cryption outStreamDecryption = null;

    public int slot = 0; // The slot the client occupying
    private int buffer_size = 1000000; // Amount of bytes in the buffer IIRC
    public Character character = null; // The character they're logged in as
    private int packet_type = 0; // Latest packet type
    private int packet_size = 0; // Latest packet size
    public boolean initialised = false;

    public Client(Socket s, int slots)
    {
        socket = s;
        slot = slots;
    }

    public void run()
    {
        // Tell the console we've got a bite
        System.out.println("Connection accepted from " + socket.getRemoteSocketAddress().toString() + " in slot " + slot);

        // Create new streams with the buffer size
        inStream = new Stream(new byte[buffer_size]);
        inStream.currentOffset = 0;
        outStream = new Stream(new byte[buffer_size]);
        outStream.currentOffset = 0;

        try {
            // Create I/O streams
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException ioe) {
            // Not even sure what'd cause this
            System.out.println("Could not get IO stream.");
        }

        // Create server/client keys for encryption
        long server_session_key;
        long client_session_key;

        // Let's generate a session key for encryption
        server_session_key = ((long)(java.lang.Math.random() * 99999999D) << 32) + (long) (java.lang.Math.random() * 99999999D);

        try {
            // Get the first 2 bytes from the data coming in
            fillInStream(2);

            // If the first byte isn't 14, what the fuck are they trying to do exactly?
            if (inStream.readUnsignedByte() != 14) {
                System.out.println("Expected login packet ID of 14.");
                // We don't wanna deal with them
                ClientHandler.kick(slot);
                return;
            }

            // I *think* this is a hash of part of the username...
            // Rumor has it this was used to "randomly" pick a world to connect to
            // I think that's bullshit.
            // Either way, it's not used
            int name_part = inStream.readUnsignedByte();

            // We spew back a load of junk, but the client loves this shit
            for (int i = 0; i < 8; i++) {
                out.write(0);
            }

            // Send login response
            out.write(LOGIN_RESPONSE);

            // Send the server part of the session key
            outStream.writeQWord(server_session_key);

            // Flush what we've written to the client thus far
            directFlushOutStream();

            // Grab the next 2 bytes
            fillInStream(2);

            // The first byte is the login type
            int loginType = inStream.readUnsignedByte();

            // We basically handle them both the same
            if (loginType != LOGIN_TYPE_NEW && loginType != LOGIN_TYPE_RECONNECT) {
                System.out.println("Unexpected login type " + loginType);
                ClientHandler.kick(slot);
                return;
            }

            // Grab the packet size
            int login_packet_size = inStream.readUnsignedByte();

            int login_encrypt_packet_size = login_packet_size - (36 + 1 + 1 + 2);

            // After it's decrypted is it 0? (I think)
            if (login_encrypt_packet_size <= 0) {
                System.out.println("Cannot have a 0 packet size.");
                ClientHandler.kick(slot);
                return;
            }

            // Get ready to send it back
            fillInStream(login_packet_size);

            // The next byte and next word (actually, it's a "short") must be 255 and 317 respectively
            // This is the 'magic' number and the client version they're running
            // If we decide to do our own client, this is where we could throw back a "server updated" response
            if (inStream.readUnsignedByte() != 255 || inStream.readUnsignedWord() != 317) {
                System.out.println("This packet ID must be 255 of 317.");
                // If they're not gonna play by our rules, they can fuck off
                ClientHandler.kick(slot);
                return;
            }

            // Are they running low memory?
            // 0 = yes
            // 1 = no
            // We probably don't care, times have moved on an we can handle it.
            int low_memory_version = inStream.readUnsignedByte();

            // We fire back some "junk", but apparently these are do with CRC32 values or something
            for (int i = 0; i < 9; i++) {
                String junk = Integer.toHexString(inStream.readDWord());
            }

            login_encrypt_packet_size--;

            // Grab the next byte
            int tmp = inStream.readUnsignedByte();

            // If that value isn't equal to the packet size tell them to go away
            // Fun fact: This is basically what the heartbleed bug was. People gave a packet size bigger than what
            // they wanted and received extra information in return
            if (login_encrypt_packet_size != tmp) {
                System.out.println("Encrypted packet data is not what they said it would be.");
                ClientHandler.kick(slot);
                return;
            }

            // Nobody seems to know what this is, LOL. If it's not 10, go away
            tmp = inStream.readUnsignedByte();

            // It might be something to with world ID? I dunno
            if (tmp != 10) {
                System.out.println("Expected a packet ID of 10");
                ClientHandler.kick(slot);
                return;
            }

            // Right. Receive the client session key and the server session key back
            client_session_key = inStream.readQWord();
            server_session_key = inStream.readQWord();

            // This is apparently the user_id but it's not clear how the client would have this...
            // Unless... it's the UID!?
            // TODO Find out if UID
            int junk = inStream.readDWord();

            // Read the username and password
            String username = inStream.readString();
            String password = inStream.readString();

            // This seems to be the server ID but we don't use it...
            try {
                String server = inStream.readString();
            } catch (Exception e) {
                String server = "default.com";
                // TODO Do something with this
            }

            // Okay, format the usernames
            username = username.toLowerCase();
            username = username.replaceAll(" ", "_");
            username = username.replaceAll("'", "");

            // Store the session keys for encrypting and decrypting
            int session_key[] = new int[4];
            session_key[0] = (int) (client_session_key >> 32);
            session_key[1] = (int) client_session_key;
            session_key[2] = (int) (server_session_key >> 32);
            session_key[3] = (int) server_session_key;
            inStreamDecryption = new Cryption(session_key);
            outStreamDecryption = new Cryption(session_key);
            outStream.packetEncryption = outStreamDecryption;

            // Let's send back a response code
            try {
                // Try to log in
                login(username, password);
            } catch (AlreadyLoggedIn ali) {
                // They're already logged in, get rid of them
                out.write(RETURN_CODE_ALREADY_ONLINE);
                ClientHandler.kick(slot);
                return;
            } catch (BadLogin bl) {
                // Bas username password, get rid of them
                out.write(RETURN_CODE_INVALID_USERNAME_PASSWORD);
                ClientHandler.kick(slot);
                return;
            } catch (CharacterBanned cb) {
                // They were banned, get rid of them
                out.write(RETURN_CODE_ACCOUNT_DISABLED);
                ClientHandler.kick(slot);
                return;
            } catch (CharacterNotFound cnf) {
                // Character doesn't exist, get rid of them
                out.write(RETURN_CODE_INVALID_USERNAME_PASSWORD);
                ClientHandler.kick(slot);
                return;
            }

            // Oh, it went well? Send them a response.
            out.write(RETURN_CODE_SUCCESS);

            // Tell them if they have privilege
            int player_rights = 0;
            if (character.isAdmin()) {
                player_rights = 2;
            } else if (character.isMod()) {
                player_rights = 1;
            }
            out.write(player_rights);

            // If they're suspicious, set this to 1. It means information about their actions will be sent
            // to the server. As this is RSPS, we might not care and can probably do without the extra packet
            // handling to be honest. Maybe review it later.
            out.write(0);

        } catch (IOException ioe) {
            // If anything weird happens, just kick them
            System.out.println("Unknown error");
            ClientHandler.kick(slot);
        }
    }

    public void fillInStream(int length) throws IOException
    {
        inStream.currentOffset = 0;
        in.read(inStream.buffer, 0, length);
    }

    public void directFlushOutStream() throws IOException
    {
        out.write(outStream.buffer, 0, outStream.currentOffset);
        outStream.currentOffset = 0; // reset
    }

    public void process()
    {
        // This is where we could do some other stuff

        parseIncomingPackets();
    }

    private void parseIncomingPackets()
    {
        try {
            if (in == null) return;

            int available_packet = in.available();
            if (available_packet == 0) return;

            if (packet_type == -1) {
                packet_type = in.read() & 0xff;
                if (inStreamDecryption != null)
                    packet_type = packet_type - inStreamDecryption.getNextKey() & 0xff;
                packet_size = PACKET_SIZES[packet_type];
                available_packet--;
            }
            if (packet_size == -1) {
                if (available_packet > 0) {
                    // this is a variable size packet, the next byte containing the length of said
                    packet_size = in.read() & 0xff;
                    available_packet--;
                } else return;
            }
            if (available_packet < packet_size) return;    // packet not completely arrived here yet

            fillInStream(packet_size);

            int i;
            int junk1;
            int junk2;
            int junk3;

            switch (packet_type) {
                case 0:
                    // Idle timer
                    break;

                case 202:
                    // Logout packet
                    break;

                default:
                    System.out.println("Unknown packet id '" + packet_type + "'.");
                    break;
            }

        } catch (IOException ioe) {
            //
        }
    }

    public void login(String username, String password) throws AlreadyLoggedIn, BadLogin, CharacterNotFound, CharacterBanned
    {
        System.out.println("Logging in with " + username + " " + password);
        // See if the user is already logged in
        int i = ClientHandler.getClientByUsername(username);
        if (i > -1) {
            // They are, which means they didn't lo0g out properly.
            throw new AlreadyLoggedIn("That character is already logged in");
        }
        // Otherwise set this client's character.
        character = Character.readUsingCredentials(username, password);
    }

    public boolean isLoggedIn()
    {
        // Basically, if we've assigned a character to this client, they're logged in
        if (character == null) {
            return false;
        }

        return true;
    }

    public void initialise()
    {
        // Send membership and player id first
        outStream.createFrame(249);
        if (character.isMember()) {
            outStream.writeByteA(1);
        } else {
            outStream.writeByteA(0);
        }
        outStream.writeWordBigEndianA(slot);

        // Lastly flag as initialised
        initialised = true;
    }
}
