package com.lektralabs.thrones.pallbearer.api.model.partial;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GenericApiResponse<T> {

    private int status;

    /**
     * Stable, machine-readable error identifier (e.g. INVALID_CREDENTIALS).
     * Null on success responses; serialized as "error_code" and omitted when null
     * so existing success/response shapes are unchanged.
     */
    @JsonProperty("error_code")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;

    private String message;
    private T data;

    public GenericApiResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public GenericApiResponse(int status, String errorCode, String message, T data) {
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.data = data;
    }
}
