package com.lektralabs.thrones.leagueapps;

// represents the different state a user could be in
public enum EscActionEnum {
    NEW_REGISTRATION,          // a new user registration
    REGISTERED,                // an existing user, already registered and paid in full
    STOP_PAYMENT,              // an existing user who has stopped paying
    NO_REGISTRATION_NO_PAYMENT // no registration, no payment
}
