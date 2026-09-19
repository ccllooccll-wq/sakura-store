package com.sakurastore.backend.presentation.controller;

import com.sakurastore.backend.application.dto.*;
import com.sakurastore.backend.application.usecase.AuthenticateUserUseCase;
import com.sakurastore.backend.application.usecase.RequestPasswordResetUseCase;
import com.sakurastore.backend.application.usecase.ResendVerificationCodeUseCase;
import com.sakurastore.backend.application.usecase.ResetPasswordUseCase;
import com.sakurastore.backend.application.usecase.VerifyEmailUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ResendVerificationCodeUseCase resendVerificationCodeUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    public AuthController(AuthenticateUserUseCase authenticateUserUseCase,
                          VerifyEmailUseCase verifyEmailUseCase,
                          ResendVerificationCodeUseCase resendVerificationCodeUseCase,
                          RequestPasswordResetUseCase requestPasswordResetUseCase,
                          ResetPasswordUseCase resetPasswordUseCase) {
        this.authenticateUserUseCase = authenticateUserUseCase;
        this.verifyEmailUseCase = verifyEmailUseCase;
        this.resendVerificationCodeUseCase = resendVerificationCodeUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginCommand command) {
        AuthResponseDto response = authenticateUserUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verificar-email")
    public ResponseEntity<AuthResponseDto> verifyEmail(@Valid @RequestBody VerifyEmailCommand command) {
        AuthResponseDto response = verifyEmailUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reenviar-codigo")
    public ResponseEntity<ApiResponseDto> resendCode(@Valid @RequestBody ResendCodeCommand command) {
        ApiResponseDto response = resendVerificationCodeUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/solicitar-recuperacion")
    public ResponseEntity<ApiResponseDto> requestPasswordReset(@Valid @RequestBody RequestPasswordResetCommand command) {
        ApiResponseDto response = requestPasswordResetUseCase.execute(command);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/restablecer-password")
    public ResponseEntity<ApiResponseDto> resetPassword(@Valid @RequestBody ResetPasswordCommand command) {
        ApiResponseDto response = resetPasswordUseCase.execute(command);
        return ResponseEntity.ok(response);
    }
}
