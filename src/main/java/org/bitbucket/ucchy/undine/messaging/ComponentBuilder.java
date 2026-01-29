/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.messaging;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;

/**
 * Adventure Component のビルダーユーティリティ。
 * MessagingUtility の MessageComponent/MessageParts を置き換えます。
 * @author ucchy
 */
public class ComponentBuilder {

    private TextComponent.Builder builder;

    /**
     * コンストラクタ
     */
    public ComponentBuilder() {
        this.builder = Component.text();
    }

    /**
     * テキストを追加
     * @param text テキスト
     * @return このビルダー
     */
    public ComponentBuilder addText(String text) {
        builder.append(Component.text(text));
        return this;
    }

    /**
     * 色付きテキストを追加
     * @param text テキスト
     * @param color 色
     * @return このビルダー
     */
    public ComponentBuilder addText(String text, ChatColor color) {
        if (color == ChatColor.BOLD) {
            builder.append(Component.text(text).decorate(TextDecoration.BOLD));
        } else if (color == ChatColor.ITALIC) {
            builder.append(Component.text(text).decorate(TextDecoration.ITALIC));
        } else if (color == ChatColor.UNDERLINE) {
            builder.append(Component.text(text).decorate(TextDecoration.UNDERLINED));
        } else if (color == ChatColor.STRIKETHROUGH) {
            builder.append(Component.text(text).decorate(TextDecoration.STRIKETHROUGH));
        } else {
            builder.append(Component.text(text, convertColor(color)));
        }
        return this;
    }

    /**
     * クリック可能なボタンを追加（RUN_COMMAND）
     * @param label ボタンラベル
     * @param color 色
     * @param command 実行するコマンド
     * @return このビルダー
     */
    public ComponentBuilder addButton(String label, ChatColor color, String command) {
        Component button = Component.text(label)
            .color(convertColor(color))
            .clickEvent(ClickEvent.runCommand(command));
        builder.append(button);
        return this;
    }

    /**
     * クリック可能なボタンを追加（RUN_COMMAND + ホバーテキスト）
     * @param label ボタンラベル
     * @param color 色
     * @param command 実行するコマンド
     * @param hoverText ホバーテキスト
     * @return このビルダー
     */
    public ComponentBuilder addButton(String label, ChatColor color, String command, String hoverText) {
        Component button = Component.text(label)
            .color(convertColor(color))
            .clickEvent(ClickEvent.runCommand(command))
            .hoverEvent(HoverEvent.showText(Component.text(hoverText)));
        builder.append(button);
        return this;
    }

    /**
     * SUGGEST_COMMAND のボタンを追加
     * @param label ボタンラベル
     * @param color 色
     * @param command 提案するコマンド
     * @return このビルダー
     */
    public ComponentBuilder addSuggestButton(String label, ChatColor color, String command) {
        Component button = Component.text(label)
            .color(convertColor(color))
            .clickEvent(ClickEvent.suggestCommand(command));
        builder.append(button);
        return this;
    }

    /**
     * SUGGEST_COMMAND のボタンを追加（ホバーテキスト付き）
     * @param label ボタンラベル
     * @param color 色
     * @param command 提案するコマンド
     * @param hoverText ホバーテキスト
     * @return このビルダー
     */
    public ComponentBuilder addSuggestButton(String label, ChatColor color, String command, String hoverText) {
        Component button = Component.text(label)
            .color(convertColor(color))
            .clickEvent(ClickEvent.suggestCommand(command))
            .hoverEvent(HoverEvent.showText(Component.text(hoverText)));
        builder.append(button);
        return this;
    }

    /**
     * ホバーテキスト付きテキストを追加
     * @param label ラベル
     * @param color 色
     * @param hoverText ホバーテキスト
     * @return このビルダー
     */
    public ComponentBuilder addHoverText(String label, ChatColor color, String hoverText) {
        Component text = Component.text(label)
            .color(convertColor(color))
            .hoverEvent(HoverEvent.showText(Component.text(hoverText)));
        builder.append(text);
        return this;
    }

    /**
     * 既存の Component を追加
     * @param component コンポーネント
     * @return このビルダー
     */
    public ComponentBuilder append(Component component) {
        builder.append(component);
        return this;
    }

    /**
     * コンポーネントをビルド
     * @return ビルドされた Component
     */
    public Component build() {
        return builder.build();
    }

    /**
     * ChatColor を TextColor に変換
     * @param color ChatColor
     * @return TextColor
     */
    public static TextColor convertColor(ChatColor color) {
        return switch (color) {
            case AQUA -> NamedTextColor.AQUA;
            case BLACK -> NamedTextColor.BLACK;
            case BLUE -> NamedTextColor.BLUE;
            case DARK_AQUA -> NamedTextColor.DARK_AQUA;
            case DARK_BLUE -> NamedTextColor.DARK_BLUE;
            case DARK_GRAY -> NamedTextColor.DARK_GRAY;
            case DARK_GREEN -> NamedTextColor.DARK_GREEN;
            case DARK_PURPLE -> NamedTextColor.DARK_PURPLE;
            case DARK_RED -> NamedTextColor.DARK_RED;
            case GOLD -> NamedTextColor.GOLD;
            case GRAY -> NamedTextColor.GRAY;
            case GREEN -> NamedTextColor.GREEN;
            case LIGHT_PURPLE -> NamedTextColor.LIGHT_PURPLE;
            case RED -> NamedTextColor.RED;
            case WHITE -> NamedTextColor.WHITE;
            case YELLOW -> NamedTextColor.YELLOW;
            default -> NamedTextColor.WHITE;
        };
    }
}
