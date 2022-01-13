package org.ods.doc.gen.api

import groovy.util.logging.Slf4j
import org.springframework.core.convert.ConversionFailedException
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

    @ExceptionHandler(value = [ IllegalArgumentException.class ])
    protected ResponseEntity<Object> handleArgumentError(RuntimeException ex, WebRequest request) {
        log.error("ExceptionHandler, handleArgumentError:${ex.message}", ex)
        return handleExceptionInternal(ex, ex.message, new HttpHeaders(), HttpStatus.PRECONDITION_FAILED, request)
    }

    @ExceptionHandler(value = [ RuntimeException.class ])
    protected ResponseEntity<Object> runtimeError(RuntimeException ex, WebRequest request) {
        log.error("ExceptionHandler, runtimeError:${ex.message}", ex)
        return handleExceptionInternal(ex, ex.message, new HttpHeaders(), HttpStatus.CONFLICT, request)
    }

}