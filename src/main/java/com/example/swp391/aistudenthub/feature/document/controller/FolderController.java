package com.example.swp391.aistudenthub.feature.document.controller;

import com.example.swp391.aistudenthub.common.dto.ApiResponse;
import com.example.swp391.aistudenthub.common.dto.MessageResponse;
import com.example.swp391.aistudenthub.feature.auth.entity.User;
import com.example.swp391.aistudenthub.feature.document.dto.request.FolderRequest;
import com.example.swp391.aistudenthub.feature.document.dto.response.FolderResponse;
import com.example.swp391.aistudenthub.feature.document.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FolderResponse>>> getMyFolders(@AuthenticationPrincipal User currentUser) {
        List<FolderResponse> folders = folderService.getMyFolders(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(folders));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FolderResponse>> createFolder(
            @Valid @RequestBody FolderRequest request,
            @AuthenticationPrincipal User currentUser) {
        FolderResponse response = folderService.createFolder(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Tạo thư mục thành công"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FolderResponse>> updateFolder(
            @PathVariable UUID id,
            @Valid @RequestBody FolderRequest request,
            @AuthenticationPrincipal User currentUser) {
        FolderResponse response = folderService.updateFolder(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thư mục thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteFolder(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        MessageResponse response = folderService.deleteFolder(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
