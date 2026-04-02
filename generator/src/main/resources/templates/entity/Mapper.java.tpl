package {{mapperPackage}};

import {{generatedModelPackage}}.{{generatedDto}};
import {{modelPackage}}.{{entity}};
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface {{entity}}Mapper {

    {{generatedDto}} toDto({{entity}} entity);

    {{entity}} fromDto({{generatedDto}} dto);

    void update({{generatedDto}} dto, @MappingTarget {{entity}} entity);
}