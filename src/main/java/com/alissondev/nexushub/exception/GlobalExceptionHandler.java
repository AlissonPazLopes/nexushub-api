package com.alissondev.nexushub.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
			MethodArgumentNotValidException ex,
			HttpServletRequest request
	) {
		ProblemDetail problem = createProblemDetail(
				HttpStatus.BAD_REQUEST,
				"Dados de entrada invalidos",
				"Um ou mais campos falharam na validacao.",
				request
		);

		List<ValidationError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> new ValidationError(fieldError.getField(), fieldError.getDefaultMessage()))
				.toList();
		problem.setProperty("errors", errors);

		return toProblemResponse(HttpStatus.BAD_REQUEST, problem);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
			HttpMessageNotReadableException ex,
			HttpServletRequest request
	) {
		ProblemDetail problem = createProblemDetail(
				HttpStatus.BAD_REQUEST,
				"Payload JSON malformado ou invalido",
				"Nao foi possivel processar o corpo da requisicao. Verifique os tipos e valores informados.",
				request
		);
		return toProblemResponse(HttpStatus.BAD_REQUEST, problem);
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ProblemDetail> handleResourceNotFound(
			ResourceNotFoundException ex,
			HttpServletRequest request
	) {
		ProblemDetail problem = createProblemDetail(
				HttpStatus.NOT_FOUND,
				"Recurso nao encontrado",
				ex.getMessage(),
				request
		);
		return toProblemResponse(HttpStatus.NOT_FOUND, problem);
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ProblemDetail> handleConflict(
			ConflictException ex,
			HttpServletRequest request
	) {
		ProblemDetail problem = createProblemDetail(
				HttpStatus.CONFLICT,
				"Conflito de dados",
				ex.getMessage(),
				request
		);
		return toProblemResponse(HttpStatus.CONFLICT, problem);
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<ProblemDetail> handleOptimisticLocking(
			ObjectOptimisticLockingFailureException ex,
			HttpServletRequest request
	) {
		ProblemDetail problem = createProblemDetail(
				HttpStatus.CONFLICT,
				"Conflito de concorrencia",
				"O benefício foi alterado por outra requisição. Tente novamente.",
				request
		);
		return toProblemResponse(HttpStatus.CONFLICT, problem);
	}

	@ExceptionHandler(BusinessRuleException.class)
	public ResponseEntity<ProblemDetail> handleBusinessRule(
			BusinessRuleException ex,
			HttpServletRequest request
	) {
		ProblemDetail problem = createProblemDetail(
				HttpStatus.UNPROCESSABLE_ENTITY,
				"Violacao de regra de negocio",
				ex.getMessage(),
				request
		);
		return toProblemResponse(HttpStatus.UNPROCESSABLE_ENTITY, problem);
	}

	private ProblemDetail createProblemDetail(
			HttpStatus status,
			String title,
			String detail,
			HttpServletRequest request
	) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setInstance(URI.create(request.getRequestURI()));
		problem.setProperty("timestamp", Instant.now());
		return problem;
	}

	private ResponseEntity<ProblemDetail> toProblemResponse(HttpStatus status, ProblemDetail problem) {
		return ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_PROBLEM_JSON)
				.body(problem);
	}

	public record ValidationError(String field, String message) {
	}

}
