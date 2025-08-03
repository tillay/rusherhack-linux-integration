package org.tilley;
import org.rusherhack.client.api.feature.hud.TextHudElement;
import org.rusherhack.core.setting.NumberSetting;
import org.rusherhack.core.setting.StringSetting;
import org.rusherhack.core.utils.Timer;

public class ShellOutputElement extends TextHudElement {
	private final Timer timer = new Timer();

	private String commandOutput(String inputCommand) {
		try {
			return new String(Runtime.getRuntime().exec(inputCommand).getInputStream().readAllBytes());
		} catch (Exception e) {
			return e.toString();
		}
	}

	private final NumberSetting<Float> delay = new NumberSetting<>("Delay", 0.1f, 0f, 5f).incremental(0.1f);
	private final StringSetting userCommand = new StringSetting("Command", "pwd");
	String output = commandOutput(userCommand.getValue());

	public ShellOutputElement() {
		super("Shell Output");
		this.registerSettings(this.delay);
		this.registerSettings(this.userCommand);
	}

	@Override
	public String getText() {
		if (timer.passed(this.delay.getValue() * 1000)) {
			timer.reset();
			output = commandOutput(userCommand.getValue()).replace("\n", " ");
		}
		return output;
	}
}
