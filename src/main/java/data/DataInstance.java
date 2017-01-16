package data;

import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by yuhui on 8/5/2016.
 * University of Michigan
 * Academic use only
 */
public class DataInstance {

    private static DataInstance instance = null;
    private static Map<String, DataInstance> instanceMap = null;

    private final String instID;

    private final List<TestRequest> testArr;
    private final List<Vehicle> vehicleArr;
    private final Map<Integer, Map<Integer, Boolean>> rehitRules;

    private final Map<Integer, TestRequest> testIdToTtestMap;
    private final Map<Integer, Vehicle> vehicleIdToVehicleMap;

    private int horizonEnd;

    protected DataInstance(String instID, List<TestRequest> tests, List<Vehicle> vehicles,
                           Map<Integer, Map<Integer, Boolean>> rehitRules) {
        this.instID = instID;
        this.testArr = tests;
        this.vehicleArr = vehicles;
        this.rehitRules = rehitRules;

        testIdToTtestMap = new HashMap<>();
        vehicleIdToVehicleMap = new HashMap<>();

        // build the maps
        tests.forEach(test -> testIdToTtestMap.put(test.getTid(), test));
        vehicles.forEach(vehicle -> vehicleIdToVehicleMap.put(vehicle.getVid(), vehicle));

        horizonEnd = getHorizonEndBasedOnDeadline();
    }

    public static void init(Reader reader) {
        instanceMap = new HashMap<>();
        instance = null;

        try {
            List<DataInstance> dataInstances = reader.readInstances();
            dataInstances.forEach(inst->instanceMap.put(inst.instID, inst));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        if (instanceMap.size()==1) {
            instance = instanceMap.values().iterator().next();
        }

        System.out.println("# instances: " + instanceMap.size());
    }

    public static DataInstance getInstance() {

        if (DataInstance.instance == null) {
            System.err.println("Data instance called while uninitialized.\n" +
                    "or multiple instances read in.");
        }

        return DataInstance.instance;
    }

    public static DataInstance getInstance(String instID) {
        assert instanceMap.containsKey(instID);
        return instanceMap.get(instID);
    }

    public static List<String> getInstIds() {
        return new ArrayList<>(instanceMap.keySet());
    }

    public static int getHorizonStartGlobal() {
        OptionalInt minStart = instanceMap.values().stream().mapToInt(DataInstance::getHorizonStart).min();
        assert minStart.isPresent();
        return minStart.getAsInt();
    }

    public static int getHorizonEndGlobal() {
        OptionalInt maxEnd = instanceMap.values().stream().mapToInt(DataInstance::getHorizonEnd).max();
        assert maxEnd.isPresent();
        return maxEnd.getAsInt();
    }

    public boolean getRelation(int tid1, int tid2) throws IllegalArgumentException{
        assert rehitRules.containsKey(tid1);

        Map<Integer, Boolean> nested = rehitRules.get(tid1);
        assert nested.containsKey(tid2);

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


    public boolean isSeqCompWithTest(List<Integer> seq, int newTid) {
        if (seq.size()==0)
            return true;

        if (seq.contains(newTid))
            return false;

        for (int tid : seq) {
            if (!this.getRelation(tid, newTid))
                return false;
        }

        return true;
    }

    public int getHorizonStart() {
        // get the length of the planning horizon
        return Collections.min(getVehicleReleaseList());
    }

    private int getHorizonEndBasedOnDeadline() {
        int longestDur = getTestArr().stream().mapToInt(TestRequest::getDur).max().getAsInt();
        int latestDeadline =getTestArr().stream().mapToInt(TestRequest::getDeadline).max().getAsInt();
        int latestReleaseDay = getTestArr().stream().mapToInt(TestRequest::getRelease).max().getAsInt();
        latestDeadline = Math.max(latestDeadline, latestReleaseDay);
        final int timeSlack = 50;
        return latestDeadline + longestDur*4 + timeSlack;
    }

    public String getInstID() {
        return instID;
    }

    public int getHorizonEnd() {
        return horizonEnd;
    }

    public void setHorizonEnd(int horizonEnd) {
        this.horizonEnd = horizonEnd;
    }
}
