package app.entities;

import app.dtos.DirectorDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "director")
public class Director {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = true)
    private String name;

    @OneToMany(mappedBy = "director", fetch = FetchType.LAZY)
    @JsonIgnore // Forhindrer problemer ved lazy-loading og JSON-serialisering
    @ToString.Exclude
    private Set<Movie> movies;

    // Constructor der konverterer fra DTO til entitet
    public Director(DirectorDTO dto) {
        this.id = dto.getId();
        this.name = dto.getName();
    }
}
