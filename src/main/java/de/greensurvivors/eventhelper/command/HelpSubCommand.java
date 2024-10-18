package de.greensurvivors.eventhelper.command;

import de.greensurvivors.eventhelper.EventHelper;
import de.greensurvivors.eventhelper.messages.LangPath;
import de.greensurvivors.eventhelper.messages.SharedLangPath;
import de.greensurvivors.eventhelper.messages.SharedPlaceHolder;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HelpSubCommand extends ASubCommand {
    private final static @NotNull Permission permission = new Permission("eventhelper.command.help", PermissionDefault.OP);
    private final @NotNull MainCmd mainCmd;

    protected HelpSubCommand(@NotNull EventHelper plugin, @NotNull Permission parentPermission, @NotNull MainCmd mainCmd) {
        super(plugin, permission);
        this.mainCmd = mainCmd;

        permission.addParent(parentPermission, true);
    }

    @Override
    public @NotNull Set<@NotNull String> getAliases() {
        return Set.of("help");
    }

    @Override
    public @NotNull LangPath getHelpTextPath() {
        return SharedLangPath.HELP_HELP_TEXT;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args) {
        if (!args.isEmpty()) {
            final @Nullable ASubCommand subCommand = mainCmd.registeredCommandMap.get(args.get(0).toLowerCase(Locale.ENGLISH));

            if (subCommand != null) {
                plugin.getMessageManager().sendLang(sender, subCommand.getHelpTextPath());
            } else {
                plugin.getMessageManager().sendLang(sender, SharedLangPath.ARG_NOT_A_SUBCMD,
                    Placeholder.unparsed(SharedPlaceHolder.ARGUMENT.getKey(), args.get(0)));
            }

            return true;
        }

        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull LinkedList<@NotNull String> args) {
        if (args.isEmpty() || args.size() == 1) {
            return mainCmd.registeredCommandMap.keySet().stream().filter(alias -> alias.startsWith(args.get(0))).toList();
        }

        return List.of();
    }
}
