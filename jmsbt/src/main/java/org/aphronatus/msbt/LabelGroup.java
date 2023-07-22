package org.aphronatus.msbt;

import lombok.Data;

import java.util.List;

@Data
public class LabelGroup {
    private int id;
    private List<Label> labels;

    public boolean add(Label label) {
        return labels.add(label);
    }
}