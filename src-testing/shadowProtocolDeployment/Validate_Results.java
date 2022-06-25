package shadowProtocolDeployment;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Validate_Results {

    public void validate_results(List<String> pylonsList) {
        Map<String, List<Map<String, String>>> logsPerPylon = new HashMap<>();
        for (String pylon : pylonsList) {
            List<Map<String, String>> data = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader("log-" + pylon + "-monitor.yaml"))) {
                String line;
                Map<String, String> log = new HashMap<>();
                while ((line = br.readLine()) != null) {
                    var getData = line.split(": ");
                    if (line.contains("destination")) {
                        if (log.containsKey("destination") && !log.get("destination").equals("Monitoring&Control_Entity/control")) {
                            data.add(log);
                        }
                        log = new HashMap<>();
                    }
                    log.put(getData[0], getData[1]);
                }
                if (log.containsKey("destination") && !log.get("destination").equals("Monitoring&Control_Entity/control")) {
                    data.add(log);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            logsPerPylon.put(pylon, data);
//            for (Map<String, String> elem : data) {
//                System.out.println(elem);
//            }
//            System.out.println();
        }
        List<Long> receive_mess_time = new ArrayList<>();
        List<Long> move_time = new ArrayList<>();

        for (Map.Entry<String, List<Map<String, String>>> elem : logsPerPylon.entrySet()) {
            var logs = elem.getValue();
            for (int i = 0; i < logs.size(); i++) {
                var log = logs.get(i);
                if (log.get("action").equals("SEND_MESSAGE") || log.get("action").equals("MOVE_TO_ANOTHER_NODE")) {
                    var result = findPairAction(logsPerPylon, log);
                    System.out.println(result + " - "  + log);

                    if(result.get("time") != null) {
                        Timestamp timestamp_start = Timestamp.valueOf(log.get("time").substring(1, log.get("time").length() - 1));
                        Timestamp timestamp_stop = Timestamp.valueOf(result.get("time").substring(1, result.get("time").length() - 1));

                        // get time difference in seconds
                        long milliseconds = timestamp_stop.getTime() - timestamp_start.getTime();
                        System.out.println(milliseconds + " ms");

                        if (log.get("action").equals("SEND_MESSAGE")) {
                            receive_mess_time.add(Math.abs(milliseconds));
                        }
                        if (log.get("action").equals("MOVE_TO_ANOTHER_NODE")) {
                            move_time.add(Math.abs(milliseconds));
                        }
                    }
                }
            }
        }

        System.out.println("SEND_MESSAGE");
        double avg = receive_mess_time.stream()
                .mapToLong(n -> n)
                .average()
                .orElse(0.0);
        System.out.println("The average time is:" + avg + " ms for " + receive_mess_time.size() + " messages send");

        System.out.println("MOVE_TO_ANOTHER_NODE");
        double avg2 = move_time.stream()
                .mapToLong(n -> n)
                .average()
                .orElse(0.0);
        System.out.println("The average time is:" + avg2 + " ms for " + move_time.size() + " actions of moving");
    }

    public Map<String, String> findPairAction(Map<String, List<Map<String, String>>> logsPerPylon, Map<String, String> needToFind) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, List<Map<String, String>>> elem : logsPerPylon.entrySet()) {
            var logs = elem.getValue();
            int index = 0;
            for (int i = index; i<logs.size(); i++) {
                var log = logs.get(i);
                if (log.get("destination").equals(needToFind.get("destination")) &&
                        log.get("source").equals(needToFind.get("source")) &&
                        log.get("content").equals(needToFind.get("content"))) {
                    if (needToFind.get("action").equals("SEND_MESSAGE") && log.get("action").equals("RECEIVE_MESSAGE")) {
                        result = log;
                    }
                    if (needToFind.get("action").equals("MOVE_TO_ANOTHER_NODE") && log.get("action").equals("ARRIVED_ON_NODE")) {
                        result = log;
                    }
                }
            }
        }
        return result;
    }

}
