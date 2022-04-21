package org.ods.doc.gen.pdf.builder.api

import groovy.util.logging.Slf4j
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@Slf4j
@ControllerAdvice
class RestResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = IllegalArgumentException.class)
    protected ResponseEntity<Object> handleArgumentError(IllegalArgumentException ex, WebRequest request) {
        log.error("Bad request: ", ex)
        return handleExceptionInternal(ex, ex.message, new HttpHeaders(), HttpStatus.BAD_REQUEST, request)
    }

    @ExceptionHandler(value = RuntimeException.class)
    protected ResponseEntity<Object> runtimeError(RuntimeException ex, WebRequest request) {
        log.error("Internal server error: ", ex)
        return handleExceptionInternal(ex, ex.message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request)
    }

}