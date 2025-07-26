package com.example.ShopifyLearn.models;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenericResponse {

    private boolean status;
    private String message;
    private LocalDateTime timestamp;
    private Object data;

}
