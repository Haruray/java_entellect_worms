package za.co.entelect.challenge.command;

public class SelectCommand implements Command {

    private final int x;
    private final String y;

    public SelectCommand(int x, String y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String render() {
        return String.format("select %d;"+y, x);
    }
}