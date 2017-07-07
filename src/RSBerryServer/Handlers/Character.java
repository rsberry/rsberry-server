package RSBerryServer.Handlers;

import RSBerryServer.Except.*;
import RSBerryServer.Game;

public class Character
{
    public static RSBerryServer.Model.Character characters[] = new RSBerryServer.Model.Character[Game.MAX_PLAYERS];

    public void process()
    {

    }

    public static int login(String username, String password) throws NoCharacterSlot, AlreadyLoggedIn, CharacterNotFound, CharacterBanned, BadLogin
    {
        int slot;

        slot = findSlotByUsername(username);
        if (slot != -1) {
            throw new AlreadyLoggedIn("This user is already logged in!");
        }

        RSBerryServer.Model.Character character = RSBerryServer.Model.Character.readUsingCredentials(username, password);

        slot = findNextSlot();

        characters[slot] = character;

        return slot;
    }

    public static int findNextSlot() throws NoCharacterSlot
    {
        for (int i = 0; i < characters.length; i++) {
            if (characters[i] == null) {
                return i;
            }
        }
        throw new NoCharacterSlot("There're no slots available for this character!");
    }

    public static int findSlotByUsername(String username)
    {
        for (int i = 0; i < characters.length; i++) {
            if (characters[i] == null) {
                continue;
            }
            if (characters[i].getUsername() == username) {
                return i;
            }
        }
        return -1;
    }
}
