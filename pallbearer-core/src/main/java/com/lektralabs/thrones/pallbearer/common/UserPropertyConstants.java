package com.lektralabs.thrones.pallbearer.common;

public interface UserPropertyConstants {

    //
    // METRICS
    //
    // drill group completion percent
    String USER_METRIC_DRILL_GROUP_COMPLETION_PERCENT = "user.metric.drill.group.completion.percent";

    // drill level completion percent
    String USER_METRIC_DRILL_LEVEL_COMPLETION_PERCENT = "user.metric.drill.level.completion.percent";

    //
    // PROGRESS
    //
    // the active drill group for the user
    String USER_DRILL_GROUP_KEY = "user.drill.group";
    String USER_DRILL_GROUP_NAME_KEY = "user.drill.group.name";

    String USER_DRILL_GROUP_LEVEL_KEY = "user.drill.group.level";
    //active drill group level index
    String USER_DRILL_GROUP_ORDER_INDEX_KEY = "user.drill.group.order.index";

    //
    // REGISTRATION
    //
    // date of user registration
    String USER_REGISTRATION_DATE_KEY = "user.registration.date";

    // the state of user registration
    String USER_REGISTRATION_STATE_KEY = "user.registration.state";

    // possible values for the user registration state property
    String USER_REGISTRATION_STATE_REGISTERED = "REGISTERED";
    String USER_REGISTRATION_STATE_UNREGISTERED = "UNREGISTERED";

    // the user registration payment status
    String USER_REGISTRATION_PAYMENT_STATE_KEY = "user.registration.payment.state";

    // possible values for the user registration payment state
    String USER_REGISTRATION_PAYMENT_STATE_PAID = "PAID";
    String USER_REGISTRATION_PAYMENT_STATE_TRIAL = "TRAIL";
    String USER_REGISTRATION_PAYMENT_STATE_PARTIAL = "PARTIAL";
    // six digit code to identify user
    String USER_REGISTRATION_SIX_DIGIT_CODE_KEY = "user.registration.sixdigit.code";
    //
    // Terms and Conditions
    String USER_TERMS_AND_CONDITIONS_ACCEPT = "user.terms.and.conditions.accept";

    String USER_REGISTRATION_STEP_KEY = "user.registration.step";
    String USER_REGISTRATION_STEP_1_6 = "step_1_6";
    String USER_REGISTRATION_STEP_2_6 = "step_2_6";
    String USER_REGISTRATION_STEP_3_6 = "step_3_6";
    String USER_REGISTRATION_STEP_4_6 = "step_4_6";
    String USER_REGISTRATION_STEP_5_6 = "step_5_6";
    String USER_REGISTRATION_STEP_6_6 = "step_6_6";
}
