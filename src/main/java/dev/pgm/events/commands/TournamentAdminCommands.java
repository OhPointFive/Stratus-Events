package dev.pgm.events.commands;

import dev.pgm.events.EventsPlugin;
import dev.pgm.events.TournamentManager;
import dev.pgm.events.api.teams.TournamentTeamRegistry;
import dev.pgm.events.team.TournamentPlayer;
import dev.pgm.events.team.TournamentTeam;
import dev.pgm.events.team.TournamentTeamManager;
import dev.pgm.events.xml.MapFormatXMLParser;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.integration.Integration;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.MatchManager;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.lib.cloud.commandframework.annotations.Argument;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandDescription;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandMethod;
import tc.oc.pgm.lib.cloud.commandframework.annotations.CommandPermission;
import tc.oc.pgm.lib.cloud.commandframework.annotations.specifier.Greedy;

@CommandMethod("tourney|tournament|tm|events")
public class TournamentAdminCommands {

  @CommandMethod("create <format>")
  @CommandDescription("Creates a tournament")
  @CommandPermission("events.staff")
  public void tourney(
      CommandSender sender,
      TournamentManager manager,
      Match match,
      @Argument("format") @Greedy String pool) {
    manager.createTournament(match, MapFormatXMLParser.parse(pool));
    sender.sendMessage(ChatColor.GOLD + "Starting tournament.");
  }

  @CommandMethod("register <team>")
  @CommandDescription("Register a team")
  @CommandPermission("events.staff")
  public void register(
      CommandSender sender,
      TournamentTeamRegistry teamRegistry,
      TournamentTeamManager teamManager,
      @Argument("team") @Greedy String name) {
    TournamentTeam team = teamRegistry.getTeam(name);
    // TODO move to provider
    if (team == null) throw new CommandException("Team not found!");

    MatchManager matchManager = PGM.get().getMatchManager();

    for (TournamentPlayer player : team.getPlayers()) {
      Player bukkit = Bukkit.getPlayer(player.getUUID());
      MatchPlayer mp = matchManager.getPlayer(bukkit);
      if (Integration.isVanished(bukkit)) Integration.setVanished(mp, false, false);
    }

    teamManager.addTeam(team);
    sender.sendMessage(ChatColor.YELLOW + "Added team " + team.getName() + "!");
  }

  @CommandMethod("list")
  @CommandDescription("List all loaded teams")
  @CommandPermission("events.staff")
  public void list(CommandSender sender, TournamentTeamRegistry registry) {
    sender.sendMessage(
        ChatColor.GOLD
            + "------- "
            + ChatColor.AQUA
            + "Registered Teams"
            + ChatColor.GOLD
            + " -------");
    for (TournamentTeam team : EventsPlugin.get().getTeamRegistry().getTeams())
      sender.sendMessage(ChatColor.AQUA + "- " + team.getName());
    sender.sendMessage(ChatColor.YELLOW + "Run /tourney info <team> to see player roster!");
  }

  @CommandMethod("info <team>")
  @CommandDescription("View information about a team")
  @CommandPermission("events.staff")
  public void info(
      CommandSender sender,
      TournamentTeamRegistry registry,
      @Argument("team") @Greedy String name) {
    TournamentTeam team = registry.getTeam(name);
    if (team == null) throw new CommandException("Team not found!");

    sender.sendMessage(
        ChatColor.GOLD
            + "------- "
            + ChatColor.AQUA
            + team.getName()
            + ChatColor.GOLD
            + " -------");
    for (TournamentPlayer player : team.getPlayers()) {
      String playerName =
          player.getUUID().toString() + ChatColor.GRAY + " (player hasn't logged on)";
      OfflinePlayer offline = Bukkit.getOfflinePlayer(player.getUUID());
      if (offline.getName() != null) playerName = offline.getName() + " - " + player.getUUID();

      sender.sendMessage(ChatColor.AQUA + "- " + playerName);
    }
  }

  @CommandMethod("add <uuid> <team>")
  @CommandDescription("Add a player to a team")
  @CommandPermission("events.staff")
  public void add(
      CommandSender sender, @Argument("uuid") String uuid, @Argument("team") @Greedy String name) {
    TournamentTeam team = EventsPlugin.get().getTeamRegistry().getTeam(name);
    if (team == null) {
      sender.sendMessage(ChatColor.RED + "Team not found!");
      return;
    }

    UUID uuidObject = UUID.fromString(uuid);

    if (team.containsPlayer(uuidObject)) {
      sender.sendMessage(ChatColor.RED + "That player is already on that team!");
      return;
    }

    TournamentPlayer player = TournamentPlayer.create(uuidObject, true);
    team.getPlayers().add(player);
  }

  @CommandMethod("remove <uuid> <team>")
  @CommandDescription("Remove a player to a team")
  @CommandPermission("events.staff")
  public void remove(
      CommandSender sender, @Argument("uuid") String uuid, @Argument("team") @Greedy String name) {
    TournamentTeam team = EventsPlugin.get().getTeamRegistry().getTeam(name);
    if (team == null) {
      sender.sendMessage(ChatColor.RED + "Team not found!");
      return;
    }

    UUID uuidObject = UUID.fromString(uuid);

    if (!team.containsPlayer(uuidObject)) {
      sender.sendMessage(ChatColor.RED + "That player is not on that team!");
      return;
    }

    Optional<TournamentPlayer> player =
        team.getPlayers().stream().filter(p -> p.getUUID().equals(uuidObject)).findFirst();

    player.ifPresent(p -> team.getPlayers().remove(p));
  }

  @CommandMethod("unregisterall")
  @CommandDescription("Clear all registered teams")
  @CommandPermission("events.staff")
  public void clear(CommandSender sender, TournamentTeamManager teamManager) {
    teamManager.clear();
    sender.sendMessage(ChatColor.YELLOW + "Unregistered all teams!");
  }

  @CommandMethod("setup <format> <teams>")
  @CommandDescription("Sets up a match")
  @CommandPermission("events.staff")
  public void setup(
      CommandSender sender,
      @Argument("format") String format,
      @Argument("teams") @Greedy String teams) {
    String team1 = teams.split(",")[0];
    String team2 = teams.split(",")[1];
    ((Player) sender).performCommand("tm unregisterall");
    ((Player) sender).performCommand("tm register " + team1);
    ((Player) sender).performCommand("tm register " + team2);
    ((Player) sender).performCommand("tm create " + format);
  }
}
