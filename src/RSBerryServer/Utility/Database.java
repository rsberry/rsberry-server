package RSBerryServer.Utility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Database
{
    private static Connection connection = null;

    public static Connection getInstance()
    {
        if (connection == null) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://" + Setting.get("DBHostname") + ":" + Setting.get("DBPort") + "/" + Setting.get("DBDatabase"),
                        Setting.get("DBUsername"),
                        Setting.get("DBPassword")
                );
                connection.setAutoCommit(true);
                connection.setReadOnly(false);
            } catch (ClassNotFoundException cnfe) {
                System.out.println("Can't find your MySQL driver.");
                System.exit(1);
            } catch (SQLException sqle) {
                System.out.println(sqle.getMessage());
                System.out.println(sqle.getSQLState());
                System.out.println(sqle.getErrorCode());
                System.exit(1); // Die!
            }
        }

        return connection;
    }
}
