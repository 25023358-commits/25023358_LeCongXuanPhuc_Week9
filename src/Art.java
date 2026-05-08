// Lớp Art kế thừa Item theo Factory Pattern
package src;
public class Art extends Item {
    private String artistName;

    public Art(String id, String name, double startingPrice, String artistName) {
        super(id, name, startingPrice);
        this.artistName = artistName;
    }

    @Override
    public void printInfo() {
        System.out.println("[Art] " + getName() + " by " + artistName + " | Start: $" + getStartingPrice() );
    }
}