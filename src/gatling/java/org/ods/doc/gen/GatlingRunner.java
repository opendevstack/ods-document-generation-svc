package org.ods.doc.gen;

import io.gatling.app.Gatling;
import io.gatling.core.config.GatlingPropertiesBuilder;

public class GatlingRunner {
    public static void main(String[] args) {
        GatlingPropertiesBuilder props = new GatlingPropertiesBuilder();
        props.simulationClass(LoadSimulation.class.getName());
        props.resultsDirectory("build/reports/gatling");
        Gatling.fromMap(props.build());
    }
}
