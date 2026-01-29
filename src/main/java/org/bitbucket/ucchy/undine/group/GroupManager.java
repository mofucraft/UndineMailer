/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import net.kyori.adventure.text.Component;
import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.bridge.PermissionsExBridge;
import org.bitbucket.ucchy.undine.command.GroupCommand;
import org.bitbucket.ucchy.undine.command.ListCommand;
import org.bitbucket.ucchy.undine.command.UndineCommand;
import org.bitbucket.ucchy.undine.messaging.ComponentBuilder;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderConsole;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

/**
 * メールグループ管理クラス
 * @author ucchy
 */
public class GroupManager {

    private static final ArrayList<String> PEX_OPTION_FLAGS;
    static {
        PEX_OPTION_FLAGS = new ArrayList<String>();
        PEX_OPTION_FLAGS.add("recieve-mail");
        PEX_OPTION_FLAGS.add("receive-mail");
    }

    private static final String PEX_OPTION_SENDMODE = "send-mode";
    private static final int PAGE_SIZE = 10;

    private static final String COMMAND = GroupCommand.COMMAND;
    private static final String PERMISSION = GroupCommand.PERMISSION;
    private static final String PERMISSION_INFINITE_CREATE = GroupCommand.PERMISSION_INFINITE_CREATE;
    private static final String PERMISSION_INFINITE_ADD_MEMBER = GroupCommand.PERMISSION_INFINITE_ADD_MEMBER;

    private UndineMailer parent;
    private HashMap<String, GroupData> groups;
    private HashMap<String, GroupData> pexGroupsCache;

    /**
     * コンストラクタ
     * @param parent
     */
    public GroupManager(UndineMailer parent) {
        this.parent = parent;
        reload();
    }

    /**
     * 全データを再読み込みする
     */
    public void reload() {

        long start = System.currentTimeMillis();

        File folder = parent.getGroupFolder();
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });

        groups = new HashMap<String, GroupData>();

        if ( files != null ) {
            for ( File f : files ) {
                GroupData group = GroupData.loadFromFile(f);
                groups.put(group.getName().toLowerCase(), group);
            }
        }

        // 特殊グループを追加する
        GroupData all = new SpecialGroupAll();
        groups.put(all.getName().toLowerCase(), all);
        GroupData allConnected = new SpecialGroupAllConnected();
        groups.put(allConnected.getName().toLowerCase(), allConnected);
        GroupData allLogin = new SpecialGroupAllLogin();
        groups.put(allLogin.getName().toLowerCase(), allLogin);

        // アップグレード処理
        start = System.currentTimeMillis();

        int total = 0;
        for ( GroupData group : groups.values() ) {
            if ( group.upgrade() ) {
                saveGroupData(group);
                total++;
            }
        }

        if ( total > 0 ) {
            UndineMailer.getInstance().getLogger().info("Upgrade group data... Done.  Time: "
                    + (System.currentTimeMillis() - start) + "ms, Data: " + total + ".");
        }
    }

    /**
     * グループを追加する。
     * 重複するグループ名が既に追加されている場合は、
     * 古いグループが上書きされてしまうことに注意する。
     * @param group グループ
     */
    public void addGroup(GroupData group) {
        String name = group.getName().toLowerCase();
        groups.put(name, group);
        saveGroupData(group);
    }

    /**
     * 指定したグループ名のグループを取得する
     * @param name グループ名
     * @return グループ
     */
    public GroupData getGroup(String name) {
        name = name.toLowerCase();

        // PEXから取得する
        if ( name.startsWith(SpecialGroupPex.NAME_PREFIX) && pexGroupsCache != null ) {
            if ( pexGroupsCache.containsKey(name) ) {
                return pexGroupsCache.get(name);
            }
        }

        // グループから取得する
        if ( groups.containsKey(name) ) {
            return groups.get(name);
        }
        return null;
    }

    /**
     * 指定したグループ名のグループを削除する
     * @param name グループ名
     */
    public void removeGroup(String name) {
        name = name.toLowerCase();
        if ( groups.containsKey(name) ) {
            groups.remove(name);
            File folder = parent.getGroupFolder();
            File file = new File(folder, name + ".yml");
            file.delete();
        }
    }

    /**
     * 全てのグループ名を取得する
     * @return 全てのグループ名
     */
    public ArrayList<String> getAllGroupNames() {
        ArrayList<String> names = new ArrayList<String>();
        for ( GroupData group : groups.values() ) {
            names.add(group.getName());
        }
        return names;
    }

    /**
     * 全てのグループを取得する
     * @return 全てのグループ
     */
    public ArrayList<GroupData> getAllGroups() {
        return new ArrayList<GroupData>(groups.values());
    }

    /**
     * 指定されたグループ名は既に存在するかどうかを確認する
     * @return 存在するかどうか
     */
    public boolean existGroupName(String name) {
        return groups.keySet().contains(name.toLowerCase());
    }

    /**
     * 指定したグループを実データファイルに保存する
     * @param group グループ
     */
    public void saveGroupData(GroupData group) {
        File folder = parent.getGroupFolder();
        File file = new File(folder, group.getName().toLowerCase() + ".yml");
        group.saveToFile(file);
    }

    /**
     * グループ名として使用できる名前かどうかを確認する
     * @param name グループ名
     * @return 使用可能かどうか
     */
    public static boolean canUseNameFromGroup(String name) {
        return name.matches("[^\\\\/\\?\\*:\\|\\\"<>\\.]{1,15}");
    }

    /**
     * 指定したsenderがオーナーのグループの個数を返す
     * @param sender
     * @return オーナーのグループの個数
     */
    public int getOwnerGroupCount(MailSender sender) {
        int total = 0;
        for ( GroupData group : groups.values() ) {
            if ( group.getOwner().equals(sender) ) total++;
        }
        return total;
    }

    /**
     * 指定したsenderは新規にグループを作成できるかどうかを返す
     * @param sender
     * @return 新規にグループを作成できるかどうか
     */
    public boolean canMakeNewGroup(MailSender sender) {
        if ( sender.hasPermission(PERMISSION_INFINITE_CREATE) ) return true;
        if ( !sender.hasPermission(PERMISSION + ".create") ) return false;
        int num = getOwnerGroupCount(sender);
        int limit = parent.getUndineConfig().getMaxCreateGroup();
        return ( num < limit );
    }

    /**
     * グループ一覧画面で表示する項目として、指定されたsenderが表示可能なグループを返す
     * @param sender 取得対象のsender
     * @return senderが宛先として送信可能なグループと、senderがメンバーのグループ、の和。
     */
    public ArrayList<GroupData> getGroupsForList(MailSender sender) {

        ArrayList<GroupData> results = new ArrayList<GroupData>();

        for ( GroupData group : groups.values() ) {
            if ( group.isMember(sender) || group.canSend(sender) ) {
                results.add(group);
            }
        }

        Collections.sort(results, new Comparator<GroupData>() {
            public int compare(GroupData o1, GroupData o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        return results;
    }

    /**
     * メール宛先で表示する項目として、指定されたsenderが送信可能なグループを返す
     * @param sender 取得対象のsender
     * @return senderが宛先として送信可能なグループ
     */
    public ArrayList<GroupData> getGroupsForSelection(MailSender sender) {

        ArrayList<GroupData> results = new ArrayList<GroupData>();

        for ( GroupData group : groups.values() ) {
            if ( group.canSend(sender) ) {
                results.add(group);
            }
        }

        Collections.sort(results, new Comparator<GroupData>() {
            public int compare(GroupData o1, GroupData o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        // PermissionsExから動的にグループを取得して結合する
        for ( GroupData group : getPexGroups() ) {
            if ( group.canSend(sender) ) {
                results.add(group);
            }
        }

        return results;
    }

    /**
     * 指定されたsenderに、Groupリストを表示する。
     * @param sender 表示対象のsender
     * @param page 表示するページ(1から始まることに注意)
     */
    public void displayGroupList(MailSender sender, int page) {

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }

        String pre = Messages.get("ListVerticalParts");
        String parts = Messages.get("ListHorizontalParts");

        ArrayList<GroupData> list = getGroupsForList(sender);
        int max = (int)((list.size() - 1) / PAGE_SIZE) + 1;

        String title = Messages.get("GroupListTitle", "%num", list.size());
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        for ( int i=0; i<10; i++ ) {

            int index = (page - 1) * 10 + i;
            if ( index < 0 || list.size() <= index ) {
                continue;
            }

            GroupData group = list.get(index);

            ComponentBuilder builder = new ComponentBuilder();
            builder.addText(pre);

            builder.addButton("[" + group.getName() + "]", ChatColor.AQUA,
                    COMMAND + " detail " + group.getName(), group.getHoverText());

            if ( group instanceof SpecialGroupAll ) {
                builder.addText(" " + Messages.get("GroupSpecialAllSummary"));
            } else {
                builder.addText(" " + Messages.get("GroupListSummayLine",
                        new String[]{"%owner", "%num"},
                        new String[]{group.getOwner().getName(), group.getMembers().size() + ""}));
            }

            sendMessageComponent(builder.build(), sender);
        }

        if ( canMakeNewGroup(sender) ) {
            ComponentBuilder builder = new ComponentBuilder();
            builder.addText(pre);

            builder.addSuggestButton(Messages.get("GroupMakeNewGroup"), ChatColor.AQUA,
                    COMMAND + " create ", Messages.get("GroupMakeNewGroupToolTip"));

            sendMessageComponent(builder.build(), sender);
        }

        sendPager(sender, COMMAND + " list", "", page, max,
                Messages.get("ListHorizontalParts"), null);
    }

    /**
     * 指定されたsenderに、Group選択リストを表示する。
     * @param sender 表示対象のsender
     * @param page 表示するページ(1から始まることに注意)
     * @param next 選択したときに実行するコマンド
     */
    public void displayGroupSelection(MailSender sender, int page, String next) {

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }

        String pre = Messages.get("ListVerticalParts");
        String parts = Messages.get("ListHorizontalParts");

        ArrayList<GroupData> list = getGroupsForSelection(sender);
        int max = (int)((list.size() - 1) / PAGE_SIZE) + 1;

        String title = Messages.get("GroupListTitle", "%num", list.size());
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        for ( int i=0; i<10; i++ ) {

            int index = (page - 1) * 10 + i;
            if ( index < 0 || list.size() <= index ) {
                continue;
            }

            GroupData group = list.get(index);

            ComponentBuilder builder = new ComponentBuilder();
            builder.addText(pre);

            builder.addButton("[" + group.getName() + "]", ChatColor.AQUA,
                    next + " " + group.getName(), group.getHoverText());

            if ( group instanceof SpecialGroupAll ) {
                builder.addText(" " + Messages.get("GroupSpecialAllSummary"));
            } else {
                builder.addText(" " + Messages.get("GroupListSummayLine",
                        new String[]{"%owner", "%num"},
                        new String[]{group.getOwner().getName(), group.getMembers().size() + ""}));
            }

            sendMessageComponent(builder.build(), sender);
        }

        if ( canMakeNewGroup(sender) ) {
            ComponentBuilder builder = new ComponentBuilder();
            builder.addText(pre);

            builder.addHoverText(Messages.get("GroupMakeNewGroup"), ChatColor.WHITE,
                    Messages.get("GroupMakeNewGroupToolTipForSelection"));

            sendMessageComponent(builder.build(), sender);
        }

        sendPager(sender, COMMAND + " list", " " + next, page, max,
                Messages.get("ListHorizontalParts"),
                UndineCommand.COMMAND + " write");
    }

    /**
     * 指定されたsenderに、groupの詳細(ReadOnly)を表示する
     * @param sender 表示対象のsender
     * @param group 表示するグループ
     */
    public void displayGroupDetailReadOnly(MailSender sender, GroupData group) {

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }

        String pre = Messages.get("DetailVerticalParts");
        String parts = Messages.get("DetailHorizontalParts");

        String title = Messages.get("GroupDetailTitle", "%name", group.getName());
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        sender.sendMessage(pre + Messages.get(
                "GroupOwnerLine", "%owner", group.getOwner().getName()));
        sender.sendMessage(pre + Messages.get("GroupMemberLine"));

        if ( group instanceof SpecialGroupAll ) {
            sender.sendMessage(pre + "  "
                    + ChatColor.WHITE + Messages.get("GroupSpecialAllMembers"));

        } else {

            // メンバーを5人ごとに区切って表示する
            ArrayList<MailSender> members = group.getMembers();
            Collections.sort(members, new Comparator<MailSender>() {
                public int compare(MailSender o1, MailSender o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
            int size = members.size();
            int max = (int)((size - 1) / 5) + 1;
            for ( int i=0; i<max; i++ ) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(pre + "  " + ChatColor.WHITE);
                for ( int j=0; j<5; j++ ) {
                    int index = i * 5 + j;
                    if ( index < size ) {
                        buffer.append(members.get(index).getName() + ", ");
                    }
                }
                sender.sendMessage(buffer.toString());
            }
        }

        ComponentBuilder builder = new ComponentBuilder();
        builder.addText(parts + parts + " ");
        builder.addButton(Messages.get("Return"), ChatColor.AQUA, COMMAND + " list");
        builder.addText(" " + parts + parts);
        sendMessageComponent(builder.build(), sender);
    }

    /**
     * 指定されたsenderに、groupの詳細(ModifyMode)を表示する
     * @param sender 表示対象のsender
     * @param group 表示するグループ
     * @param page 表示するページ(1から始まることに注意)
     */
    public void displayGroupDetailModifyMode(MailSender sender, GroupData group, int page) {

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }

        String pre = Messages.get("DetailVerticalParts");
        String parts = Messages.get("DetailHorizontalParts");

        String title = Messages.get("GroupDetailTitle", "%name", group.getName());
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        sender.sendMessage(pre + Messages.get(
                "GroupOwnerLine", "%owner", group.getOwner().getName()));
        sender.sendMessage(pre + Messages.get("GroupMemberLine"));

        ArrayList<MailSender> members = group.getMembers();
        Collections.sort(members, new Comparator<MailSender>() {
            public int compare(MailSender o1, MailSender o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        int size = members.size();
        int max = (int)((size - 1) / PAGE_SIZE) + 1;

        // メンバーの一覧を表示する
        for ( int i=0; i<PAGE_SIZE; i++ ) {

            int index = (page - 1) * PAGE_SIZE + i;
            if( index < 0 || index >= members.size() ) continue;

            MailSender member = members.get(index);

            ComponentBuilder builder = new ComponentBuilder();
            builder.addText(pre + "  ");

            if ( !group.getOwner().equals(member) ) {
                builder.addButton(Messages.get("GroupDeleteMemberButton"), ChatColor.AQUA,
                        COMMAND + " remove " + group.getName() + " " + member.getName(),
                        Messages.get("GroupDeleteMemberToolTip"));
            } else {
                builder.addHoverText(Messages.get("GroupDeleteMemberButton"), ChatColor.WHITE,
                        Messages.get("GroupDeleteMemberOwnerToolTip"));
            }

            builder.addText(member.getName());

            sendMessageComponent(builder.build(), sender);
        }

        // メンバー追加ボタンと、設定ボタンを置く
        if ( group.getMembers().size() < parent.getUndineConfig().getMaxGroupMember()
                || sender.hasPermission(PERMISSION_INFINITE_ADD_MEMBER) ) {

            ComponentBuilder builder = new ComponentBuilder();
            builder.addText(pre);

            if ( !parent.getUndineConfig().isEnablePlayerList() ) {
                builder.addSuggestButton(Messages.get("GroupAddMember"), ChatColor.AQUA,
                        COMMAND + " add " + group.getName() + " ");
            } else {
                builder.addButton(Messages.get("GroupAddMemberAddress"), ChatColor.AQUA,
                        ListCommand.COMMAND_INDEX + " " + COMMAND + " add " + group.getName() + " ");
            }

            sendMessageComponent(builder.build(), sender);

            ComponentBuilder builder2 = new ComponentBuilder();
            builder2.addText(pre);

            builder2.addButton(Messages.get("GroupAddMemberAllLogin"), ChatColor.AQUA,
                    COMMAND + " addalllogin " + group.getName());

            sendMessageComponent(builder2.build(), sender);
        }

        ComponentBuilder builder = new ComponentBuilder();
        builder.addText(pre);
        builder.addButton(Messages.get("GroupChangeSetting"), ChatColor.AQUA,
                COMMAND + " detail " + group.getName() + " setting");

        sendMessageComponent(builder.build(), sender);

        sendPager(sender, COMMAND + " detail " + group.getName(), "",
                page, max, Messages.get("DetailHorizontalParts"),
                COMMAND + " list");
    }

    /**
     * 指定されたsenderに、グループの設定変更画面を表示する
     * @param sender
     * @param group
     */
    public void displayGroupSetting(MailSender sender, GroupData group) {

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }

        String pre = Messages.get("DetailVerticalParts");
        String parts = Messages.get("DetailHorizontalParts");

        String title = Messages.get("GroupSettingTitle", "%name", group.getName());
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        GroupPermissionMode mode = group.getSendMode();
        sender.sendMessage(pre + Messages.get("GroupSendPerm")
                + ChatColor.WHITE + mode.getDisplayString() );

        ComponentBuilder builderSend = new ComponentBuilder();
        builderSend.addText(pre + " ");
        addChangeButtons(builderSend,
                COMMAND + " perm " + group.getName() + " send",
                (mode != GroupPermissionMode.OWNER),
                (mode != GroupPermissionMode.MEMBER),
                (mode != GroupPermissionMode.EVERYONE), true);
        sendMessageComponent(builderSend.build(), sender);

        mode = group.getModifyMode();
        sender.sendMessage(pre + Messages.get("GroupModifyPerm")
                + ChatColor.WHITE + mode.getDisplayString() );

        ComponentBuilder builderMod = new ComponentBuilder();
        builderMod.addText(pre + " ");
        addChangeButtons(builderMod,
                COMMAND + " perm " + group.getName() + " modify",
                (mode != GroupPermissionMode.OWNER),
                (mode != GroupPermissionMode.MEMBER),
                false, false);
        sendMessageComponent(builderMod.build(), sender);

        mode = group.getDissolutionMode();
        sender.sendMessage(pre + Messages.get("GroupDissolutionPerm")
                + ChatColor.WHITE + mode.getDisplayString() );

        ComponentBuilder builderDis = new ComponentBuilder();
        builderDis.addText(pre + " ");
        addChangeButtons(builderDis,
                COMMAND + " perm " + group.getName() + " dissolution",
                (mode != GroupPermissionMode.OWNER),
                (mode != GroupPermissionMode.MEMBER),
                false, false);
        sendMessageComponent(builderDis.build(), sender);

        if ( group.canBreakup(sender) ) {
            ComponentBuilder builder = new ComponentBuilder();
            builder.addText(pre);
            builder.addButton(Messages.get("GroupDeleteGroup"), ChatColor.AQUA,
                    COMMAND + " delete " + group.getName());
            sendMessageComponent(builder.build(), sender);
        }

        ComponentBuilder builder = new ComponentBuilder();
        builder.addText(pre);
        builder.addButton(Messages.get("GroupReturnToMemberList"), ChatColor.AQUA,
                COMMAND + " detail " + group.getName());
        sendMessageComponent(builder.build(), sender);

        sender.sendMessage(Messages.get("DetailLastLine"));
    }

    /**
     * 権限設定のメッセージコンポーネントにボタンを加える
     * @param builder
     * @param base
     * @param owner
     * @param member
     * @param everyone
     * @param evisible
     */
    private void addChangeButtons(ComponentBuilder builder, String base,
            boolean owner, boolean member, boolean everyone, boolean evisible) {

        builder.addText(" ");
        String ownerLabel = Messages.get("GroupPermChangeButton", "%perm",
                GroupPermissionMode.OWNER.getDisplayString());
        if ( owner ) {
            builder.addButton(ownerLabel, ChatColor.AQUA,
                    base + " " + GroupPermissionMode.OWNER);
        } else {
            builder.addText(ownerLabel, ChatColor.WHITE);
        }

        builder.addText(" ");
        String memberLabel = Messages.get("GroupPermChangeButton", "%perm",
                GroupPermissionMode.MEMBER.getDisplayString());
        if ( member ) {
            builder.addButton(memberLabel, ChatColor.AQUA,
                    base + " " + GroupPermissionMode.MEMBER);
        } else {
            builder.addText(memberLabel, ChatColor.WHITE);
        }

        if ( evisible ) {
            builder.addText(" ");
            String everyoneLabel = Messages.get("GroupPermChangeButton", "%perm",
                    GroupPermissionMode.EVERYONE.getDisplayString());
            if ( everyone ) {
                builder.addButton(everyoneLabel, ChatColor.AQUA,
                        base + " " + GroupPermissionMode.EVERYONE);
            } else {
                builder.addText(everyoneLabel, ChatColor.WHITE);
            }
        }
    }

    /**
     * ページャーを対象プレイヤーに表示する
     * @param sender 表示対象
     * @param commandPre コマンドのプレフィックス
     * @param commandSuf コマンドのサフィックス
     * @param page 現在のページ
     * @param max 最終ページ
     * @param parts ボタンの前後に表示する枠パーツ
     * @param returnCommand 戻るボタンに設定するコマンド、nullを指定したら戻るボタンは表示しない
     */
    private void sendPager(MailSender sender, String commandPre, String commandSuf,
            int page, int max,
            String parts, String returnCommand) {

        String firstLabel = Messages.get("FirstPage");
        String prevLabel = Messages.get("PrevPage");
        String nextLabel = Messages.get("NextPage");
        String lastLabel = Messages.get("LastPage");
        String firstToolTip = Messages.get("FirstPageToolTip");
        String prevToolTip = Messages.get("PrevPageToolTip");
        String nextToolTip = Messages.get("NextPageToolTip");
        String lastToolTip = Messages.get("LastPageToolTip");

        ComponentBuilder builder = new ComponentBuilder();

        builder.addText(parts + " ");

        if ( returnCommand != null ) {
            builder.addButton(Messages.get("Return"), ChatColor.AQUA,
                    returnCommand, Messages.get("ReturnToolTip"));
            builder.addText(" ");
        }

        if ( page > 1 ) {
            builder.addButton(firstLabel, ChatColor.AQUA,
                    commandPre + " 1" + commandSuf, firstToolTip);

            builder.addText(" ");

            builder.addButton(prevLabel, ChatColor.AQUA,
                    commandPre + " " + (page - 1) + commandSuf, prevToolTip);

        } else {
            builder.addText(firstLabel + " " + prevLabel, ChatColor.WHITE);
        }

        builder.addText(" (" + page + "/" + max + ") ");

        if ( page < max ) {
            builder.addButton(nextLabel, ChatColor.AQUA,
                    commandPre + " " + (page + 1) + commandSuf, nextToolTip);

            builder.addText(" ");

            builder.addButton(lastLabel, ChatColor.AQUA,
                    commandPre + " " + max + commandSuf, lastToolTip);

        } else {
            builder.addText(nextLabel + " " + lastLabel, ChatColor.WHITE);
        }

        builder.addText(" " + parts);

        sendMessageComponent(builder.build(), sender);
    }

    /**
     * PEXからグループを取得してGroupDataに変換して返す
     * @return PEXからインポートされたグループ
     */
    private ArrayList<GroupData> getPexGroups() {

        PermissionsExBridge pex = UndineMailer.getInstance().getPex();
        ArrayList<GroupData> results = new ArrayList<GroupData>();

        if ( pex == null ) {
            return results;
        }

        pexGroupsCache = new HashMap<String, GroupData>();
        for ( String group : pex.getGroupNamesByBooleanOption(PEX_OPTION_FLAGS) ) {
            GroupPermissionMode sendmode = GroupPermissionMode.getFromString(
                    pex.getGroupOptionAsString(group, PEX_OPTION_SENDMODE),
                    UndineMailer.getInstance().getUndineConfig().getSpecialGroupPexSendMode());
            GroupData gd = new SpecialGroupPex(group, sendmode);
            results.add(gd);
            pexGroupsCache.put(gd.getName().toLowerCase(), gd);
        }

        return results;
    }

    /**
     * 指定されたメッセージコンポーネントを、指定されたMailSenderに送信する。
     * @param msg メッセージコンポーネント
     * @param sender 送信先
     */
    private void sendMessageComponent(Component msg, MailSender sender) {
        if ( sender instanceof MailSenderPlayer && sender.isOnline() ) {
            sender.getPlayer().sendMessage(msg);
        } else if ( sender instanceof MailSenderConsole ) {
            Bukkit.getConsoleSender().sendMessage(msg);
        }
    }
}
