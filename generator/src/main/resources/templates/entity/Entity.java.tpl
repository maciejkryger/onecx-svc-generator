package {{modelPackage}};

{{entityImports}}import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.tkit.quarkus.jpa.models.TraceableEntity;

@Entity
@Table(name = "{{tableName}}")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class {{entity}} extends TraceableEntity {

    @Column(name = "tenant")
    private String tenant;

{{fieldsDecl}}{{relationsDecl}}
}