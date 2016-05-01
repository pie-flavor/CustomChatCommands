package flavor.pie;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.command.*;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.message.MessageChannelEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializer;
import org.spongepowered.api.text.serializer.TextSerializers;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

@Plugin(id="customchatcommands",name="CustomChatCommands",version="1.2")
public class CustomChatCommands {
	@Inject PluginContainer container;
	@Inject Game game;
	@Inject @DefaultConfig(sharedRoot=false) Path path;
	@Inject @DefaultConfig(sharedRoot=false) ConfigurationLoader<CommentedConfigurationNode> loader;
	@Inject Logger logger;
	CommentedConfigurationNode root;
	ArrayList<CommandMapping> mappings;
	Random rng;
	@Listener
	public void onEnable(GameStartingServerEvent e) {
		mappings = new ArrayList<>();
		rng = new Random();
		try {
			setupConfig();
		} catch (IOException | ObjectMappingException e1) {
			e1.printStackTrace();
			logger.error("Config unloadable! Disabling CustomChatCommands.");
			return;
		}
		CommandSpec spec = CommandSpec.builder().permission("ccc.reload").executor(this::reload).description(Text.of("Reloads the config and all commands.")).arguments(GenericArguments.none()).build();
		CommandSpec rootspec = CommandSpec.builder().executor(this::rootcmd).child(spec, "reload").build();
		game.getCommandManager().register(this, rootspec, "customchatcommands","ccc");
	}
	public CommandResult reload(CommandSource src, CommandContext args) {
		for (CommandMapping mapping : mappings) {
			game.getCommandManager().removeMapping(mapping);
		}
		mappings.clear();
		try {
			setupConfig();
		} catch (IOException | ObjectMappingException e1) {
			e1.printStackTrace();
			logger.error("Config unloadable! Canceling reload.");
			if (!(src.equals(game.getServer().getConsole()))) {
				src.sendMessage(Text.of(TextColors.RED, "Config unloadable! Canceling reload."));
			}
			return CommandResult.empty();
		}
		src.sendMessage(Text.of(TextColors.GREEN, "Reloaded all commands."));
		return CommandResult.success();
	}
	public CommandResult rootcmd(CommandSource src, CommandContext args) {
		Text message = TextSerializers.JSON.deserialize("{\"text\":\"Plugin by: \",\"color\": \"green\",\"extra\": [{\"text\":\"pie_flavor\",\"color\":\"dark_green\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://forums.spongepowered.org/users/pie_flavor/\"},\"hoverEvent\":{\"action\":\"show_text\",\"value\":\"Click to go to my Sponge page!\"}}]}");
		src.sendMessage(message);
		return CommandResult.success();
	}
	void setupConfig() throws IOException, ObjectMappingException {
		root = loader.load();
		if (root.getNode("version").getInt() < 4) {
			CommentedConfigurationNode node = root.getNode("me");
			node.setComment("The name of this node is the command name.");
			CommentedConfigurationNode args = node.getNode("args");
			args.setValue(1);
			args.setComment("Number of arguments in the command. The last argument is all the remaining ones joined together.");
			CommentedConfigurationNode message = node.getNode("message");
			message.setValue("{\"text\":\"* <player> <arg1>\",\"color\":\"yellow\"}");
			message.setComment("The message to send. <player> will be replaced with the player's name, and <arg#x> will be replaced with the command's argument at position x. This can be in ampersand, json, or xml format.");
			CommentedConfigurationNode aliases = node.getNode("aliases");
			aliases.setValue(Arrays.asList("action"));
			aliases.setComment("Various aliases for this command. Optional.");
			CommentedConfigurationNode permission = node.getNode("permission");
			permission.setValue("minecraft.command.me");
			permission.setComment("The permission to run this command. Optional.");
			CommentedConfigurationNode radius = node.getNode("radius");
			radius.setValue(0);
			radius.setComment("The radius in which the message can be heard. Set to 0 for standard chat, -1 for server-wide, or a valid radius for, you know, the radius. Optional, defaults to 0.");
			CommentedConfigurationNode format = node.getNode("format");
			format.setValue("json");
			format.setComment("What format the 'message' node is in. Can be 'json', 'ampersand', or 'xml'.");
			CommentedConfigurationNode example = node.getNode("example");
			example.setValue(true);
			example.setComment("These are examples, and as such have this 'example' tag denoting that they are not to be loaded. Remove this if you want to use these!");
			CommentedConfigurationNode xml = root.getNode("advertise");
			xml.setComment("Example of TextXML format");
			xml.getNode("args").setValue(0);
			xml.getNode("permission").setValue("minecraft.command.say");
			xml.getNode("format").setValue("xml");
			xml.getNode("radius").setValue(-1);
			CommentedConfigurationNode xmlmessage = xml.getNode("message");
			xmlmessage.setValue("<c n=\"green\">You are playing on the LameNameCraft server, hope you're having fun!</c> <c n=\"blue\">Click <a href=\"http://google.com\"><b>here</b></a> to go to our server store!</c>");
			xmlmessage.setComment("This would probably be on a timer of some sort.");
			xml.getNode("example").setValue(true);
			CommentedConfigurationNode ampersand = root.getNode("broadcast");
			ampersand.setComment("Example of ampersand format");
			ampersand.getNode("args").setValue(1);
			ampersand.getNode("permission").setValue("minecraft.command.say");
			ampersand.getNode("format").setValue("ampersand");
			ampersand.getNode("radius").setValue(-1);
			ampersand.getNode("message").setValue("&c[&a&l<player>&c] &b<arg1>");
			ampersand.getNode("example").setValue(true);
			CommentedConfigurationNode defualt = root.getNode("default");
			defualt.setComment("If this node is present, it will change the format of normal chat. If this node does not exist, chat is unaffected. Only 'format' and 'message' are supported. Take note: As of 1.2 this replaces the chat message rather than the formatter, so this will break any plugins that modify your format.");
			CommentedConfigurationNode defualtmessage = defualt.getNode("message");
			defualtmessage.setValue("&f<<player>> <arg1>");
			defualtmessage.setComment("There is only one argument, which is the chat message.");
			defualt.getNode("format").setValue("ampersand");
			defualt.getNode("example").setValue(true);
			root.getNode("version").setValue(4);
			loader.save(root);
		}
		for (CommentedConfigurationNode node : root.getChildrenMap().values()) {
			if (node.getKey().equals("version") || node.getKey().equals("default")) continue;
			if (node.getNode("example").getBoolean()) continue;
			if (!(node.getKey() instanceof String) || node.getNode("args").getInt() < 0 || node.getNode("message").getString() == null || node.getNode("radius").getInt() < -1) {
				logger.info("Skipping configuration node '"+node.getKey()+"' because it is incorrectly formatted");
				continue;
			}
			String s = (String) node.getKey();
			int argsCount = node.getNode("args").getInt();
			String message = node.getNode("message").getString();
			List<String> list = node.getNode("aliases").getList(TypeToken.of(String.class));
			ArrayList<String> aliases = list == null ? new ArrayList<>() : new ArrayList<>(list);
			String format = node.getNode("format").getString();
			aliases.add(0, s);
			String permission = node.getNode("permission").getString();
			int radius = node.getNode("radius").getInt();
			CommandSpec.Builder spec = CommandSpec.builder()
					.arguments((argsCount == 0 ? GenericArguments.none() : (argsCount > 1 ? GenericArguments.seq(
							GenericArguments.repeated(GenericArguments.string(Text.of("args")), argsCount-1),
							GenericArguments.remainingJoinedStrings(Text.of("message"))
							) : GenericArguments.remainingJoinedStrings(Text.of("message"))))
							).executor((src, context) -> {
								Collection<String> args = context.<String>getAll("args");
								String toSend = message.replace("<player>", src.getName());
								int i = 1;
								for (String s1 : args) {
									toSend = toSend.replace("<arg"+i+">", s1);
									i++;
								}
								Optional<String> opt = context.<String>getOne("message");
								if (opt.isPresent()) {
									toSend = toSend.replace("<arg"+i+">", opt.get());
								}
								Text text = null;
								if (format == null) {
									text = TextSerializers.JSON.deserialize(toSend);
								} else
									switch (format) {
									case "ampersand":
										text = TextSerializers.FORMATTING_CODE.deserialize(toSend);
										break;
									case "xml":
										text = TextSerializers.TEXT_XML.deserialize(toSend);
										break;
									case "json":
									default:
										text = TextSerializers.JSON.deserialize(toSend);
										break;
									}
								switch (radius) {
								case -1:
									game.getServer().getBroadcastChannel().send(text);
									break;
								case 0:
									src.getMessageChannel().send(text);
									break;
								default:
									if (!(src instanceof Player)) {
										src.getMessageChannel().send(text);
									} else {
										for (Entity p : ((Player) src).getWorld().getEntities((entity) -> {
											if (!(entity instanceof Player)) return false;
											Location<World> srcl = ((Player) src).getLocation();
											Location<World> pl = ((Player) entity).getLocation();
											return (pl.getX()-srcl.getX() < radius && pl.getZ() - srcl.getZ() < radius);
										})) {
											((Player) p).sendMessage(text);
										}
									}
								}
								return CommandResult.success();
							});
			if (permission != null) spec = spec.permission(permission);
			CommandSpec spec0 = spec.build();
			Optional<CommandMapping> opt = game.getCommandManager().register(this, spec0, aliases);
			if (opt.isPresent()) {
				CommandMapping mapping = opt.get();
				mappings.add(mapping);
			}
		}
	}
	@Listener
	public void onChat(MessageChannelEvent.Chat e, @First CommandSource src) {
		CommentedConfigurationNode node = root.getNode("default");
		if (node.isVirtual()) return;
		if (node.getNode("example").getBoolean()) return;
		String msg = node.getNode("message").getString();
		String format = node.getNode("format").getString();
		if (format == null || !(format.equals("xml") || format.equals("json") || format.equals("ampersand"))) {
			format = "json";
		}
		TextSerializer formatter = null;
		switch (format) {
		case "json":
			formatter = TextSerializers.JSON;
			break;
		case "xml":
			formatter = TextSerializers.TEXT_XML;
			break;
		case "ampersand":
			formatter = TextSerializers.FORMATTING_CODE;
			break;
		}
		msg = msg.replace("<player>", src.getName()).replace("<arg1>", e.getRawMessage().toPlain());
		Text text = formatter.deserialize(msg);
		e.setMessage(text);
	}
}
