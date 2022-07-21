package nextstep.subway.domain;

import nextstep.subway.exception.AddSectionException;
import nextstep.subway.exception.DeleteSectionException;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

@Embeddable
public class Sections {

    @OneToMany(mappedBy = "line", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    private List<Section> sections = new ArrayList<>();

    public void addSection(Section section) {
        if (sections.isEmpty()) {
            this.sections.add(section);
            return;
        }

        matchLastStationAndNewUpStation(section.getUpStation());
        duplicateStation(section.getDownStation());

        this.sections.add(section);
    }

    private void matchLastStationAndNewUpStation(Station upStation) {
        Station lastStation = lastSection().getDownStation();

        if (!lastStation.equals(upStation)) {
            throw new AddSectionException("기존 노선의 종점역과 신규 노선의 상행역이 일치하지 않습니다.");
        }
    }

    private void duplicateStation(Station downStation) {
        Optional<Station> findStation = sections.stream()
                .map(Section::getUpStation)
                .filter(upStation -> upStation.equals(downStation))
                .findAny();

        if (findStation.isPresent()) {
            throw new AddSectionException("신규 구간의 하행역이 기존 노션의 역에 이미 등록되어 있습니다.");
        }
    }

    public List<Station> allStations() {
        if (sections.isEmpty()) {
            return emptyList();
        }

        List<Station> stations = sections.stream()
                .map(Section::getDownStation)
                .collect(toList());

        stations.add(0, sections.get(0).getUpStation());

        return unmodifiableList(stations);
    }

    public void removeSection(Station station) {
        if (sections.size() == 1) {
            throw new DeleteSectionException("구간이 1개인 노선은 구간 삭제를 진행할 수 없습니다.");
        }

        if (!allStations().contains(station)) {
            throw new DeleteSectionException("삭제하려는 역이 노선에 등록되지 않은 역입니다.");
        }

        if (!lastSection().getDownStation().equals(station)) {
            throw new DeleteSectionException("삭제하려는 역이 마지막 구간의 역이 아닙니다.");
        }

        sections.remove(lastSection());
    }

    public Section firstSection() {
        return sections.stream()
                .filter(section -> matchSectionsDownStation(section.getUpStation()))
                .findAny()
                .orElseThrow();
    }

    private boolean matchSectionsDownStation(Station upStation) {
        return sections.stream()
                .anyMatch(section -> !section.getDownStation().equals(upStation));
    }

    private Section lastSection() {
        return sections.get(sections.size() - 1);
    }

    public List<Section> getSections() {
        return unmodifiableList(sections);
    }
}
