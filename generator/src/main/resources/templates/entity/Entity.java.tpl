package {{modelPackage}};

import jakarta.persistence.*;

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
