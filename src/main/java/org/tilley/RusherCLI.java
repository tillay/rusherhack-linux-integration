package org.tilley;

import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.StringSetting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RusherCLI extends ToggleableModule {
    StringSetting filePath = new StringSetting("String", "/tmp/signal");

    public RusherCLI() {
        super("RusherCLI", "Run rusher commands from external scripts", ModuleCategory.CLIENT);
        this.registerSettings(filePath);
    }

    @Subscribe
    private void onUpdate(EventUpdate event) {
        try {
            Path path = Paths.get(filePath.getValue());
            if (Files.exists(path)) {
                String signal = Files.readAllLines(path).getFirst();
                ChatUtils.print("got message: " + signal);
                Files.delete(path);
                if (signal.charAt(0) == '*') {
                    mc.getConnection().sendChat(signal);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}