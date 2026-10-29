package com.cti.provedor.controller;

import com.cti.provedor.dto.ApiError;
import com.cti.provedor.service.ClienteDuplicadoException;
import com.cti.provedor.service.ColunaObrigatoriaException;
import com.cti.provedor.service.ExcelInvalidoException;
import com.cti.provedor.service.RecursoNaoEncontradoException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.*;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  ResponseEntity<ApiError> missingResource(Exception error) {
    return response(404, "Recurso não encontrado.", List.of());
  }

  @ExceptionHandler(RecursoNaoEncontradoException.class)
  ResponseEntity<ApiError> notFound(RecursoNaoEncontradoException error) {
    return response(404, error.getMessage(), List.of());
  }

  @ExceptionHandler({
    IllegalArgumentException.class,
    ExcelInvalidoException.class,
    ColunaObrigatoriaException.class
  })
  ResponseEntity<ApiError> invalid(RuntimeException error) {
    return response(400, error.getMessage(), List.of());
  }

  @ExceptionHandler(ClienteDuplicadoException.class)
  ResponseEntity<ApiError> duplicate(ClienteDuplicadoException error) {
    return response(409, error.getMessage(), List.of());
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ApiError> status(ResponseStatusException error) {
    return response(error.getStatusCode().value(), error.getReason(), List.of());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException error) {
    var fields =
        error.getBindingResult().getFieldErrors().stream()
            .map(field -> new ApiError.FieldError(field.getField(), field.getDefaultMessage()))
            .toList();
    return response(400, "Confira os campos obrigatórios e seus formatos.", fields);
  }

  @ExceptionHandler({
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class,
    MissingServletRequestParameterException.class,
    MissingServletRequestPartException.class
  })
  ResponseEntity<ApiError> invalidInput(Exception error) {
    return response(400, "Dados inválidos. Confira os campos, formatos e parâmetros.", List.of());
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiError> method(Exception error) {
    return response(405, "Método não permitido para este recurso.", List.of());
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<ApiError> tooLarge(Exception error) {
    return response(413, "O arquivo deve ter no máximo 20 MB.", List.of());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<ApiError> constraint(Exception error) {
    return response(
        409,
        "Registro duplicado ou vinculado a outros dados. Confira antes de alterar ou excluir.",
        List.of());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> internal(Exception error) {
    String reference = UUID.randomUUID().toString();
    // Não registra mensagem da exceção: pode conter SQL, senhas ou dados enviados.
    String frame = error.getStackTrace().length == 0 ? "" : error.getStackTrace()[0].toString();
    LOG.error(
        "Falha interna referencia={} tipo={} origem={}",
        reference,
        error.getClass().getName(),
        frame);
    return response(
        500, "Não foi possível concluir a operação. Referência: " + reference, List.of());
  }

  private ResponseEntity<ApiError> response(
      int status, String message, List<ApiError.FieldError> fields) {
    return ResponseEntity.status(status)
        .body(
            new ApiError(
                OffsetDateTime.now(),
                status,
                message == null || message.isBlank()
                    ? "Não foi possível concluir a operação."
                    : message,
                fields));
  }
}
