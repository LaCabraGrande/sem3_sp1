package dtos;

import lombok.Data;

import java.util.List;

@Data
public class FilmDTO {
    private String titel;
    private String beskrivelse;
    private String udgivelsesdato;
    private String plakatSti;
    private double gennemsnitligStemme;
    private int antalStemmer;
    private String baggrundSti;
    private List<Integer> genreIds;
}
