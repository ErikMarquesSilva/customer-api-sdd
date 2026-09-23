package com.example.customerapi.common.web;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.example.customerapi.customer.CustomerNotFoundException;
import com.example.customerapi.customer.DuplicateFieldException;

/**
 * The single error contract (AD-003): every error is an RFC 9457 problem; validation and uniqueness failures add an
 * {@code errors} array of {@code {field, message}}.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	static final String UNEXPECTED_DETAIL = "An unexpected error occurred.";

	/** RFC 9457 default type; Spring 7 leaves {@code type} null and omits it from the JSON. */
	private static final URI DEFAULT_TYPE = URI.create("about:blank");

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	private static final Map<String, String> UNIQUE_CONSTRAINT_FIELDS = Map.of("uk_customer_email", "email",
			"uk_customer_cpf", "cpf");

	public record FieldErrorEntry(String field, String message) {
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		// One entry per field, even when several constraints on the same field fail (CUST-36).
		Map<String, String> messages = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(error -> messages.putIfAbsent(error.getField(),
				error.getDefaultMessage()));
		List<FieldErrorEntry> errors = messages.entrySet()
			.stream()
			.map(entry -> new FieldErrorEntry(entry.getKey(), entry.getValue()))
			.toList();
		return problem(ex, HttpStatus.BAD_REQUEST, "Request validation failed.", errors, request);
	}

	@ExceptionHandler(CustomerNotFoundException.class)
	ResponseEntity<Object> handleNotFound(CustomerNotFoundException ex, WebRequest request) {
		return problem(ex, HttpStatus.NOT_FOUND, ex.getMessage(), null, request);
	}

	@ExceptionHandler(DuplicateFieldException.class)
	ResponseEntity<Object> handleDuplicate(DuplicateFieldException ex, WebRequest request) {
		List<FieldErrorEntry> errors = List
			.of(new FieldErrorEntry(ex.getField(), "is already used by another customer"));
		return problem(ex, HttpStatus.CONFLICT, ex.getMessage(), errors, request);
	}

	/** A unique constraint rejected a write that passed the pre-check, e.g. two concurrent creates (CUST-12). */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
		String field = uniqueField(ex);
		if (field == null) {
			return handleUnexpected(ex, request);
		}
		return handleDuplicate(new DuplicateFieldException(field), request);
	}

	/** Another transaction committed a change to the same customer first; its data is kept (CUST-24). */
	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ResponseEntity<Object> handleOptimisticLock(ObjectOptimisticLockingFailureException ex, WebRequest request) {
		return problem(ex, HttpStatus.CONFLICT, "The customer was changed by another request. Reload it and retry.",
				null, request);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
		log.error("Unexpected error", ex);
		return problem(ex, HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_DETAIL, null, request);
	}

	@Override
	protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers, HttpStatusCode statusCode,
			WebRequest request) {
		if (body instanceof ProblemDetail problem && problem.getType() == null) {
			problem.setType(DEFAULT_TYPE);
		}
		return super.createResponseEntity(body, headers, statusCode, request);
	}

	private ResponseEntity<Object> problem(Exception ex, HttpStatus status, String detail,
			List<FieldErrorEntry> errors, WebRequest request) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
		if (errors != null) {
			body.setProperty("errors", errors);
		}
		return handleExceptionInternal(ex, body, new HttpHeaders(), status, request);
	}

	private static String uniqueField(DataIntegrityViolationException ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation && violation.getConstraintName() != null) {
				return UNIQUE_CONSTRAINT_FIELDS.get(violation.getConstraintName());
			}
		}
		return null;
	}

}
