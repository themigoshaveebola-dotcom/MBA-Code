package me.x_tias.partix.plugin.rightclick;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RightClickData {

    private long lastClickTimestamp;
    private int successes;
}
