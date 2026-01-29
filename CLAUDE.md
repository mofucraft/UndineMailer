# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

UndineMailer is a Minecraft Bukkit/Spigot plugin that provides an in-game mail system for players. Players can send/receive mail with text messages and item attachments, create mail groups, and use COD (Cash On Delivery) features.

- **License**: LGPL v3
- **Java Version**: 1.8
- **Bukkit API**: 1.13+

## Build Commands

```bash
# Compile
mvn clean compile

# Package (creates shaded JAR in target/)
mvn package

# Run tests
mvn test

# Run a single test
mvn -Dtest=UUIDResolverTest test

# Full deploy (default goal: clean javadoc:jar source:jar deploy)
mvn
```

The build uses maven-shade-plugin to bundle dependencies (MessagingUtility, ItemConfigUtility, gson) into the final JAR with relocations.

## Architecture

### Main Plugin Entry Point
- [UndineMailer.java](src/main/java/org/bitbucket/ucchy/undine/UndineMailer.java) - Main plugin class extending JavaPlugin. Initializes managers and handles command routing.

### Core Managers
- **MailManager** - Handles mail send/receive, inbox/outbox/trash display, and mail persistence. Loads mail data asynchronously on startup.
- **AttachmentBoxManager** - Manages item attachments to mails via inventory GUI
- **GroupManager** - Manages mail recipient groups (create, delete, add/remove members)
- **PlayerUuidCache** - Caches player name-to-UUID mappings

### Data Models
- **MailData** - Single mail entity with recipients, sender, message, attachments, COD settings, read/trash flags. Persisted to YAML.
- **GroupData** - Mail group with owner, members, and permission modes (send/modify/dissolution)
- **MailSender** (abstract) - Polymorphic sender type with implementations:
  - MailSenderPlayer - Regular player
  - MailSenderConsole - Server console
  - MailSenderBlock - Command block

### Command Structure
Commands are in `org.bitbucket.ucchy.undine.command` package:
- **UndineCommand** (`/mail`) - Main command with subcommands (inbox, outbox, trash, write, send, etc.)
- **ListCommand** (`/undinelist`) - Player index for recipient selection
- **GroupCommand** (`/undinegroup`) - Group management

Each subcommand extends **SubCommand** abstract class.

### Data Storage
Data is stored in YAML files under the plugin data folder:
- `mail/` - Mail data files (format: `%08d.yml`)
- `group/` - Group data files
- `cache/` - UUID cache data
- `editmails.yml` - Draft mails saved on server shutdown

### Optional Integrations
- **Vault** - Economy support for COD money feature (VaultEcoBridge)
- **PermissionsEx** - Permission group integration for special groups (PermissionsExBridge)

## Localization

Messages are in `src/main/resources/`:
- `messages_ja.yml` (Japanese)
- `messages_en.yml` (English)
- `messages_de.yml` (German)

The Messages class loads localized strings from these files.

## Key Patterns

- Mail data loading is **asynchronous** - always check `MailManager.isLoaded()` before operations
- MailSender uses string serialization with `$` prefix for UUIDs (e.g., `$uuid-string`)
- Interactive chat UI uses MessageComponent/MessageParts for clickable elements
- Special groups exist for broadcast scenarios: SpecialGroupAll, SpecialGroupAllConnected, SpecialGroupAllLogin, SpecialGroupPex
