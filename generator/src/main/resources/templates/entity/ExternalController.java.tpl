package {{externalControllerPackage}};

import {{generatedExternalApiPackage}}.{{generatedExternalApiInterface}};
import {{generatedExternalModelPackage}}.{{generatedExternalDto}};
import {{generatedExternalModelPackage}}.{{generatedExternalSearchCriteria}};
import {{generatedExternalModelPackage}}.ProblemDetailResponseDTOV1;
import {{externalMapperPackage}}.{{entity}}Mapper;
import {{externalMapperPackage}}.ExceptionMapper;
import {{domainServicePackage}}.{{entity}}Service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.quarkus.jpa.exceptions.ConstraintException;

import java.util.List;

@ApplicationScoped
@Transactional(Transactional.TxType.NOT_SUPPORTED)
public class {{entity}}Controller implements {{generatedExternalApiInterface}} {

    @Inject
    {{entity}}Service service;

    @Inject
    {{entity}}Mapper mapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response get{{entity}}ById(String id) {
        return Response.ok(mapper.toDto(service.findById(id))).build();
    }

    @Override
    public Response search{{resourceOperationPlural}}(
            @Valid {{generatedExternalSearchCriteria}} criteria) {

        List<{{generatedExternalDto}}> result = service
                .findByCriteria(mapper.toCriteria(criteria))
                .stream()
                .map(mapper::toDto)
                .toList();

        return Response.ok(result).build();
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTOV1> exception(ConstraintException ex) {
        return exceptionMapper.exception(ex);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTOV1> constraint(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }
}