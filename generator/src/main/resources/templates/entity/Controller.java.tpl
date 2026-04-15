package {{controllerPackage}};

import {{generatedApiPackage}}.{{generatedApiInterface}};
import {{generatedModelPackage}}.{{generatedDto}};
import {{generatedModelPackage}}.{{entity}}SearchCriteriaDTO;
import {{generatedModelPackage}}.ProblemDetailResponseDTO;
import {{mapperPackage}}.ExceptionMapper;
import {{mapperPackage}}.{{entity}}Mapper;
import {{domainServicePackage}}.{{entity}}Service;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.tkit.quarkus.jpa.exceptions.ConstraintException;

import java.util.List;

@ApplicationScoped
@Transactional(Transactional.TxType.NOT_SUPPORTED)
public class {{entity}}Controller implements {{generatedApiInterface}} {

    @Inject
    {{entity}}Service service;

    @Inject
    {{entity}}Mapper mapper;

    @Inject
    ExceptionMapper exceptionMapper;

    @Override
    public Response create{{entity}}({{generatedDto}} dto) {
        var created = service.create(dto);
        return Response.status(Response.Status.CREATED)
                .entity(mapper.toDto(created))
                .build();
    }

    @Override
    public Response get{{entity}}ById(String id) {
        return Response.ok(mapper.toDto(service.findById(id))).build();
    }

    @Override
    public Response update{{entity}}(String id, {{generatedDto}} dto) {
        var updated = service.update(id, dto);
        return Response.ok(mapper.toDto(updated)).build();
    }

    @Override
    public Response delete{{entity}}(String id) {
        service.delete(id);
        return Response.noContent().build();
    }

    @Override
    public Response search{{resourceOperationPlural}}(Integer limit, Integer offset, {{entity}}SearchCriteriaDTO criteria) {
        List<{{generatedDto}}> result = service.findByCriteria(criteria, offset, limit)
                .stream()
                .map(mapper::toDto)
                .toList();
        return Response.ok(result).build();
    }

    public Response search{{resourceOperationPlural}}({{entity}}SearchCriteriaDTO criteria, Integer limit, Integer offset) {
        List<{{generatedDto}}> result = service.findByCriteria(criteria, offset, limit)
                .stream()
                .map(mapper::toDto)
                .toList();
        return Response.ok(result).build();
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> exception(ConstraintException ex) {
        return exceptionMapper.exception(ex);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> constraint(ConstraintViolationException ex) {
        return exceptionMapper.constraint(ex);
    }

    @ServerExceptionMapper
    public RestResponse<ProblemDetailResponseDTO> daoException(OptimisticLockException ex) {
        return exceptionMapper.optimisticLock(ex);
    }
}