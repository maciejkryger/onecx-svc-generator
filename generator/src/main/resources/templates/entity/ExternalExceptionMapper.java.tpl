package {{externalMapperPackage}};

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.tkit.quarkus.jpa.exceptions.ConstraintException;

import {{package}}.rs.common.ExceptionMapper;
import {{generatedExternalModelPackage}}.ProblemDetailResponseDTOV1;

@ApplicationScoped
public class ExternalExceptionMapper extends ExceptionMapper<ProblemDetailResponseDTOV1> {

    @Override
    protected ProblemDetailResponseDTOV1 createConstraintViolationResponse(ConstraintViolationException ex) {
        return new ProblemDetailResponseDTOV1()
                .errorCode("CONSTRAINT_VIOLATION")
                .detail(ex.getMessage());
    }

    @Override
    protected ProblemDetailResponseDTOV1 createConstraintExceptionResponse(ConstraintException ex) {
        return new ProblemDetailResponseDTOV1()
                .errorCode(ex.getMessageKey().name())
                .detail(ex.getMessage());
    }

    @Override
    protected ProblemDetailResponseDTOV1 createOptimisticLockResponse(OptimisticLockException ex) {
        return new ProblemDetailResponseDTOV1()
                .errorCode("OPTIMISTIC_LOCK")
                .detail(ex.getMessage());
    }

    @Override
    protected Response.Status optimisticLockStatus() {
        return Response.Status.CONFLICT;
    }
}