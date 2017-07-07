package RSBerryServer.Except;

public class BadLogin extends Exception
{
    public BadLogin(String message)
    {
        super(message);
    }
}
