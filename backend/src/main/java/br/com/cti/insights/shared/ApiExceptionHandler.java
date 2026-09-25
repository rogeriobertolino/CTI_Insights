package br.com.cti.insights.shared;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import br.com.cti.insights.clients.ClienteDuplicadoException;
import br.com.cti.insights.imports.ExcelInvalidoException;
import br.com.cti.insights.imports.ColunaObrigatoriaException;
import java.time.OffsetDateTime;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class) ResponseEntity<?> badRequest(Exception e){return error(HttpStatus.BAD_REQUEST,e);}
    @ExceptionHandler(ClienteDuplicadoException.class) ResponseEntity<?> duplicate(ClienteDuplicadoException e){return error(HttpStatus.CONFLICT,e);}
    @ExceptionHandler({ExcelInvalidoException.class, ColunaObrigatoriaException.class}) ResponseEntity<?> invalidWorkbook(RuntimeException e){return error(HttpStatus.BAD_REQUEST,e);}
    @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> tooLarge(MaxUploadSizeExceededException e){return error(HttpStatus.PAYLOAD_TOO_LARGE,new IllegalArgumentException("O arquivo deve ter no máximo 20 MB."));}
    @ExceptionHandler(Exception.class) ResponseEntity<?> internal(Exception e){return error(HttpStatus.INTERNAL_SERVER_ERROR,e);}
    private ResponseEntity<?> error(HttpStatus status,Exception e){String message=e.getMessage();if(message==null||message.isBlank())message="Não foi possível concluir a operação.";return ResponseEntity.status(status).body(Map.of("timestamp",OffsetDateTime.now(),"status",status.value(),"message",message));}
}
