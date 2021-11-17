package com.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@AllArgsConstructor
@Getter
@Setter
public class ValidationErrorMsg {
    long errorCode;
    String errorMessage;
    String technicalMessage;
    int tagNumber;
}
