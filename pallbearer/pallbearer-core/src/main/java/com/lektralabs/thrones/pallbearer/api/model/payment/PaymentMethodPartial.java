package com.lektralabs.thrones.pallbearer.api.model.payment;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class PaymentMethodPartial {
    private boolean useStripeSdk;
    private String paymentMethodId;
    private String currency;
    private List<Map<String, Object>> items;
}
