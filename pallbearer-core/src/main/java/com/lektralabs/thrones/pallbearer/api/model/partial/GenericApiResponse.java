package com.lektralabs.thrones.pallbearer.api.model.partial;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GenericApiResponse<T> {

    private int status;
    private String message;
    private T data;

    public GenericApiResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }
}
