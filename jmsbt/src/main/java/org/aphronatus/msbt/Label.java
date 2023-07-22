package org.aphronatus.msbt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class Label {
    @JsonIgnore
    private String value;
    private String name;
    private int tableIndex;
}

