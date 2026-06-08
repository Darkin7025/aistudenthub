package com.example.swp391.aistudenthub.feature.document.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentFilterOptionsResponse {
    private List<String> subjects;
    private List<String> majors;
}
