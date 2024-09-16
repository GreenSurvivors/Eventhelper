package de.greensurvivors.eventhelper.messages;

import org.jetbrains.annotations.NotNull;

/**
 * Paths of all translatable
 */
public enum StandardLangPath implements LangPath {
    //plugin prefix
    PLUGIN_PREFIX("prefix", "<gold>[EventHelper]</gold> "),
    ERROR_SIGN_BACKSIDE("error.sign.backside"),// todo
    ERROR_WHAT("error.unknownError"),  // todo
    // list builder
    LIST_HEADER_PAGED("list.header.paged"),
    LIST_HEADER_PLAIN("list.header.plain"),
    LIST_FOOTER_OUTER("list.footer.outer"),
    LIST_FOOTER_INNER("list.footer.inner"),
    LIST_FOOTER_BACK("list.footer.back"),
    LIST_FOOTER_NEXT("list.footer.next"),
    LIST_FOOTER_NONE("list.footer.none"),
    // user cmd args errors
    NOT_A_PLAYER("cmd.error.self-not-a-player"),
    ARG_NOT_A_BOOL("cmd.error.arg.not-a-bool"),
    ARG_NOT_A_NUMBER("cmd.error.arg.not-a-number"),
    ARG_NOT_A_PLAYER("cmd.error.arg.not-a-player"),
    NOT_ENOUGH_ARGS("cmd.error.arg.not-enough-args"),
    UNKNOWN_ARG("cmd.error.arg.unknown"),
    NO_PERMISSION("no-permission"),
    // reload subcommand
    CMD_SUB_RELOAD_SUCCESS("cmd.sub.reload.success"),
    CMD_SUB_RELOAD_HELP("cmd.sub.reload.help");

    private final @NotNull String path;
    private final @NotNull String defaultValue;

    StandardLangPath(@NotNull String path) {
        this.path = path;
        this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
    }

    StandardLangPath(@NotNull String path, @NotNull String defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getDefaultValue() {
        return defaultValue;
    }
}