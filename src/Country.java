import java.util.ArrayList;

/**
 * Created by Peter on 3/7/2017.
 */
public class Country implements Comparable<Country> {
    private ArrayList<Integer> tour;
    private double cost;
    private double normCost;
    private int id;

    public Country(ArrayList<Integer> tour, int i) {
        this.tour = tour;
        this.cost = ICA.computeTourLength(tour);
        id = i;
    }

    public double getNormCost() {
        return normCost;
    }

    public void setNormCost(double normCost) {
        this.normCost = normCost;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getCost() {
        return cost;
    }

    /*public void setCost(double cost) {
        this.cost = cost;
    }*/


    public ArrayList<Integer> getTour() {
        return tour;
    }

    public void setTour(ArrayList<Integer> tour) {
        this.tour = tour;
        cost = ICA.computeTourLength(tour);
    }

    @Override
    public int compareTo(Country o) {
        if (cost > o.getCost()) return 1;
        if (cost < o.getCost()) return -1;
        return 0;
    }

    public int size(){
        return tour.size();
    }

    public String toString() {
        return "\n" + tour.toString() + "\nCost: " + cost;
    }
}
