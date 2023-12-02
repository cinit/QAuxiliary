package top.linl.util;

public class ReflectException extends RuntimeException {

    private Exception otherExceptions;

    public ReflectException() {
        super();
    }

    public ReflectException(String content) {
        super(content);
    }

    public ReflectException(String content, Exception e) {
        super(content);
        this.otherExceptions = e;
    }

    public boolean hasOtherExceptions() {
        return otherExceptions != null;
    }

    public Exception getOtherExceptions() {
        return this.otherExceptions;
    }
}
