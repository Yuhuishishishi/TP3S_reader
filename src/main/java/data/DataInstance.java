package data;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by yuhui on 8/5/2016.
 */
public class DataInstance {

    private static DataInstance instance = null;
    private List<TestRequest> testArr;
    private List<Vehicle> vehicleArr;
    private Map<Integer, Map<Integer, Boolean>> rehitRules;

    private Map<Integer, TestRequest> testIdToTtestMap;
    private Map<Integer, Vehicle> vehicleIdToVehicleMap;

    private DataInstance () {

    }

    public static void init(Reader reader) {

        List<TestRequest> tests = null;
        List<Vehicle> vehicles = null;
        Map<Integer, Map<Integer, Boolean>> rules = null;
        try {
            tests = reader.getTests();
            vehicles = reader.getVehicles();
            rules = reader.getRehitRules();
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        instance = new DataInstance();
        instance.testArr = tests.stream().sorted((t1,t2)->t1.getTid()-t2.getTid())
                .collect(Collectors.toList());
        instance.vehicleArr = vehicles.stream().sorted((v1,v2)->v1.getRelease()-v2.getRelease())
                .collect(Collectors.toList());
        instance.rehitRules = rules;

        instance.testIdToTtestMap = new HashMap<>();
        instance.vehicleIdToVehicleMap = new HashMap<>();
        assert tests != null;
        tests.forEach(test -> instance.testIdToTtestMap.put(test.getTid(), test));
        assert vehicles != null;
        vehicles.forEach(vehicle -> instance.vehicleIdToVehicleMap.put(vehicle.getVid(), vehicle));
    }

    public static DataInstance getInstance() {
        if (DataInstance.instance == null) {
            System.err.println("Data instance called while uninitialized.");
        }

        return DataInstance.instance;
    }

    public boolean getRelation(int tid1, int tid2) throws IllegalArgumentException{
        if (!rehitRules.containsKey(tid1)) {
            throw new IllegalArgumentException(tid1 + " is not a valid tid.");
        }

        Map<Integer, Boolean> nested = getInstance().rehitRules.get(tid1);
        if (!nested.containsKey(tid2)) {
            throw new IllegalArgumentException(tid1 + ", " + tid2 + " is not a valid tid pair.");
        }

        return rehitRules.get(tid1).get(tid2);

    }

    public TestRequest getTestById(int tid) {
        return testIdToTtestMap.get(tid);
    }

    public int numVehiclesByRelease(int release) {
        return (int) vehicleArr.stream()
                .map(Vehicle::getRelease).filter(r -> r==release).count();
    }

    public List<TestRequest> getTestArr() {
        return testArr;
    }

    public List<Integer> getTidList() {
        return testArr.stream().map(TestRequest::getTid).sorted().collect(Collectors.toList());
    }

    public List<Integer> getVehicleReleaseList() {
        return vehicleArr.stream()
                .map(Vehicle::getRelease).distinct().sorted().collect(Collectors.toList());
    }


    public static boolean isSeqCompWithTest(List<Integer> seq, int newTid) {
        if (seq.size()==0)
            return true;

        if (seq.contains(newTid))
            return false;

        for (int tid : seq) {
            if (!DataInstance.getInstance().getRelation(tid, newTid))
                return false;
        }

        return true;
    }

    public int getHorizonStart() {
        // get the length of the planning horizon
        return Collections.min(DataInstance.getInstance().getVehicleReleaseList());
    }

    public int getHorizonEnd() {
        int longestDur = DataInstance.getInstance()
                .getTestArr().stream().mapToInt(TestRequest::getDur).max().getAsInt();
        int latestDeadline = DataInstance.getInstance()
                .getTestArr().stream().mapToInt(TestRequest::getDeadline).max().getAsInt();
        final int timeSlack = 50;
        return latestDeadline + longestDur + timeSlack;
    }
}
