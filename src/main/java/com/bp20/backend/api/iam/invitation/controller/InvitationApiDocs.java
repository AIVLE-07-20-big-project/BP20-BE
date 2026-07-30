package com.bp20.backend.api.iam.invitation.controller;

import com.bp20.backend.api.iam.invitation.dto.request.InvitationRequest;
import com.bp20.backend.api.iam.invitation.dto.response.InvitationResponse;
import com.bp20.backend.global.response.ApiResponse;
import com.bp20.backend.global.security.principal.SecurityPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

public interface InvitationApiDocs {

    @Operation(
            summary = "관리자 초대",
            description = "최고관리자가 현재 비밀번호를 재확인한 뒤 ADMIN 회원가입에 사용할 일회용 임시 비밀번호를 발급합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InvitationRequest.class),
                            examples = @ExampleObject(
                                    name = "최고관리자의 관리자 초대",
                                    summary = "SUPER_ADMIN이 ADMIN 초대",
                                    value = """
                                            {
                                              "email": "admin@bp20.com",
                                              "currentPassword": "bp20superadmin"
                                            }
                                            """
                            )
                    )
            )
    )
    ResponseEntity<ApiResponse<InvitationResponse>> inviteAdmin(
            @Parameter(hidden = true) SecurityPrincipal currentUser,
            InvitationRequest request,
            @Parameter(hidden = true) HttpServletRequest servletRequest
    );

    @Operation(
            summary = "점주 초대",
            description = "최고관리자 또는 일반관리자가 현재 비밀번호를 재확인한 뒤 STORE_OWNER 회원가입에 사용할 일회용 임시 비밀번호를 발급합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = InvitationRequest.class),
                            examples = {
                                    @ExampleObject(
                                            name = "최고관리자의 점주 초대",
                                            summary = "SUPER_ADMIN이 STORE_OWNER 초대",
                                            value = """
                                                    {
                                                      "email": "store-owner@bp20.com",
                                                      "currentPassword": "bp20superadmin"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "일반관리자의 점주 초대",
                                            summary = "ADMIN이 STORE_OWNER 초대",
                                            value = """
                                                    {
                                                      "email": "store-owner@bp20.com",
                                                      "currentPassword": "bp20admin001"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
    )
    ResponseEntity<ApiResponse<InvitationResponse>> inviteStoreOwner(
            @Parameter(hidden = true) SecurityPrincipal currentUser,
            InvitationRequest request,
            @Parameter(hidden = true) HttpServletRequest servletRequest
    );
}
