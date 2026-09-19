package com.sakurastore.backend.presentation.controller;

import com.sakurastore.backend.application.dto.ApiResponseDto;
import com.sakurastore.backend.application.dto.RegisterUserCommand;
import com.sakurastore.backend.application.usecase.RegisterUserUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
public class UserRegistrationController {

    private final RegisterUserUseCase registerUserUseCase;

    public UserRegistrationController(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }

    @PostMapping({"/api/usuarios/registro", "/api/users/registro", "/api/auth/registro"})
    public ResponseEntity<ApiResponseDto> registerUser(@Valid @RequestBody RegisterUserCommand command) {
        ApiResponseDto response = registerUserUseCase.execute(command);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
