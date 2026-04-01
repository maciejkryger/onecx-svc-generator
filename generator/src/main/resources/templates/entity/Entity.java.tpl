package {{modelPackage}};

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
public class {{entity}} {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

{{fieldsDecl}}{{relationsDecl}}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

{{gettersSetters}}
}