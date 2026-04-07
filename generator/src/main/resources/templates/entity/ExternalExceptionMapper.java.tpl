package {{externalMapperPackage}};

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.tkit.quarkus.jpa.exceptions.ConstraintException;

import {{generatedExternalModelPackage}}.ProblemDetailResponseDTOV1;

@ApplicationScoped
public class ExceptionMapper {

    public RestResponse<ProblemDetailResponseDTOV1> constraint(ConstraintViolationException ex) {
        return RestResponse.status(Response.Status.BAD_REQUEST,
                new ProblemDetailResponseDTOV1()
                        .errorCode("CONSTRAINT_VIOLATION")
                        .detail(ex.getMessage()));
    }

    public RestResponse<ProblemDetailResponseDTOV1> exception(ConstraintException ex) {
        return RestResponse.status(Response.Status.BAD_REQUEST,
                new ProblemDetailResponseDTOV1()
                        .errorCode(ex.getMessageKey().name())
                        .detail(ex.getMessage()));
    }

    public RestResponse<ProblemDetailResponseDTOV1> optimisticLock(OptimisticLockException ex) {
        return RestResponse.status(Response.Status.CONFLICT,
                new ProblemDetailResponseDTOV1()
                        .errorCode("OPTIMISTIC_LOCK")
                        .detail(ex.getMessage()));
    }
}