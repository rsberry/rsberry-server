package RSBerryServer.Except;

public class AlreadyLoggedIn extends Exception
{
    public AlreadyLoggedIn(String message)
    {
        super(message);
    }
}
