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
        int renderHeight = 50;
    }

    @Override
    public void validatePostLoad() throws ValidationException {
        ConfigData.super.validatePostLoad();
    }
}
