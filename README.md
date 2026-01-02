# 🎉 ParadiseTeamChat v1.1.0 - NOW AVAILABLE! 🎉

> **High-Performance Team Communication for Paper 1.21.10**

---

## 🚀 What is ParadiseTeamChat?

A blazing-fast, feature-rich team communication plugin that brings **private team channels** to your Minecraft server - without the complexity of faction plugins!

**Perfect for:** Survival servers, SMP networks, community servers, minigame lobbies

---

## ✨ Key Features

### 🏃 **Performance-First Design**

- ⚡ **Zero server lag** - All database operations run asynchronously
- 💾 **SQLite included** - No external database setup required

### 💬 **Seamless Team Chat**

- 🔄 Toggle between **team** and **global** chat modes
- 📨 Send quick messages without changing modes
- 🏷️ Custom team tags displayed in chat: `[TAG] PlayerName: message` (Integrations w/ PlaceholderAPI and LPC)
- 🎨 Color-coded formatting for easy identification
- 👥 Only team members see team chat messages

### 🛡️ **Rock-Solid Security**

- 🔒 Input validation prevents malicious content
- 🚫 Banned word filtering (blocks staff impersonation)
- 🧹 Sanitizes color codes and invisible Unicode
- 💉 SQL injection protection built-in

### 🎮 **Player-Friendly Commands**

```
/team create <name> <tag>  - Create your team
/team invite <player>      - Invite friends
/team accept              - Join a team
/tc                       - Toggle team chat
/tc <message>             - Quick team message
/gc                       - Switch to global chat
```

### 🔧 **Fully Customizable**

- 📝 Configure max team size, name/tag lengths
- 🎯 Regex patterns for validation
- 💬 Customize all messages and colors
- ⚙️ Banned words list
- 🕒 Cache cleanup intervals

---

## 📊 Technical Highlights

```
✅ Paper 1.21.10 compatible
✅ Offline-mode server support
✅ Adventure API integration
✅ Thread-safe concurrent operations
✅ Async database layer
✅ Smart cache management
✅ Modern Java 21 codebase
```

---

## 🎯 Perfect For

- **Survival Servers** - Let players form teams without factions
- **SMP Networks** - Organize communities with private channels
- **Event Servers** - Temporary team coordination
- **Minigame Lobbies** - Quick team formation and chat

---

## 📥 Download & Installation

**GitHub:** [github.com/patelheet30/ParadiseTeamChat](https://github.com/patelheet30/ParadiseTeamChat)

### Quick Setup:

1. Download the `jar` via the GitHub releases page
2. Drop into your `plugins/` folder
3. Restart server
4. **That's it!** Zero configuration needed ✨

The plugin creates a default `config.yml` with sensible defaults. Customize if needed!

---

## 🎬 How It Works

**Example Workflow:**

```
Player1: /team create Legends LGD
         ✅ Team created!

Player1: /team invite Steve
         📧 Invite sent!

Steve:   /team accept
         🎉 Joined team Legends!

Player1: /tc
         🔄 Team chat mode enabled

Player1: Hey team, let's raid!
         [Shows as: [LGD] Player1: Hey team, let's raid!]
         [Only visible to team members]

Player1: /gc Hello server!
         [Shows as: <Player1> Hello server!]
         [Visible to everyone, still in team mode]
```

---

## 🏆 What Makes This Special?

Unlike other team plugins:

- **No lag** - Async operations prevent server slowdown
- **Lightning fast** - Cache system = instant lookups
- **Zero dependencies** - SQLite bundled, no setup required
- **Offline-mode friendly** - Works on cracked servers
- **Modern codebase** - Built with Paper's latest APIs
- **Production-ready** - Comprehensive error handling

---

## 🔮 Roadmap (Future Updates)

- [ ] Team roles (co-owner, moderator)
- [ ] Team alliances
- [ ] Public API for other plugins

---

## 💝 Support the Project

⭐ **Star on GitHub** if you find this useful!
🐛 **Report issues** on our GitHub Issues page
💡 **Suggest features** - we love feedback!
🤝 **Contribute** - PRs welcome!

---

## 👨‍💻 About the Developer

Built by **@patelheet30** - A data scientist trying out Java/Minecraft development

- First major Minecraft plugin project
- Focus on performance, security, and code quality
- Open to collaboration and learning!

---

## 📜 License

**GNU General Public License v3.0** - see `LICENSE` file for details.

---

**Download now and bring seamless team communication to your server!** 🎮✨
