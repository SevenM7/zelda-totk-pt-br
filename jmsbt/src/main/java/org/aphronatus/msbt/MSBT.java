package org.aphronatus.msbt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class MSBT {
    private short byteOrderMark = 0;
    private short version = 0;
    private short sectionSize = 0;
    private List<LabelGroup> labelGroups = new ArrayList<>();
    private List<String> stringTable = new ArrayList<>();

    @JsonIgnore
    public boolean addGroup(LabelGroup labelGroup) {
        return labelGroups.add(labelGroup);
    }

    @JsonIgnore
    public List<LabelGroup> getLabelsFilled() {
        List<LabelGroup> labelsFilled = new ArrayList<>();
        for (LabelGroup labelGroup : labelGroups) {
            LabelGroup labelGroupFilled = new LabelGroup();
            labelGroupFilled.setId(labelGroup.getId());
            labelGroupFilled.setLabels(new ArrayList<>());
            for (Label label : labelGroup.getLabels()) {
                Label labelFilled = new Label();
                labelFilled.setName(label.getName());
                labelFilled.setTableIndex(label.getTableIndex());
                labelFilled.setValue(stringTable.get(label.getTableIndex()));
                labelGroupFilled.add(labelFilled);
            }
            labelsFilled.add(labelGroupFilled);
        }
        return labelsFilled;
    }
}
