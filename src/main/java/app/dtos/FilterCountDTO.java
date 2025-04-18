package app.dtos;

import java.util.Map;

public class FilterCountDTO {
    private Map<String, Integer> genreCount;
    private Map<String, Integer> yearCount;
    private Map<String, Integer> languageCount;
    private Map<String, Integer> ratingIntervalCount;
    private Map<String, Integer> directorCount;
    private Map<String, Integer> actorCount;
    private Map<String, Integer> titleCount;

    public FilterCountDTO(Map<String, Integer> genreCount,
                          Map<String, Integer> yearCount,
                          Map<String, Integer> languageCount,
                          Map<String, Integer> ratingIntervalCount,
                          Map<String, Integer> directorCount,
                          Map<String, Integer> actorCount,
                          Map<String, Integer> titleCount) {
        this.genreCount = genreCount;
        this.yearCount = yearCount;
        this.languageCount = languageCount;
        this.ratingIntervalCount = ratingIntervalCount;
        this.directorCount = directorCount;
        this.actorCount = actorCount;
        this.titleCount = titleCount;
    }

    public Map<String, Integer> getGenreCount() {
        return genreCount;
    }

    public void setGenreCount(Map<String, Integer> genreCount) {
        this.genreCount = genreCount;
    }

    public Map<String, Integer> getYearCount() {
        return yearCount;
    }

    public void setYearCount(Map<String, Integer> yearCount) {
        this.yearCount = yearCount;
    }

    public Map<String, Integer> getLanguageCount() {
        return languageCount;
    }

    public void setLanguageCount(Map<String, Integer> languageCount) {
        this.languageCount = languageCount;
    }

    public Map<String, Integer> getRatingIntervalCount() {
        return ratingIntervalCount;
    }

    public void setRatingIntervalCount(Map<String, Integer> ratingIntervalCount) {
        this.ratingIntervalCount = ratingIntervalCount;
    }

    public Map<String, Integer> getDirectorCount() {
        return directorCount;
    }

    public void setDirectorCount(Map<String, Integer> directorCount) {
        this.directorCount = directorCount;
    }

    public Map<String, Integer> getActorCount() {
        return actorCount;
    }

    public void setActorCount(Map<String, Integer> actorCount) {
        this.actorCount = actorCount;
    }

    public Map<String, Integer> getTitleCount() {
        return titleCount;
    }

    public void setTitleCount(Map<String, Integer> titleCount) {
        this.titleCount = titleCount;
    }

    @Override
    public String toString() {
        return "FilterCountDTO{" +
                "genreCount=" + genreCount +
                ", yearCount=" + yearCount +
                ", languageCount=" + languageCount +
                ", ratingIntervalCount=" + ratingIntervalCount +
                ", directorCount=" + directorCount +
                ", actorCount=" + actorCount +
                ", titleCount=" + titleCount +
                '}';
    }
}
