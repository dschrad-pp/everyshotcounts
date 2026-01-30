package com.lektralabs.thrones.pallbearer.security;

import io.quarkus.runtime.Quarkus;

// @QuarkusMain
public class KeycloakRunner {

    public static void main(String ... args) {
        System.out.println("KeycloakRunner::main app started");
        Quarkus.run(KeycloakRunnerApplication.class);
    }
}
