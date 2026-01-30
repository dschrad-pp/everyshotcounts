package com.lektralabs.thrones.pallbearer.common;

public interface DrillStatusConstants {

    // The drill media has finished processing, the athlete has reviewed and
    // provided optional self-report
    String COMPLETE = "COMPLETE";

    // The drill media has finished processing. The drill is waiting on athlete
    // review and optional self-report
    String PENDING = "PENDING";

    // The drill media is in the midst of processing by the content pipeline
    // and the CV model
    String PROCESSING = "PROCESSING";

    // The athlete attempted to complete a drill but the business logic
    // mandates that the athlete needs to try again
    String RETRY = "RETRY";

    String NOT_ATTEMPTED = "NOT-ATTEMPTED";
}
