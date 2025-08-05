package org.tilley;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.lwjgl.glfw.GLFW;
import org.rusherhack.client.api.feature.window.ResizeableWindow;
import org.rusherhack.client.api.ui.window.content.component.TextFieldComponent;
import org.rusherhack.client.api.ui.window.content.ComboContent;
import org.rusherhack.client.api.ui.window.view.RichTextView;
import org.rusherhack.client.api.ui.window.view.TabbedView;
import org.rusherhack.client.api.ui.window.view.WindowView;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LinuxTerminalWindow extends ResizeableWindow {
    private final TabbedView rootView;
    private final RichTextView rusherShellView;
    private final TextFieldComponent inputBox;

    private final String username = System.getProperty("user.name");
    private final String startDirectory = System.getProperty("user.home");

    String hostname = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[1];

    String command = null;
    String prompt = updatePrompt();

    private Process bashProcess;
    private BufferedWriter toShell;
    private BufferedReader fromShell;

    public LinuxTerminalWindow() {
        super("Linux Terminal", 200, 100, 400, 300);
        this.rusherShellView = new RichTextView("commands", this);
        final ComboContent inputCombo = new ComboContent(this);
        inputBox = new TextFieldComponent(this, this.getWidth());
        inputBox.setValue(prompt);
        inputCombo.addContent(inputBox, ComboContent.AnchorSide.LEFT);
        this.rootView = new TabbedView(this, List.of(this.rusherShellView, inputCombo));
        startShell();
    }

    private void startShell() {
        try {
            bashProcess = new ProcessBuilder(System.getenv("SHELL")).directory(new File(startDirectory)).redirectErrorStream(true).start();
            toShell = new BufferedWriter(new OutputStreamWriter(bashProcess.getOutputStream()));
            fromShell = new BufferedReader(new InputStreamReader(bashProcess.getInputStream()));

            new Thread(() -> {
                String line;
                try {
                    while ((line = fromShell.readLine()) != null) {
                        if (!line.isEmpty()) {
                            addLine(line);
                        }
                    }
                } catch (IOException ignored) {}
            }).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private record AnsiResult(Component component, Color lastColor) {}

    private Color lastColor = Color.WHITE;

    private static AnsiResult fromAnsi(String input, Color startColor) {
        MutableComponent base = Component.literal("");
        Pattern p = Pattern.compile("\u001B\\[([0-9;]*)m");
        Matcher m = p.matcher(input);

        int lastEnd = 0;
        Color currentColor = startColor;

        while (m.find()) {
            if (m.start() > lastEnd) {
                String text = input.substring(lastEnd, m.start());
                base = base.append(Component.literal(text).withStyle(Style.EMPTY.withColor(currentColor.getRGB())));
            }

            String[] codes = m.group(1).split(";");
            for (String code : codes) {
                currentColor = switch (code) {
                    case "30", "90" -> Color.BLACK;
                    case "31", "91" -> Color.RED;
                    case "32", "92" -> Color.GREEN;
                    case "33", "93" -> Color.YELLOW;
                    case "34", "94" -> Color.BLUE;
                    case "35", "95" -> Color.MAGENTA;
                    case "36", "96" -> Color.CYAN;
                    case "37", "0" -> Color.WHITE;
                    default -> currentColor;
                };
            }

            lastEnd = m.end();
        }

        if (lastEnd < input.length()) {
            base = base.append(Component.literal(input.substring(lastEnd)).withStyle(Style.EMPTY.withColor(currentColor.getRGB())));
        }

        return new AnsiResult(base, currentColor);
    }


    private void addLine(String line) {
        AnsiResult result = fromAnsi(line, lastColor);
        this.rusherShellView.add(result.component(), -1);
        lastColor = result.lastColor();
    }


    private String updatePrompt() {
        return "";
    }

    @Override
    public boolean keyTyped(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER) {
            command = inputBox.getValue().substring(prompt.length());

            if (command.equals("clear")) {
                rusherShellView.clear();

            } else {
                addLine(prompt + command);
                try {
                    toShell.write(command + "\n");
                    toShell.flush();
                } catch (IOException ignored) {}
            }

            prompt = updatePrompt();
            inputBox.setValue(prompt);
            return true;

        } else if (key == GLFW.GLFW_KEY_C && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            bashProcess.destroy();
            addLine("^C");
            startShell();
            return true;

        } else if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (inputBox.getValue().length() <= prompt.length()) return true;
        } else if (key == GLFW.GLFW_KEY_UP) {
            inputBox.setValue(prompt + command);
        }

        return super.keyTyped(key, scanCode, modifiers);
    }

    @Override
    public WindowView getRootView() {
        return this.rootView;
    }

    @Override
    public String getDescription() {
        return null;
    }
}
