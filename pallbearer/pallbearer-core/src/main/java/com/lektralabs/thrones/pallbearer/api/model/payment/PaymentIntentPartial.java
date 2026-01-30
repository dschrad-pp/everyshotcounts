package com.lektralabs.thrones.pallbearer.api.model.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentIntentPartial {
    private String paymentIntentId;
}
