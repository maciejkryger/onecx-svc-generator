package {{controllerPackage}};

import {{generatedApiPackage}}.{{generatedApiInterface}};
import {{generatedModelPackage}}.{{generatedDto}};
import {{mapperPackage}}.{{entity}}Mapper;
import {{domainServicePackage}}.{{entity}}Service;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import java.util.List;

@ApplicationScoped
public class {{entity}}Controller implements {{generatedApiInterface}} {

    @Inject
    {{entity}}Service service;

    @Inject
    {{entity}}Mapper mapper;

    @Override
    public Response getAll{{resourceOperationPlural}}() {
        List<{{generatedDto}}> result = service.listAll().stream().map(mapper::toDto).toList();
        return Response.ok(result).build();
    }

    @Override
    public Response create{{entity}}({{generatedDto}} dto) {
        var created = service.create(mapper.fromDto(dto));
        return Response.status(Response.Status.CREATED).entity(mapper.toDto(created)).build();
    }

    @Override
    public Response get{{entity}}ById(Long id) {
        return Response.ok(mapper.toDto(service.findById(id))).build();
    }

    @Override
    public Response update{{entity}}(Long id, {{generatedDto}} dto) {
        var updated = service.update(id, mapper.fromDto(dto));
        return Response.ok(mapper.toDto(updated)).build();
    }

    @Override
    public Response delete{{entity}}(Long id) {
        service.delete(id);
        return Response.noContent().build();
    }
}
