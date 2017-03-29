import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Peter on 3/17/2017.
 */
public class Test {
    public static void main(String[] args){
        ArrayList<Integer> parent1 = new ArrayList<>(Arrays.asList(2, 4, 5, 1, 6, 7, 9, 8, 0, 3));
        ArrayList<Integer> parent2 = ICA.twoOptRandom(parent1);
        System.out.println(parent1 + " Cost: " + ICA.computeTourLength(parent1));
        System.out.println(parent2 + " Cost: " + ICA.computeTourLength(parent2));


    }
}
