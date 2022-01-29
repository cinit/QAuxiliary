package mqq.app;

public class AccountNotMatchException extends Exception {

    public AccountNotMatchException(String current, String old) {
        super("The current account is " + current + " instead of " + old);
    }
}
