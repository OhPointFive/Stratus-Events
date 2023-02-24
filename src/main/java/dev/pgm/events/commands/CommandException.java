package dev.pgm.events.commands;

import static tc.oc.pgm.lib.net.kyori.adventure.text.Component.text;

import org.jetbrains.annotations.Nullable;
import tc.oc.pgm.lib.net.kyori.adventure.text.Component;
import tc.oc.pgm.lib.net.kyori.adventure.util.ComponentMessageThrowable;

public class CommandException extends RuntimeException implements ComponentMessageThrowable {

  private Component component;

  public CommandException(String message) {
    super(message);
  }

  public CommandException(Component component) {
    this.component = component;
  }

  @Override
  public @Nullable Component componentMessage() {
    if (component != null) return component;
    return text(this.getMessage());
  }
}
