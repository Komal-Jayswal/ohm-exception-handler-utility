package net.apmoller.crb.ohp.microservices.model;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@AllArgsConstructor
@Data
@JsonRootName("apiStackTrace")
public class ApiStackTrace {

    private String exceptionDescription;
    private List<String> lines;
    private String id;

}