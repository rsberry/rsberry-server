package RSBerryServer.Model;

import RSBerryServer.Except.NPCNotFound;
import RSBerryServer.Utility.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class NPC
{
    private Integer id;
    private String name;
    private Integer combat;
    private Integer health;

    public static NPC read(int id) throws NPCNotFound
    {
        Connection connection = Database.getInstance();

        NPC instance = new NPC();
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM npc WHERE id = ? LIMIT 1;");
            statement.setInt(1, id);
            statement.execute();
            ResultSet results = statement.getResultSet();
            results.last();
            if (results.getRow() < 1) {
                throw new NPCNotFound("An NPC with id '" + id + "' could not be found");
            }
            results.first();
            instance.id = results.getInt("id");
            instance.name = results.getString("name");
            instance.combat = results.getInt("combat");
            instance.health = results.getInt("health");
        } catch (SQLException sqle) {
            System.out.println("Something failed when looking for an NPC");
            System.exit(1);
        }

        return instance;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getCombat() {
        return combat;
    }

    public Integer getHealth() {
        return health;
    }
}
