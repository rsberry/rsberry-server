package RSBerryServer.Model;

import RSBerryServer.Except.BadLogin;
import RSBerryServer.Except.CharacterBanned;
import RSBerryServer.Except.CharacterNotFound;

public class Character
{
    private String username = "sam";

    public static Character readUsingCredentials(String username, String password) throws CharacterNotFound, CharacterBanned, BadLogin
    {
//        throw new CharacterNotFound("No character with that username recognised.");
        return new Character();
    }

    public String getUsername()
    {
        return username;
    }

    public boolean isAdmin()
    {
        return true;
    }

    public boolean isMod()
    {
        return true;
    }
}
