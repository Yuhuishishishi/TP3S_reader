package data;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * Created by yuhui on 8/5/2016.
 * University of Michigan
 * Academic use only
 */
public class Reader {

    private final String filePath;
    private final JSONParser parser;

    public Reader(String filePath) {
        this.filePath = filePath;
        parser = new JSONParser();
    }

    public List<DataInstance> readInstances() throws IOException, ParseException {
        List<DataInstance> result = new ArrayList<>();
        JSONArray instArr = (JSONArray) this.parser.parse(new FileReader(this.filePath));
        for (Object inst : instArr) {
            JSONObject instJson = (JSONObject) inst;
            List<TestRequest> tests = getTests(instJson);
            List<Vehicle> vehicles = getVehicles(instJson);
            Map<Integer, Map<Integer, Boolean>> rehits = getRehitRules(instJson);
            String instID = (String) instJson.get("inst_id");

            DataInstance dataInstance = new DataInstance(instID,
                    tests, vehicles, rehits);
            result.add(dataInstance);
        }

        return result;

    }



    private
    List<TestRequest> getTests(JSONObject instObj) throws IOException, ParseException {
        JSONObject obj;
        if (null == instObj) {
            obj = (JSONObject) this.parser.parse(new FileReader(this.filePath));
        } else {
            obj = instObj;
        }

        JSONArray testArr = (JSONArray) obj.get("tests");
        List<TestRequest> testList = new ArrayList<>();
        for (Object aTestArr : testArr) {
            JSONObject testObj = (JSONObject) aTestArr;

            int tid = ((Long) testObj.get("test_id")).intValue();
            int prep = ((Long) testObj.get("prep")).intValue();
            int tat = ((Long) testObj.get("tat")).intValue();
            int analysis = ((Long) testObj.get("analysis")).intValue();

            int release = ((Long) testObj.get("release")).intValue();
            int deadline = ((Long) testObj.get("deadline")).intValue();

            TestRequest newTest = new TestRequest(tid, release,
                    prep, tat, analysis, deadline);
            testList.add(newTest);
        }

        System.out.println("# tests read in: " + testList.size());
        return testList;
    }

    private
    List<Vehicle> getVehicles(JSONObject instObj) throws IOException, ParseException {
        JSONObject obj;
        if (null == instObj) {
            obj = (JSONObject) this.parser.parse(new FileReader(this.filePath));
        } else {
            obj = instObj;
        }
        JSONArray vehicleArr = (JSONArray) obj.get("vehicles");
        List<Vehicle> vehicleList = new ArrayList<>();

        for (Object aVehicleArr : vehicleArr) {
            JSONObject vehicleObj = (JSONObject) aVehicleArr;

            int vid = ((Long) vehicleObj.get("vehicle_id")).intValue();
            int release = ((Long) vehicleObj.get("release")).intValue();

            Vehicle newVehicle = new Vehicle(vid, release);
            vehicleList.add(newVehicle);
        }

        System.out.println("# vehicles read in: " + vehicleList.size() );
        return vehicleList;
    }

    private
    Map<Integer, Map<Integer, Boolean>> getRehitRules(JSONObject instObj) throws IOException, ParseException {
        JSONObject obj;
        if (null == instObj) {
            obj = (JSONObject) this.parser.parse(new FileReader(this.filePath));
        } else {
            obj = instObj;
        }
        JSONObject rehitObj = (JSONObject) obj.get("rehit");

        Map<Integer, Map<Integer, Boolean>> rehitMap = new HashMap<>();
        Set entrySet = rehitObj.entrySet();
        for (Object anEntrySet : entrySet) {
            Map.Entry entry = (Map.Entry) anEntrySet;
            int key1 = Integer.valueOf((String) entry.getKey());
            rehitMap.put(key1, new HashMap<>());
            JSONObject nestedObj = (JSONObject) entry.getValue();
            for (Object elem : nestedObj.entrySet()) {
                Map.Entry e2 = (Map.Entry) elem;
                int key2 = Integer.valueOf((String) e2.getKey());
                boolean bool = (Boolean) e2.getValue();
                rehitMap.get(key1).put(key2, bool);
            }
        }

        // compute the density of the compatibility matrix
        int numEntries = 0;
        int numOnes = 0;
        for (Map<Integer, Boolean> nestedMap : rehitMap.values()) {
            numEntries += nestedMap.size();
            for (Boolean rule : nestedMap.values()) {
                if (rule) numOnes++;
            }
        }
        double density = 1.0 - (double) numOnes / numEntries;
        System.out.println("Density: " + density);
        return rehitMap;
    }
}
