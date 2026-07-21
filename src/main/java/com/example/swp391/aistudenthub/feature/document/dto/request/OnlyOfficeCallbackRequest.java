package com.example.swp391.aistudenthub.feature.document.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OnlyOfficeCallbackRequest {

    private String key;
    private Integer status;
    private String url;
    private String changesurl;
    private List<String> users;
    private String userdata;
    private Integer filetype;
    private String forcesavetype;
}
