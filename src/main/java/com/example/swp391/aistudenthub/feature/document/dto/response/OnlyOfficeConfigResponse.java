package com.example.swp391.aistudenthub.feature.document.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlyOfficeConfigResponse {

    private String docserviceUrl;
    private String token;
    private String documentType;
    private DocumentConfig document;
    private EditorConfig editorConfig;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentConfig {
        private String fileType;
        private String key;
        private String title;
        private String url;
        private Permissions permissions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Permissions {
        private boolean edit;
        private boolean download;
        private boolean print;
        private boolean comment;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EditorConfig {
        private String mode;
        private String callbackUrl;
        private UserInfo user;
        private String lang;
        private Customization customization;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customization {
        private boolean autosave;
        private boolean forcesave;
    }
}
