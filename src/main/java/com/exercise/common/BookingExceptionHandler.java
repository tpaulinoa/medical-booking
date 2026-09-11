package com.exercise.common;

import com.exercise.appointment.BookingError;
import com.exercise.appointment.BookingException;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class BookingExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BookingExceptionHandler.class);

    @ExceptionHandler(BookingException.class)
    ResponseEntity<ErrorResponse> handle(BookingException exception) {
        return respond(exception.error(), exception.error().detail());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException exception) {
        return respond(
                BookingError.INVALID_REQUEST,
                describe(exception.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + " " + error.getDefaultMessage())));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ErrorResponse> handle(HandlerMethodValidationException exception) {
        return respond(
                BookingError.INVALID_REQUEST,
                describe(exception.getParameterValidationResults().stream()
                        .flatMap(result -> result.getResolvableErrors().stream()
                                .map(error -> result.getMethodParameter().getParameterName() + " "
                                        + error.getDefaultMessage()))));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handle(MethodArgumentTypeMismatchException exception) {
        return respond(BookingError.INVALID_REQUEST, exception.getName() + " has an invalid value");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> handle(HttpMessageNotReadableException exception) {
        return respond(BookingError.INVALID_REQUEST, BookingError.INVALID_REQUEST.detail());
    }

    private static String describe(Stream<String> problems) {
        return problems.sorted()
                .reduce((left, right) -> left + "; " + right)
                .orElse(BookingError.INVALID_REQUEST.detail());
    }

    private ResponseEntity<ErrorResponse> respond(BookingError error, String detail) {
        log.info("request rejected: {}", error);

        ResponseEntity.BodyBuilder response =
                ResponseEntity.status(error.status()).contentType(MediaType.APPLICATION_JSON);

        if (error == BookingError.TEMPORARILY_UNAVAILABLE || error == BookingError.SLOT_TAKEN) {
            response.header(HttpHeaders.RETRY_AFTER, "1");
        }
        return response.body(new ErrorResponse(error, detail));
    }
}
