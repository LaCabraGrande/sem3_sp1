package app.persistence.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class GenreData {
    private Integer id;
    private String name;

    public GenreData(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
}
