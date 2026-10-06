package com.claudeproducts.api.shared;

import java.util.Comparator;
import java.util.List;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.claudeproducts.api.product.domain.DuplicateProductNameException;
import com.claudeproducts.api.product.domain.ProductNotFoundException;
import com.claudeproducts.api.product.domain.StaleProductException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Converte erros em Problem Details (RFC 9457) com mensagens em português.
 * Erros de validação trazem a lista {@code errors} com campo e mensagem.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	static final String INVALID_DATA = "Dados inválidos";

	record FieldError(String field, String message) {
	}

	@ExceptionHandler(ProductNotFoundException.class)
	ProblemDetail notFound(ProductNotFoundException ex) {
		return problem(HttpStatus.NOT_FOUND, "Produto não encontrado", ex.getMessage());
	}

	@ExceptionHandler(DuplicateProductNameException.class)
	ProblemDetail duplicate(DuplicateProductNameException ex) {
		return problem(HttpStatus.CONFLICT, "Nome já cadastrado", ex.getMessage());
	}

	@ExceptionHandler(StaleProductException.class)
	ProblemDetail stale(StaleProductException ex) {
		return problem(HttpStatus.CONFLICT, "Versão desatualizada", ex.getMessage());
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	ProblemDetail optimisticLock(ObjectOptimisticLockingFailureException ex) {
		return stale(new StaleProductException());
	}

	@ExceptionHandler(InvalidRequestException.class)
	ProblemDetail invalidRequest(InvalidRequestException ex) {
		return invalid(List.of(new FieldError(ex.getField(), ex.getMessage())));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldError> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
			.toList();
		return ResponseEntity.badRequest().body(invalid(errors));
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldError> errors = ex.getParameterValidationResults()
			.stream()
			.flatMap(result -> result.getResolvableErrors()
				.stream()
				.map(error -> new FieldError(result.getMethodParameter().getParameterName(),
						error.getDefaultMessage())))
			.toList();
		return ResponseEntity.badRequest().body(invalid(errors));
	}

	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String field = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName()
				: ex.getPropertyName();
		return ResponseEntity.badRequest()
			.body(invalid(List.of(new FieldError(field, "Valor inválido para " + field + "."))));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		if (ex.getCause() instanceof MismatchedInputException mismatch && !mismatch.getPath().isEmpty()) {
			JacksonException.Reference last = mismatch.getPath().getLast();
			String field = last.getPropertyName();
			if (field != null) {
				String message = "quantity".equals(field) ? "Quantidade deve ser um número inteiro."
						: "Valor inválido para " + field + ".";
				return ResponseEntity.badRequest().body(invalid(List.of(new FieldError(field, message))));
			}
		}
		return ResponseEntity.badRequest()
			.body(problem(HttpStatus.BAD_REQUEST, INVALID_DATA, "Corpo da requisição inválido."));
	}

	private static ProblemDetail invalid(List<FieldError> errors) {
		List<FieldError> sorted = errors.stream()
			.sorted(Comparator.comparing(FieldError::field).thenComparing(FieldError::message))
			.toList();
		ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, INVALID_DATA, sorted.getFirst().message());
		problem.setProperty("errors", sorted);
		return problem;
	}

	private static ProblemDetail problem(HttpStatus status, String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		return problem;
	}

}
