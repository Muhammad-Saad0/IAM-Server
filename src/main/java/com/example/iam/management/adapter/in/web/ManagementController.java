package com.example.iam.management.adapter.in.web;

import com.example.iam.account.application.service.AccountManagementService;
import com.example.iam.auth.application.service.OAuthClientManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/management")
@RequiredArgsConstructor
public class ManagementController {
    private final AccountManagementService accountManagementService;
    private final OAuthClientManagementService oAuthClientManagementService;

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountManagementService.CreatedAccount createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        return accountManagementService.createAccount(
                request.email(),
                request.password(),
                request.roles()
        );
    }

    @PostMapping("/oauth-clients")
    @ResponseStatus(HttpStatus.CREATED)
    public OAuthClientManagementService.CreatedOAuthClient createOAuthClient(
            @Valid @RequestBody CreateOAuthClientRequest request
    ) {
        return oAuthClientManagementService.createClient(
                request.clientName(),
                request.redirectUris(),
                request.scopes() == null ? List.of() : request.scopes()
        );
    }
}
