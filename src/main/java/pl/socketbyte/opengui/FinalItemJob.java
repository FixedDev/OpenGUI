package pl.socketbyte.opengui;

import lombok.Getter;

@Getter
public class FinalItemJob {

    private int slot;
    private GUIExtenderItem guiExtenderItem;

    public FinalItemJob(int slot, GUIExtenderItem guiExtenderItem) {
        this.slot = slot;
        this.guiExtenderItem = guiExtenderItem;
    }

    public FinalItemJob(GUIExtenderItem guiExtenderItem) {
        this.slot = -1;
        this.guiExtenderItem = guiExtenderItem;
    }

}
