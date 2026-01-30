package com.lektralabs.thrones.pallbearer.api.model.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentPartial {
    private String cardNumber;
    private String expirationDate;
    private String cvv;
    private String cardholderName;
    // Amount intended to be collected by this PaymentIntent.
    // A positive integer representing how much to charge in the smallest currency unit
    private int amount;

}
