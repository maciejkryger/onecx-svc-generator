package {{mapperPackage}};

import {{generatedModelPackage}}.{{generatedDto}};
import {{modelPackage}}.{{entity}};
import org.mapstruct.Mapper;

@Mapper(componentModel = "jakarta")
public interface {{entity}}Mapper {

    {{generatedDto}} toDto({{entity}} entity);

    {{entity}} fromDto({{generatedDto}} dto);
}
