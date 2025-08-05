package org.tilley;

import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;
import org.rusherhack.core.command.argument.StringCapture;

public class ShellCommand extends Command {
    private String commandOutput(String inputCommand) {
        try {
            return new String(Runtime.getRuntime().exec(inputCommand).getInputStream().readAllBytes());
        } catch (Exception e) {
            return e.toString();
        }
    }

    public ShellCommand() {
        super("exec", "joo lee do the thing");
    }

    @CommandExecutor
    @CommandExecutor.Argument("command")
    private String exec(StringCapture command) {
        return "\n"+commandOutput(command.string());
    }


}