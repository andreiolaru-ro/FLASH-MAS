package gabi.entityOperationTest.scenario.relations;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ActivityRelation {

    CLOUD_COMPUTING, OPERATING_SYSTEMS;

    public static List<String> getAllActivities() {
        return Stream.of(ActivityRelation.values())
                .map(Enum::name)
                .collect(Collectors.toList());
    }
}
