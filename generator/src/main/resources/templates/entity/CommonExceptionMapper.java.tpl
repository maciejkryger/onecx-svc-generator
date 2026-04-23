package {{package}}.rs.common;

import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.tkit.quarkus.jpa.exceptions.ConstraintException;

public abstract class ExceptionMapper<T> {

    public RestResponse<T> constraint(ConstraintViolationException ex) {
        return RestResponse.status(constraintStatus(), createConstraintViolationResponse(ex));
    }

    public RestResponse<T> exception(ConstraintException ex) {
        return RestResponse.status(constraintExceptionStatus(), createConstraintExceptionResponse(ex));
    }

    public RestResponse<T> optimisticLock(OptimisticLockException ex) {
        return RestResponse.status(optimisticLockStatus(), createOptimisticLockResponse(ex));
    }

    protected Response.Status constraintStatus() {
        return Response.Status.BAD_REQUEST;
    }

    protected Response.Status constraintExceptionStatus() {
        return Response.Status.BAD_REQUEST;
    }

    protected Response.Status optimisticLockStatus() {
        return Response.Status.BAD_REQUEST;
    }

    protected String constraintViolationsCode() {
        return ErrorKeys.CONSTRAINT_VIOLATIONS.name();
    }

    protected String optimisticLockCode() {
        return ErrorKeys.OPTIMISTIC_LOCK.name();
    }

    protected abstract T createConstraintViolationResponse(ConstraintViolationException ex);

    protected abstract T createConstraintExceptionResponse(ConstraintException ex);

    protected abstract T createOptimisticLockResponse(OptimisticLockException ex);

    public enum ErrorKeys {
        OPTIMISTIC_LOCK,
        CONSTRAINT_VIOLATIONS;
    }
}