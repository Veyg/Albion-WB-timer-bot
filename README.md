# Albion-WB-timer-bot

**License:** GNU General Public License (GPL)

---
[![Support Server](https://img.shields.io/badge/Join-Support%20Server-blue)](https://discord.gg/QqRC8vnaeZ)
[![Invite Bot](https://img.shields.io/badge/Invite-Bot%20to%20Server-green)](https://discord.com/api/oauth2/authorize?client_id=1145671676902785084&permissions=566935907456&scope=bot%20applications.commands)
![CI/CD Status](https://github.com/Veyg/Albion-WB-timer-bot/actions/workflows/AWS_CI-CD.yml/badge.svg)

## Introduction

Albion-WB-timer-bot is a Discord bot designed to help Albion Online players manage world boss spawn timers for different maps. With this bot, you can easily set, edit, and delete timers for world bosses, making it a valuable tool for organizing your in-game activities.

## Features

- Set designated channels for timers.
- Add timers for world bosses in various maps.
- Edit existing timers when needed.
- Delete timers that are no longer relevant.

## Usage

To use the Albion-WB-timer-bot, you can invite it to your Discord server and interact with it through Discord commands.

### Available Commands
1. `/help`: Display a list of available commands.

2. `/setdesignatedchannel`: Set the designated channel for timers.

3. `/addtimer`: Add a timer for a world boss.
   - Options:
     - `time`: Time for the boss spawn in HH:mm:ss format (required).
     - `map`: Name of the map where the boss spawns (required).

4. `/deletetimer`: Delete a timer for a world boss.
   - Options:
     - `map`: Name of the map where the boss spawns (required).

5. `/edittimer`: Edit a timer for a world boss.
   - Options:
     - `map`: Name of the map where the boss spawns (required).
     - `newtime`: New time for the boss spawn in HH:mm:ss format (required).
     - `newdate`: New date for the boss spawn in d/MM/yyyy format (required).

## Support the Project

If you find Albion-WB-timer-bot useful and would like to support its development, you can:

- [Buy Me a Coffee](https://www.buymeacoffee.com/Veyg): Show your appreciation by buying the author a coffee.

## Getting Started

To get started with the Albion-WB-timer-bot, follow these steps:

1. **Invite the Bot**: [Invite the bot to your Discord server](https://discord.com/api/oauth2/authorize?client_id=1145671676902785084&permissions=566935907456&scope=bot%20applications.commands).

2. **Set Up Designated Channels**: Use the `/setdesignatedchannel` command to specify the channels where the bot will send timer notifications.

3. **Add Timers**: Use the `/addtimer` command to create timers for world bosses in different maps.

4. **Manage Timers**: You can use the `/deletetimer` and `/edittimer` commands to manage your timers as needed.

## Contributing

Contributions to this project are welcome! If you have suggestions, bug reports, or would like to contribute code, please [open an issue](../../issues) or [submit a pull request](../../pulls).

## Support

For questions, issues, or support, you can contact the author, Veyg, or create a GitHub issue in this repository.

---

Thank you for using the Albion-WB-timer-bot!
