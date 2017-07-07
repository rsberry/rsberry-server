package RSBerryServer;

import RSBerryServer.Model.Character;
import RSBerryServer.Except.AlreadyLoggedIn;
import RSBerryServer.Except.BadLogin;
import RSBerryServer.Except.CharacterBanned;
import RSBerryServer.Except.CharacterNotFound;
import RSBerryServer.Handler.ClientHandler;
import RSBerryServer.Legacy.Cryption;
import RSBerryServer.Legacy.Stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client
{
    private static final int LOGIN_RESPONSE = 0;

    private static final int LOGIN_TYPE_NEW = 16;
    private static final int LOGIN_TYPE_RECONNECT = 18;

    private static final int RETURN_CODE_WAIT_2 = 1;
    private static final int RETURN_CODE_SUCCESS = 2;
    private static final int RETURN_CODE_BAD_LOGIN = 3;
    private static final int RETURN_CODE_BANNED = 4;
    private static final int RETURN_CODE_ALREADY_ONLINE = 5;
    private static final int RETURN_CODE_GAME_UPDATED = 6;
    private static final int RETURN_CODE_WORLD_FULL = 7;
    private static final int RETURN_CODE_LOGIN_SERVER_OFFLINE = 8;
    private static final int RETURN_CODE_INVALID_WORLD = 10;
    private static final int RETURN_CODE_UPDATING = 14;
    private static final int RETURN_CODE_ACCOUNT_REQUIRED = 20;

    public Socket socket = null;
    private InputStream in = null;
    private OutputStream out = null;
    private Stream inStream = null;
    private Stream outStream = null;
    private int character_slot = -1;
    private int buffer_size = 1000000;
    private Character character = null;
    private Cryption inStreamDecryption = null;
    private Cryption outStreamDecryption = null;

    public Client(Socket s, int slot)
    {
        socket = s;
        character_slot = slot;

        System.out.println("Connection accepted from ");
        inStream = new Stream(new byte[buffer_size]);
        inStream.currentOffset = 0;
        outStream = new Stream(new byte[buffer_size]);
        outStream.currentOffset = 0;
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException ioe) {
            // Not even sure what'd cause this
            System.out.println("Could not get IO stream.");
        }

        long server_session_key = 0;
        long client_session_key = 0;

        // Let's generate a session key for encryption
        server_session_key = ((long)(java.lang.Math.random() * 99999999D) << 32) + (long) (java.lang.Math.random() * 99999999D);

        try {
            fillInStream(2);
            if (inStream.readUnsignedByte() != 14) {
                System.out.println("Expected login packet ID of 14.");
                ClientHandler.kick(slot);
                return;
            }

            int name_part = inStream.readUnsignedByte();

            for (int i = 0; i < 8; i++) {
                out.write(0);
            }

            // Send login response
            out.write(LOGIN_RESPONSE);

            // Send the server part of the session key
            outStream.writeQWord(server_session_key);

            directFlushOutStream();

            fillInStream(2);

            int loginType = inStream.readUnsignedByte();

            if (loginType != LOGIN_TYPE_NEW && loginType != LOGIN_TYPE_RECONNECT) {
                System.out.println("Unexpected login type " + loginType);
                ClientHandler.kick(slot);
                return;
            }

            int login_packet_size = inStream.readUnsignedByte();
            int login_encrypt_packet_size = login_packet_size - (36 + 1 + 1 + 2);

            if (login_encrypt_packet_size <= 0) {
                System.out.println("Cannot have a 0 packet size.");
                ClientHandler.kick(slot);
                return;
            }

            fillInStream(login_packet_size);

            if (inStream.readUnsignedByte() != 255 || inStream.readUnsignedWord() != 317) {
                System.out.println("This packet ID must be 255 of 317.");
                ClientHandler.kick(slot);
                return;
            }

            int low_memory_version = inStream.readUnsignedByte();

            for (int i = 0; i < 9; i++) {
                String junk = Integer.toHexString(inStream.readDWord());
            }

            login_encrypt_packet_size--;

            int tmp = inStream.readUnsignedByte();

            if (login_encrypt_packet_size != tmp) {
                ClientHandler.kick(slot);
                System.out.println("Encrypted packet data is not what they said it would be.");
                return;
            }

            tmp = inStream.readUnsignedByte();

            if (tmp != 10) {
                ClientHandler.kick(slot);
                System.out.println("Expected a packet ID of 10");
                return;
            }

            client_session_key= inStream.readQWord();
            server_session_key = inStream.readQWord();

            int junk = inStream.readDWord();

            String username = inStream.readString();
            String password = inStream.readString();
            try {
                String server = inStream.readString();
            } catch (Exception e) {
                String server = "default.com";
                // TODO Do something with this
            }

            username = username.toLowerCase();
            username = username.replaceAll(" ", "_");
            username = username.replaceAll("'", "");

            int session_key[] = new int[4];
            session_key[0] = (int) (client_session_key >> 32);
            session_key[1] = (int) client_session_key;
            session_key[2] = (int) (server_session_key >> 32);
            session_key[3] = (int) server_session_key;

            inStreamDecryption = new Cryption(session_key);
            outStreamDecryption = new Cryption(session_key);

            outStream.packetEncryption = outStreamDecryption;

            try {
                login(username, password);
                out.write(RETURN_CODE_SUCCESS);
            } catch (AlreadyLoggedIn ali) {
                out.write(RETURN_CODE_ALREADY_ONLINE);
                ClientHandler.kick(slot);
                return;
            } catch (BadLogin bl) {
                out.write(RETURN_CODE_BAD_LOGIN);
                ClientHandler.kick(slot);
                return;
            } catch (CharacterBanned cb) {
                out.write(RETURN_CODE_BANNED);
                ClientHandler.kick(slot);
                return;
            } catch (CharacterNotFound cnf) {
                out.write(RETURN_CODE_ACCOUNT_REQUIRED);
                ClientHandler.kick(slot);
                return;
            }

            int player_rights = 0;
            if (character.isAdmin()) {
                player_rights = 2;
            } else if (character.isMod()) {
                player_rights = 1;
            }

            out.write(player_rights);
            out.write(0);

        } catch (IOException ioe) {
            ClientHandler.kick(slot);
            System.out.println("Unknown error");
            return;
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

    public void parseIncomingPackets()
    {
        try {
            int available = in.available();
            if (available == 0) {
                return;
            }
            int packet_type = in.read() & 0xff;

        } catch (IOException ioe) {
            //
        }
    }

    public void login(String username, String password) throws AlreadyLoggedIn, BadLogin, CharacterNotFound, CharacterBanned
    {
        character = Character.readUsingCredentials(username, password);
    }
}
