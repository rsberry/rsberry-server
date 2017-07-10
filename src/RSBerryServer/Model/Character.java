package RSBerryServer.Model;

import RSBerryServer.Except.BadLogin;
import RSBerryServer.Except.CharacterBanned;
import RSBerryServer.Except.CharacterNotFound;
import RSBerryServer.Legacy.Stream;
import RSBerryServer.Utility.BCrypt;
import RSBerryServer.Utility.Database;

import java.sql.*;

public class Character
{
    private Integer id = null;
    private String username = null;
    private String password = null;
    private Integer x = null;
    private Integer y = null;
    private Integer banned = null;
    private Integer height = null;
    private Integer rights = null;
    private Integer member = null;
    private String last_connection = null;
    private String last_login = null;
    private Integer energy = null;
    private Integer game_time = null;
    private Integer game_count = null;
    private Integer look_0 = null;
    private Integer look_1 = null;
    private Integer look_2 = null;
    private Integer look_3 = null;
    private Integer look_4 = null;
    private Integer look_5 = null;

    public static Character read(int id) throws CharacterNotFound
    {
        Character instance = new Character();

        Connection connection = Database.getInstance();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM `users` WHERE `id` = ? LIMIT 1;");
            statement.setInt(1, id);
            statement.execute();
            ResultSet results = statement.getResultSet();
            results.last();
            int count = results.getRow();
            if (count < 1) {
                throw new CharacterNotFound("Character with ID '" + id + "' doesn't exist.");
            }
            results.first();
            instance.id = results.getInt("id");
            instance.username = results.getString("username");
            instance.password = results.getString("password");
            instance.x = results.getInt("x");
            instance.y = results.getInt("y");
            instance.height = results.getInt("height");
            instance.rights = results.getInt("rights");
            instance.banned = results.getInt("banned");
            instance.member = results.getInt("member");
            instance.last_connection = results.getString("last_connection");
            instance.last_login = results.getString("last_login");
            instance.energy = results.getInt("energy");
            instance.game_time = results.getInt("game_time");
            instance.game_count = results.getInt("game_count");
            instance.look_0 = results.getInt("look_0");
            instance.look_1 = results.getInt("look_1");
            instance.look_2 = results.getInt("look_2");
            instance.look_3 = results.getInt("look_3");
            instance.look_4 = results.getInt("look_4");
            instance.look_5 = results.getInt("look_5");
        } catch (SQLException sqle) {
            System.out.println(sqle.getMessage());
            System.exit(1);
        }

        return instance;
    }

    public static Character readUsingCredentials(String username, String password) throws CharacterNotFound, CharacterBanned, BadLogin
    {
        Character character = null;
        Connection connection = Database.getInstance();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT `id` FROM `users` WHERE `username` = ? LIMIT 1;");
            statement.setString(1, username);
            statement.execute();
            ResultSet results = statement.getResultSet();
            results.last();
            int count = results.getRow();
            if (count < 1) {
                throw new CharacterNotFound("Character with username '" + username + "' doesn't exist.");
            }
            results.first();

            character = read(results.getInt("id"));
            if (character.isBanned()) {
                throw new CharacterBanned("This character has been banned.");
            }

            if (!BCrypt.checkpw(password, character.getPassword())) {
                throw new BadLogin("Bad username/password combination");
            }

            return character;
        } catch (SQLException sqle) {
            System.out.println(sqle.getMessage());
            System.exit(1);
        }

        return character;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public boolean isAdmin()
    {
        if (rights >= 2) {
            return true;
        }
        return false;
    }

    public boolean isMod()
    {
        if (rights >= 1) {
            return true;
        }
        return false;
    }

    public boolean isBanned()
    {
        if (banned >= 1) {
            return true;
        }
        return false;
    }

    public Integer getId() {
        return id;
    }

    public Integer getX() {
        return x;
    }

    public Integer getY() {
        return y;
    }

    public Integer getHeight() {
        return height;
    }

    public boolean isMember() {
        if (member >= 1) {
            return true;
        }
        return false;
    }

    public String getLastConnection() {
        return last_connection;
    }

    public String getLastLogin() {
        return last_login;
    }

    public Integer getEnergy() {
        return energy;
    }

    public Integer getGameTime() {
        return game_time;
    }

    public Integer getGameCount() {
        return game_count;
    }

    public Integer getLook0() {
        return look_0;
    }

    public Integer getLook1() {
        return look_1;
    }

    public Integer getLook2() {
        return look_2;
    }

    public Integer getLook3() {
        return look_3;
    }

    public Integer getLook4() {
        return look_4;
    }

    public Integer getLook5() {
        return look_5;
    }
}
