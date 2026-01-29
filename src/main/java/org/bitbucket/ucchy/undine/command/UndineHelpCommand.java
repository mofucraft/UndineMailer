/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.command;

import java.util.List;

import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.messaging.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

/**
 * undine help コマンド
 * @author ucchy
 */
public class UndineHelpCommand implements SubCommand {

    private static final String NAME = "help";
    private static final String NODE = "undine." + NAME;
    private static final String PERMISSION_PREFIX = "undine.";

    /**
     * コマンドを取得します。
     * @return コマンド
     * @see org.bitbucket.ucchy.undine.command.SubCommand#getCommandName()
     */
    @Override
    public String getCommandName() {
        return NAME;
    }

    /**
     * パーミッションノードを取得します。
     * @return パーミッションノード
     * @see org.bitbucket.ucchy.undine.command.SubCommand#getPermissionNode()
     */
    @Override
    public String getPermissionNode() {
        return NODE;
    }

    /**
     * コマンドを実行します。
     * @param sender コマンド実行者
     * @param label 実行時のラベル
     * @param args 実行時の引数
     * @see org.bitbucket.ucchy.undine.command.SubCommand#runCommand(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public void runCommand(CommandSender sender, String label, String[] args) {

        String parts = Messages.get("ListHorizontalParts");
        String pre = Messages.get("ListVerticalParts");

        sender.sendMessage(parts + parts + " "
                + Messages.get("HelpTitle") + " " + parts + parts);

        // umailコマンドのヘルプ
        for ( String c : new String[]{
                "inbox", "outbox", "trash", "text", "write",
                "item", "reload"} ) {

            if ( !sender.hasPermission(PERMISSION_PREFIX + c) ) {
                continue;
            }

            ComponentBuilder builder = new ComponentBuilder();
            builder.addText(pre);

            String l = "[" + Messages.get("HelpCommand_" + c) + "]";
            if ( c.equals("text") ) {
                // undine text コマンドだけは、suggest_commandを設定する。
                builder.addSuggestButton(l, ChatColor.AQUA,
                        UndineCommand.COMMAND + " " + c);
            } else {
                builder.addButton(l, ChatColor.AQUA,
                        UndineCommand.COMMAND + " " + c);
            }

            builder.addText(" " + ChatColor.WHITE + Messages.get("HelpDescription_" + c));

            sender.sendMessage(builder.build());
        }

        // ugroupコマンドのヘルプ
        if ( sender.hasPermission(GroupCommand.PERMISSION + ".command") ) {

            ComponentBuilder builder = new ComponentBuilder();
            builder.addText(pre);

            String l = "[" + Messages.get("HelpCommand_group") + "]";
            builder.addButton(l, ChatColor.AQUA, GroupCommand.COMMAND);

            builder.addText(" " + ChatColor.WHITE + Messages.get("HelpDescription_group"));

            sender.sendMessage(builder.build());
        }

        // helpコマンドのヘルプ
        if ( sender.hasPermission(PERMISSION_PREFIX + "help") ) {
            ComponentBuilder builder = new ComponentBuilder();
            builder.addText(pre);

            String l = "[" + Messages.get("HelpCommand_help") + "]";
            builder.addButton(l, ChatColor.AQUA, UndineCommand.COMMAND + " help");

            builder.addText(" " + ChatColor.WHITE + Messages.get("HelpDescription_help"));

            sender.sendMessage(builder.build());
        }

        sender.sendMessage(Messages.get("ListLastLine"));
    }

    /**
     * TABキー補完を実行します。
     * @param sender コマンド実行者
     * @param args 補完時の引数
     * @return 補完候補
     * @see org.bitbucket.ucchy.undine.command.SubCommand#tabComplete(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
