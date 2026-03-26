package {{package}}.dao;

import {{package}}.entity.{{entity}};
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class {{entity}}DAO implements PanacheRepository<{{entity}}> { }
