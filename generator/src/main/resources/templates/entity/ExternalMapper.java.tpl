package {{externalMapperPackage}};

import {{generatedExternalModelPackage}}.{{generatedExternalDto}};
import {{generatedExternalModelPackage}}.{{generatedExternalSearchCriteria}};
import {{generatedModelPackage}}.{{generatedInternalSearchCriteria}};
import {{modelPackage}}.{{entity}};
import org.mapstruct.Mapper;
import org.tkit.quarkus.rs.mappers.OffsetDateTimeMapper;

@Mapper(uses = { OffsetDateTimeMapper.class })
public interface {{entity}}Mapper {

    {{generatedExternalDto}} toDto({{entity}} entity);

    {{generatedInternalSearchCriteria}} toCriteria({{generatedExternalSearchCriteria}} criteria);
}