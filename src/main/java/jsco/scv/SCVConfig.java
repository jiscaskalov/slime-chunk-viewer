package jsco.scv;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "slime-chunk-viewer")
public class SCVConfig implements ConfigData {
    boolean enabled = true;
    long seed = 0;

    @ConfigEntry.Gui.CollapsibleObject
    RenderOptions renderOptions = new RenderOptions();

    static class RenderOptions {
        boolean renderValid = true;
        boolean renderInvalid = false;
        int renderHeight = 75;
        String renderColor = "48,255,255";
        int renderOpacity = 115;
        boolean renderOutline = true;
        String renderOutlineColor = "96,255,66";
        int renderOutlineOpacity = 255;
    }

    @Override
    public void validatePostLoad() throws ValidationException {
        ConfigData.super.validatePostLoad();
    }
}
