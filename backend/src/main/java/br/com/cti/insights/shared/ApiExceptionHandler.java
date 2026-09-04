package br.com.cti.insights.shared;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> badRequest(Exception e){return error(HttpStatus.BAD_REQUEST,e);}
    @ExceptionHandler(Exception.class) ResponseEntity<?> internal(Exception e){return error(HttpStatus.INTERNAL_SERVER_ERROR,e);}
    private ResponseEntity<?> error(HttpStatus status,Exception e){return ResponseEntity.status(status).body(Map.of("timestamp",OffsetDateTime.now(),"status",status.value(),"message",e.getMessage()));}
}
