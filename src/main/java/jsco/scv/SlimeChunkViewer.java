package jsco.scv;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class SlimeChunkViewer implements ModInitializer {
	public static final MinecraftClient client = MinecraftClient.getInstance();

	@Override
	public void onInitialize() {
		AutoConfig.register(SCVConfig.class, Toml4jConfigSerializer::new);
        KeyBinding restoreChunks = KeyBindingHelper.registerKeyBinding(new KeyBinding("Reload Chunks", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "SlimeChunkViewer"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (restoreChunks.wasPressed()) {
                SCVRenderer.slimeChunks.clear();
                SCVRenderer.nonSlimeChunks.clear();
            }
        });

        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("setseed").then(argument("seed", LongArgumentType.longArg()).executes(context -> {
                SCVConfig config = AutoConfig.getConfigHolder(SCVConfig.class).getConfig();
                config.seed = context.getArgument("seed", Long.class);
                return 1;
            })));
        }));
    }

	public static Object getConfigProperty(String key) {
		SCVConfig config = AutoConfig.getConfigHolder(SCVConfig.class).getConfig();
        return switch (key) {
            case "enabled" -> config.enabled;
            case "seed" -> config.seed;
            case "renderValid" -> config.renderOptions.renderValid;
            case "renderInvalid" -> config.renderOptions.renderInvalid;
            case "renderHeight" -> config.renderOptions.renderHeight;
            default -> null;
        };
    }
}