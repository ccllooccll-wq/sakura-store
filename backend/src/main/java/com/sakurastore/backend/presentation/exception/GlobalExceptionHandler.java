package com.sakurastore.backend.presentation.exception;

import com.sakurastore.backend.domain.exception.DomainException;
import com.sakurastore.backend.infrastructure.mail.ResendEmailAdapter.MailDeliveryException;
import com.sakurastore.backend.presentation.dto.ErrorResponseDto;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private ResponseEntity<ErrorResponseDto> error(int status, String message) {
    return ResponseEntity.status(status)
        .body(new ErrorResponseDto(status, "Solicitud no completada", message));
  }

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorResponseDto> domain(DomainException e) {
    return error(400, e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDto> validation(MethodArgumentNotValidException e) {
    return error(
        400,
        e.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(x -> x.getField() + ": " + x.getDefaultMessage())
            .orElse("Datos inválidos."));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponseDto> status(ResponseStatusException e) {
    return error(e.getStatusCode().value(), e.getReason());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponseDto> denied(AccessDeniedException e) {
    return error(403, "No tienes permiso para realizar esta operación.");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponseDto> conflict(DataIntegrityViolationException e) {
    return error(
        409,
        "El registro está duplicado o tiene datos relacionados. Revisa SKU, usuario, correo y"
            + " referencias.");
  }

  @ExceptionHandler(MailDeliveryException.class)
  public ResponseEntity<ErrorResponseDto> mail(MailDeliveryException e) {
    return error(503, e.getMessage());
  }

  @ExceptionHandler({
    org.springframework.http.converter.HttpMessageNotReadableException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ErrorResponseDto> invalid(Exception e) {
    return error(400, "El formato de los datos no es válido.");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDto> general(Exception e) {
    org.slf4j.LoggerFactory.getLogger(getClass()).error("Error interno", e);
    return error(500, "No se pudo completar la operación. Contacta al administrador.");
  }
}
