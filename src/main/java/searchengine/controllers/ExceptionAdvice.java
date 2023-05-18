package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import searchengine.dto.ErrorResponse;
import searchengine.services.IndexingServiceException;

@ControllerAdvice
public class ExceptionAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(IndexingServiceException.class)
    public ResponseEntity<ErrorResponse> handle(Exception e, WebRequest webRequest) {
        return ResponseEntity.ok(new ErrorResponse(false, e.getMessage()));
    }
}
