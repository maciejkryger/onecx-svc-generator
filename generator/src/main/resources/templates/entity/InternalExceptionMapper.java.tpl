package {{mapperPackage}};

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import org.tkit.quarkus.jpa.exceptions.ConstraintException;

import {{package}}.rs.common.ExceptionMapper;
import {{generatedModelPackage}}.ProblemDetailInvalidParamDTO;
import {{generatedModelPackage}}.ProblemDetailParamDTO;
import {{generatedModelPackage}}.ProblemDetailResponseDTO;

@ApplicationScoped
public class InternalExceptionMapper extends ExceptionMapper<ProblemDetailResponseDTO> {

    @Override
    protected ProblemDetailResponseDTO createConstraintViolationResponse(ConstraintViolationException ex) {
        var dto = problem(constraintViolationsCode(), ex.getMessage());
        dto.setInvalidParams(createErrorValidationResponse(ex.getConstraintViolations()));
        return dto;
    }

    @Override
    protected ProblemDetailResponseDTO createConstraintExceptionResponse(ConstraintException ex) {
        var dto = problem(ex.getMessageKey().name(), ex.getConstraints());
        dto.setParams(map(ex.namedParameters));
        return dto;
    }

    @Override
    protected ProblemDetailResponseDTO createOptimisticLockResponse(OptimisticLockException ex) {
        return problem(optimisticLockCode(), ex.getMessage());
    }

    private ProblemDetailResponseDTO problem(String errorCode, String detail) {
        ProblemDetailResponseDTO dto = new ProblemDetailResponseDTO();
        dto.setErrorCode(errorCode);
        dto.setDetail(detail);
        return dto;
    }

    public List<ProblemDetailParamDTO> map(Map<String, Object> params) {
        if (params == null) {
            return List.of();
        }
        return params.entrySet().stream().map(e -> {
            var item = new ProblemDetailParamDTO();
            item.setKey(e.getKey());
            if (e.getValue() != null) {
                item.setValue(e.getValue().toString());
            }
            return item;
        }).toList();
    }

    public List<ProblemDetailInvalidParamDTO> createErrorValidationResponse(
            Set<ConstraintViolation<?>> constraintViolations) {
        if (constraintViolations == null || constraintViolations.isEmpty()) {
            return List.of();
        }
        return constraintViolations.stream()
                .map(this::createError)
                .toList();
    }

    public ProblemDetailInvalidParamDTO createError(ConstraintViolation<?> constraintViolation) {
        ProblemDetailInvalidParamDTO dto = new ProblemDetailInvalidParamDTO();
        dto.setName(mapPath(constraintViolation.getPropertyPath()));
        dto.setMessage(constraintViolation.getMessage());
        return dto;
    }

    public String mapPath(Path path) {
        return path.toString();
    }
}
