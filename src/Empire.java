import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

/**
 * Created by Peter on 3/15/2017.
 */
public class Empire implements Comparable<Empire> {
    private Country imperialist;
    private ArrayList<Country> colonies;
    private double totalNormalCost;
    private double totalCost;
    public Empire(Country imperialist, ArrayList<Country> colonies) {
        this.imperialist = imperialist;
        this.colonies = colonies;
    }

    public double getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public void computeTotalCost() {
        double sum = imperialist.getCost();
        for (Country c : colonies) {
            sum += c.getCost();
        }

        totalCost = sum;
    }

    public Country getImperialist() {
        return imperialist;
    }

    public void setImperialist(Country imperialist) {
        this.imperialist = imperialist;
    }

    public ArrayList<Country> getColonies() {
        return colonies;
    }

    public void setColonies(ArrayList<Country> colonies) {
        this.colonies = colonies;
    }

    public double getTotalNormalCost() {
        return totalNormalCost;
    }

    public void setTotalNormalCost(double totalNormalCost) {
        this.totalNormalCost = totalNormalCost;
    }


    public boolean add(Country c){
        return colonies.add(c);
    }

    public Country remove(int index){
        return colonies.remove(index);
    }

    public boolean remove(Country c){
        return colonies.remove(c);
    }

    public int size(){
        return colonies.size();
    }

    public Country get(int index){
        return colonies.get(index);
    }

    public void sort(){
        Collections.sort(colonies);
    }

    public String toString() {
        return "\n\nImperialist: " + imperialist + "\n"
                + "Colonies:\n" + colonies;
    }

    public double getCost(){
        return imperialist.getCost();
    }

    @Override
    public int compareTo(Empire o) {
        return imperialist.compareTo(o.getImperialist());
    }
}
