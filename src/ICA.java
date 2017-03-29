import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Dell on 11/3/2016.
 */
public class ICA {

    /**
     * Parameters
     **/
    private static int N_COUNTRIES = 250;
    private static int N_IMPERIALISTS = 20;
    private static final int N_COLONIES = N_COUNTRIES - N_IMPERIALISTS;
    private static final double REVOLUTION_RATE = 0.3;
    private static final int N_ITERATIONS = 200;
    private static final double UNITE_THRESHOLD = 10.0;
    public static ArrayList<Empire> empires = new ArrayList<>();
    private static int[][] costs = {    //Cities' distance settings
            {0, 13, 4, 6, 27, 1, 11, 3, 5, 6},
            {13, 0, 2, 2, 5, 6, 3, 4, 1, 1},
            {4, 2, 0, 4, 4, 2, 2, 9, 7, 9},
            {6, 2, 4, 0, 10, 11, 11, 14, 19, 17},
            {27, 5, 4, 10, 0, 22, 28, 18, 19, 33},
            {1, 6, 2, 11, 22, 0, 63, 15, 18, 10},
            {11, 3, 2, 11, 28, 63, 0, 17, 20, 20},
            {3, 4, 9, 14, 18, 15, 17, 0, 11, 10},
            {5, 1, 7, 19, 19, 18, 20, 11, 0, 10},
            {6, 1, 9, 17, 33, 10, 20, 10, 10, 0}
    };
    private static Random rand = new Random();
    private static double totalCost;
    private static double maxCost = Double.MIN_VALUE;
    private static int graphSize;
    private static ArrayList<Country> countries = new ArrayList<>(N_COUNTRIES);

    public static void main(String[] args) {
        costs = readFile("resources\\kroA100.xml", 100, "xml");
        graphSize = costs.length;
        //System.out.println("---------------- Distances ----------------");
        //System.out.println(Arrays.deepToString(costs));
        randomInit();
        for (int i = 0; i < N_ITERATIONS; i++) {
            for (Empire e: empires){
                e.getImperialist().setTour(twoOptLocalSearch(e.getImperialist().getTour()));
                for (int j = 0; j < e.size(); j++) {
                    e.get(j).setTour(assimilateOX1(e.getImperialist().getTour(), e.get(j).getTour()));
                }
            }
            //revolution();
            imperialisticCompetition();
            //unite();
            if (empires.size() == 1) break;
        }

       /* System.out.println("------------------------------ Testing -----------------------------");
        Empire e = empires.get(0);
        System.out.println(e.get(0));
        System.out.println(e.getImperialist());
        e.get(0).setTour(assimilateOX1(e.getImperialist().getTour(), e.get(0).getTour()));
        System.out.println(e.get(0));*/

        System.out.println("----------------------------- Results -----------------------------");
        Collections.sort(empires);
        for (Empire e: empires) System.out.println(e.getImperialist());
        //System.out.println(empires);
        System.out.println("\n\nBest tour: " + empires.get(0).getImperialist());
        System.out.println("\nNumber of empires: " + empires.size());


       /* ArrayList<Integer> parent1 = new ArrayList<>(Arrays.asList(2, 4, 5, 1, 6, 0, 3));
        ArrayList<Integer> parent2 = new ArrayList<>(Arrays.asList(0, 2, 1, 3, 6, 4, 5));
        System.out.println(parent1);
        System.out.println(parent2);
        ArrayList<Integer> offspring = assimilateOX1(parent1, parent2);
        System.out.println(offspring);*/
    }

    private static void randomInit() {

        boolean visited[][] = new boolean[N_COUNTRIES][graphSize];
        for (int i = 0; i < graphSize; i++) {
            Arrays.fill(visited[i], false);  // Boolean array to avoid duplicate countries within a colony
        }

        ArrayList<ArrayList<Integer>> tours = new ArrayList<>();
        for (int i = 0; i < N_COUNTRIES; i++) {
            tours.add(new ArrayList<>());
        }

        /* Build random countries (TSP instances) represented as TSP paths */
        for (int i = 0; i < N_COUNTRIES; i++) {
            for (int j = 0; j < graphSize; j++) {
                int next = rand.nextInt(graphSize);
                //System.out.println("next = " + next + ", j = " + j);
                //System.out.println(Arrays.deepToString(visited));
                if (next == i) {
                    --j;
                    continue;
                }
                while (visited[i][next]) {
                    next = rand.nextInt(graphSize);
                    //System.out.println("inwhile:\n next = " + next + ", j = " + j);
                }
                tours.get(i).add(next);
                visited[i][next] = true;
            }
        }
        for (int i = 0; i < N_COUNTRIES; i++) {
            Country c = new Country(tours.get(i), i);
            //System.out.println("Tour " + i + ": " + "Size: " + tours.get(i).size());
            countries.add(c);
        }
        //System.out.println("---------------- Countries ---------------");
        //System.out.println(countries);
        //System.out.println();


        /* From the generated countries, identify the N_Imperialists Imperialists */

        // Sum all costs and get total and maximum costs
        sumCosts();

        // Sort cost-to-country mappings by their costs to get the imperialists
        // Note: Java8 code
        //Map<Integer, Double> imperials  = sortByCost(costs);
        Collections.sort(countries);
        //System.out.println("---------------- Sorted Countries --------------------");
        //System.out.println(countries);
        ArrayList<Country> mutableCountries = new ArrayList<>(countries);
        TreeSet<Country> imperials = new TreeSet<>();
        for (int i = 0; i < N_IMPERIALISTS; i++) {
            imperials.add(mutableCountries.remove(0));
        }
        //System.out.println("---------------- Imperialists ----------------");
        //System.out.println(imperials);
        //System.out.println();

        /* Randomly assign countries to identified imperialists based on computed power */
        Map<Country, Integer> imperialistNoOfColonies = new TreeMap<>();

        double totalNormCost = 0;
        for (Country c : imperials) {
            double normCost = maxCost - c.getCost();
            c.setNormCost(normCost);  // Normalize costs for each colony
            totalNormCost += normCost;
        }

        for (Country c : imperials) {
            double power = Math.abs(c.getNormCost() / totalNormCost);
            //System.out.println("power = " + power + ", norm = " + e.getValue());
            int noCol = (int) Math.round(power * N_COLONIES);
            imperialistNoOfColonies.put(c, noCol);
        }

        //System.out.println("--------------- Number of colonies per imperialist --------------------");
        //System.out.println(imperialistNoOfColonies);
        //System.out.println("Number of colonies: " + imperialistNoOfColonies.values().stream()
                //.mapToInt(Integer::intValue).sum()); // Sum number of countries in each colony
        boolean assigned[] = new boolean[N_COUNTRIES];
        int count = 0;
        for (Map.Entry<Country, Integer> e : imperialistNoOfColonies.entrySet()) {
            ArrayList<Country> colonies = new ArrayList<>();
            int index;
            for (int i = 0; i < e.getValue(); i++) {
                while (assigned[index = rand.nextInt(N_COLONIES)])
                    ;  // Avoid assigning countries to more than one imperial
                colonies.add(mutableCountries.get(index));
                assigned[index] = true;
                count++;
                if (count == N_COLONIES) {
                    break;
                }
            }
            //System.out.println("colonies = " + colonies);
            Empire emp = new Empire(e.getKey(), colonies);
            empires.add(emp);
            //imperialistMappings.put(e.getKey(), colonies);
        }
        /* Imperialist and their colonies */
        //System.out.println("--------------------- Imperialists and their colonies ----------------------");
        //for (Empire e: empires) e.sort();
        //System.out.println(empires);

    }

    private static Map<Integer, Double> sortByCost(Map<Integer, Double> costs) {
        return costs.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(N_IMPERIALISTS)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    /**
     * Calculates the cost of a tour by summing the costs of all edges within it.
     * Closes the tour by adding the cost of the last node to the first one.
     *
     * @param tour The tour whose cost is to be calculated
     * @return Total cost of the tour
     */
    public static double computeTourLength(ArrayList<Integer> tour) {
        double tourLength = 0;
        for (int i = 0; i < tour.size() - 1; i++) {
            tourLength += costs[tour.get(i)][tour.get(i + 1)];
        }
        tourLength += costs[tour.get(tour.size() - 1)][tour.get(0)]; // Close tour;
        return tourLength;
    }

    private static void sumCosts() {
        for (int i = 0; i < N_COUNTRIES; i++) {
            double cost = countries.get(i).getCost();
            totalCost += cost;
            if (cost > maxCost)
                maxCost = cost;
        }
    }

    /**
     * Reads TSP instances from TSPLIB in XML file format
     *
     * @param filename name of XML file to be read
     * @return 2-dimensional symmetric cost array
     */
    private static int[][] readFile(String filename, int n, String type) {
        int graph[][] = new int[n][n];
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(filename)));

            // Skip first 14 irrelevant lines
            for (int i = 0; i < 14; i++) {
                br.readLine();
            }

            String s;
            int i = -1;
            while (br.ready()) {
                s = br.readLine();
                if(type.equals("xml")) {
                    int j = -1;
                    if (s.contains("<vertex>")) {
                        ++i;
                        do {
                            ++j;
                            if (i == j) {
                                graph[i][j] = -1;
                                continue;
                            }
                            s = br.readLine();
                            // No time to come up with a single check
                            if (s.contains("</vertex>"))
                                break;

                            String[] stringHolder = s.split("\"");
                            int cost = (int) (Double.valueOf(stringHolder[1]) + 0.5);
                            graph[i][j] = cost;
                        } while (!s.contains("</vertex>"));
                    }
                } else if(type.equals("tsp")){

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return graph;
    }

    public static double round(double d, int numbersAfterDecimalPoint) {
        double n = Math.pow(10, numbersAfterDecimalPoint);
        double d2 = d * n;
        long lon = (long) d2;
        lon = ((long) (d2 + 0.5) > lon) ? lon + 1 : lon;
        return (lon) / n;
    }

    public static ArrayList<Integer> assimilateOX1(ArrayList<Integer> imperial, ArrayList<Integer> colony) {
        int nCities = imperial.size();
        //System.out.println("nCities = " + nCities);
        int cutPoint1 = rand.nextInt(nCities);
        int cutPoint2 = rand.nextInt(nCities);
        while (cutPoint1 == cutPoint2) cutPoint2 = rand.nextInt(nCities);

        // Ensure the first cutPoint is greater than the second
        int tempCut = cutPoint1 > cutPoint2 ? cutPoint1 : cutPoint2;
        if (tempCut == cutPoint1) {
            cutPoint1 = cutPoint2;
            cutPoint2 = tempCut;
        }
        //System.out.println("Cutpoint1 :" + cutPoint1 + " Cutpoint2: " + cutPoint2);
        boolean[] assignedIndex = new boolean[nCities];
        boolean[] assignedValue = new boolean[nCities];
        //System.out.println("Lengths: " + assignedIndex.length + " " + assignedValue.length);
        //System.out.println("nCities: " + nCities);
        int added = 0;

        /* Copy elements between the two cut points of imperial into the offspring */
        ArrayList<Integer> offspring = new ArrayList<>(Collections.nCopies(nCities, 0));
        for (int i = cutPoint1; i <= cutPoint2; i++) {
            offspring.set(i, imperial.get(i));
            assignedIndex[i] = true;
            assignedValue[imperial.get(i)] = true;
            ++added;
        }

        //System.out.println(offspring);

        /* Copy elements from colony into empty positions in offspring, starting from cut point 2 */
        int i = (cutPoint2 + 1) % (nCities);
        int j = (cutPoint2 + 1) % (nCities);
        while (added < nCities) {
            if (!assignedIndex[i] && !assignedValue[colony.get(j)]) {
                offspring.set(i, colony.get(j));
                assignedIndex[i] = true;
                ++added;
                i = (i + 1) % nCities;  // Loop back to beginning if the offspring is not full and end is reached
            }
            j = (j + 1) % nCities;
        }

        return offspring;
    }

    public static void imperialisticCompetition() {
        // Get the maximum total cost among all empires
        double maxTotalCost = Double.MIN_VALUE;
        for (Empire emp : empires) {
            //Collections.sort(emp.getColonies());  // sort the colonies in each empire by their costs
            if(emp.size() == 0) {
                continue;
            }
            Country bestColony = emp.get(0);
            for (Country c: emp.getColonies()){
                if (c.compareTo(bestColony) < 0) bestColony = c;
            }
            if(emp.getImperialist().compareTo(bestColony) > 0){
                Country c = emp.getImperialist();
                emp.setImperialist(bestColony);
                emp.remove(bestColony);
                emp.add(c);
            }
            //Collections.sort(emp.getColonies());
            emp.computeTotalCost();  // recompute the total cost for each empire
            double sum = emp.getTotalCost();
            if (sum > maxTotalCost) maxTotalCost = sum;
        }

        // Calculate total norm cost for the entire universe, as well as the total norm costs for each empire
        double sumTotalNormCost = 0;
        for (Empire emp : empires) {
            double normCost = Math.abs(emp.getTotalCost() - maxTotalCost);
            emp.setTotalNormalCost(normCost);
            sumTotalNormCost += normCost;
        }

        // Calculate the possession probabilities for all empires
        double maxD = Double.MIN_VALUE;
        int maxIndex = 0;
        int empIndex = 0;
        for (Empire emp : empires) {
            double prob = emp.getTotalNormalCost() / sumTotalNormCost;
            double r = rand.nextDouble();
            double d = prob - r;
            if (d > maxD) {
                maxD = d;
                maxIndex = empIndex;
            }
            empIndex++;
        }

        // Imperial possession
        Empire maxEmpire = empires.get(maxIndex);
        //Collections.sort(empires);
        Empire weakestEmpire = empires.get(empires.size() - 1);
        for (Empire emp: empires){
            if (emp.compareTo(weakestEmpire) > 0) weakestEmpire = emp;
        }
        int weakestEmpireSize = weakestEmpire.size();
        if (weakestEmpireSize == 0) {
            empires.remove(weakestEmpire);
        } else {
            int weakestColonyIndex = 0;
            int i = 0;
            Country weakestColony = weakestEmpire.get(weakestColonyIndex);
            for (Country c: weakestEmpire.getColonies()){
                if (c.compareTo(weakestColony) > 0) {
                    weakestColony = c;
                    weakestColonyIndex = i;
                }
                ++i;
            }
            weakestEmpire.remove(weakestColonyIndex);
            maxEmpire.add(weakestColony);
        }

    }

    public static void revolution(){
        //System.out.println("------------------ Revolution ---------------------");
        // Formally invovled randomly swapping weak countries into other colonies.
       Collections.sort(empires);
        /*int nRevolution = (int) (REVOLUTION_RATE * N_COLONIES);
        if(nRevolution >= empires.size()) nRevolution = 1;
        for (int i = 0; i < nRevolution; i++) {
            int weakIndex = empires.size() - i - 1;
            Empire weakEmpire = empires.get(weakIndex);
            System.out.println("WeakIndex: " + weakIndex);
            if(weakEmpire.size() == 0) continue;
            Country c1 = weakEmpire.remove(rand.nextInt(weakEmpire.size()));
            Empire otherEmpire = empires.get(rand.nextInt(empires.size()));
            if (otherEmpire.size() == 0) continue;
            Country c2 = otherEmpire.remove(rand.nextInt(otherEmpire.size()));
            weakEmpire.add(c2);
            otherEmpire.add(c1);
        }*/
        //for (Empire e: empires) e.sort();
        //System.out.println(empires);

        // From literature, discovered it makes more sense for revolution to be some sort of local search, which, wrt
        // TSP, would be a two-opt move 'mutation'

        int nRevolution = (int) (REVOLUTION_RATE * N_COLONIES);
        for (int i = 0; i < nRevolution; i++) {
            int weakIndex = rand.nextInt(empires.size());
            Empire weakEmpire = empires.get(weakIndex);
            //System.out.println("WeakIndex: " + weakIndex);
            if(weakEmpire.size() == 0) continue;
            Country c1 = weakEmpire.get(rand.nextInt(weakEmpire.size()));

            ArrayList<Integer> newTour = twoOptRandom(c1.getTour());
            if(computeTourLength(newTour) < c1.getCost()){
                c1.setTour(newTour);
            }

        }
    }

    public static ArrayList<Integer> twoOptMove(ArrayList<Integer> tour, int j, int k){
        ArrayList<Integer> newTour = new ArrayList<>(tour.subList(0, j));
        ArrayList<Integer> reverseTour = new ArrayList<>(tour.subList(j, k));
        Collections.reverse(reverseTour);
        newTour.addAll(reverseTour);
        ArrayList<Integer> tourEnd = new ArrayList<>(tour.subList(k, tour.size()));
        newTour.addAll(tourEnd);

        return newTour;
    }


        public static ArrayList<Integer> twoOptRandom(ArrayList<Integer> tour){
        int nCities = tour.size();
        int j = rand.nextInt(nCities);
        int k = rand.nextInt(nCities);
        while (j == k) k = rand.nextInt(nCities);

        // Ensure that j is less than k. That is, if j > k, swap j and k
        int tempCut = j > k ? j : k;
        if (tempCut == j) {
            j = k;
            k = tempCut;
        }
        return twoOptMove(tour, j, k);
    }

    public static ArrayList<Integer> twoOptLocalSearch(ArrayList<Integer> tour){
        for (int i = 0; i < tour.size(); i++) {
            for (int j = 0; j < tour.size(); j++) {
                if(i < j){
                    ArrayList<Integer> newTour = twoOptMove(tour, i, j);
                    if (computeTourLength(newTour) < computeTourLength(tour)){
                        return newTour;
                    }
                }
            }
        }
        return tour;
    }

    public static void unite(){
        Collections.sort(empires);
        for (int i = 0; i < empires.size() - 1; i++) {
            Empire emp = empires.get(i);
            double costDifference = Math.abs(emp.getCost() - empires.get(i+1).getCost());
            if(costDifference < UNITE_THRESHOLD) {
                emp.getColonies().addAll(empires.get(i+1).getColonies());
                empires.remove(i+1);
            }
        }

    }
}
