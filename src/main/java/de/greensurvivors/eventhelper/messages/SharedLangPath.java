package de.greensurvivors.eventhelper.messages;

import org.jetbrains.annotations.NotNull;

/**
 * Paths of all translatable
 */
public enum SharedLangPath implements LangPath {
    //plugin prefix
    MESSAGE_PREFIX("prefix", "<gold>[EventHelper]</gold> "),
    ERROR_SIGN_BACKSIDE("error.sign.backside"),
    ERROR_WHAT("error.unknownError"),
    // list builder
    LIST_HEADER_PAGED("list.header.paged"),
    LIST_HEADER_PLAIN("list.header.plain"),
    LIST_FOOTER_OUTER("list.footer.outer"),
    LIST_FOOTER_INNER("list.footer.inner"),
    LIST_FOOTER_BACK("list.footer.back"),
    LIST_FOOTER_NEXT("list.footer.next"),
    LIST_FOOTER_NONE("list.footer.none"),
    // user cmd args errors
    CMD_ERROR_SENDER_NOT_A_PLAYER("cmd.error.sender-not-a-player"),
    ARG_NOT_A_BOOL("cmd.error.arg.not-a-bool"),
    ARG_NOT_A_NUMBER("cmd.error.arg.not-a-number"),
    ARG_NOT_A_SUBCMD("cmd.error.arg.not-a-subcommand"),
    ARG_NOT_A_PLAYER("cmd.error.arg.not-a-player"),
    ARG_NOT_A_TIME("cmd.error.arg.not-a-time"),
    ARG_NUMBER_OUT_OF_BOUNDS("cmd.error.arg.number-out-of-bounds"),
    NOT_ENOUGH_ARGS("cmd.error.arg.not-enough-args"),
    UNKNOWN_ARG("cmd.error.arg.unknown"),
    NO_PERMISSION("no-permission"),
    HELP_HELP_TEXT("cmd.help.help-text"),
    RELOAD_HELP_TEXT("cmd.reload.help-text");

    private final @NotNull String path;
    private final @NotNull String defaultValue;

    SharedLangPath(@NotNull String path) {
        this.path = path;
        this.defaultValue = path; // we don't need to define a default value, but if something couldn't get loaded we have to return at least helpful information
    }

    SharedLangPath(@NotNull String path, @NotNull String defaultValue) {
        this.path = path;
        this.defaultValue = defaultValue;
    }

    public @NotNull String getPath() {
        return path;
    }

    public @NotNull String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public @NotNull String getModulName() {
        return "shared";
    }
}
