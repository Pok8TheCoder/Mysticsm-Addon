# Mysticism Ultimates

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-brightgreen.svg)]()
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.259-orange.svg)]()
[![Tensura](https://img.shields.io/badge/Tensura-2.0.1.3-blue.svg)]()

**Mysticism Ultimates** (`mysticism_ultimates`) is an advanced gameplay and expansion addon for **Tensura Mod: Reincarnated** and **Mysticism** on **Minecraft 1.21.1 (NeoForge)**. It implements fully realized Ultimate skills, Unique abilities, spirit contracts, custom entities, spatial inventories, and balanced evolution pathways faithful to the original light novel lore and mod inspirations.

---

## 📖 Detailed Skill Acquisition & Evolution Guide

For in-depth breakdowns of each skill, mode mechanics, EP costs, evolution trees, mastery thresholds, and server gamerules, please refer to:
👉 **[SKILLS_AND_REQUIREMENTS.md](SKILLS_AND_REQUIREMENTS.md)**

---

## 🚀 Current Patch Notes (v0.1.0-Patch)

### 💀 1. Hades Complete Overhaul
- **Infinite Eater Execution & First Devour**:
  - Guaranteed lethal execution with resistance bypass (`10.0f`), barrier bypass (`10.0f`), dodge bypass, invulnerability cooldown reset (`invulnerableTime = 0`), and fallback lethal execution.
  - **First Devour Mechanic**: Devouring an entity species (or player via UUID) for the first time grants **50% of the target's total EP** (`targetEp * 0.5`), split evenly between permanent Max Magicule and Max Aura (`GainType.MAX` and `NORMAL`).
  - **Subsequent Devours**: Subsequent kills of already eaten species fall back to the standard Tensura EP calculation formula (`EnergyHelper.getEPGain`).
  - Automatically flags `setSkippingEPDrop(true)` on devoured targets to prevent duplicate EP orb drops.
- **Attack Mode & Magicule Overflow Fix**:
  - Implemented `ICloning` on `HadesSkill` for safe clone lifecycle management.
  - Fixed an unbounded magicule doubling bug by clamping restored Aura and Magicule to the player's attribute maximums (`EnergyHelper.getMaxAura` / `EnergyHelper.getMaxMagicule`).
  - Restricted clone death events to players only, preventing clone death from accidentally doubling player reserves.
- **Merciless Mode Refinements**:
  - Mode 4 is now cleanly displayed as **"Merciless"** across client action bars and messages.
  - Added strict immunity checks: entities with `boss`, `mini_boss`, `miniboss`, `bosses`, `mini_bosses`, `named` (or custom name), or `otherwordly` / `otherworldly` tags are completely immune to Merciless.
  - Channels lethal soul consumption costing **50% of each target's EP in magicules**.

### 🛡️ 2. Uriel Complete Faithfulness Port
- **Absolute Guard Passive**: Converts 80% of incoming spiritual damage to magicule cost, granting absolute spiritual damage immunity as long as magicule reserves remain.
- **Energy Drain Immunity**: Protects the wielder from energy/EP drain unless attacker wields Susanoo or possesses $\ge 8\times$ the wielder's EP.
- **9 Dedicated Modes**: Imprison (`infinity_prison`), Imaginary Space (`imaginary_space`), Molecular Manipulation (`law_manipulation`), Gravity Manipulation (`gravity_manipulation`), Universal Barrier (`universal_barrier`), Absolute Severance (`absolute_severance` spatial blades), Fatal Imprison (`fatal_imprison`), Absolute Guard (`absolute_guard`), and Spatial Domination (`spatial_domination`).

### 🎲 3. Character & Skill Reroll Integration
- **Starting & Second Skills Pools**:
  - Unique skills **Plunderer** (`mysticism_ultimates:plunderer`), **Vainglory** (`mysticism_ultimates:vainglory`), and **Otherworldly** (`mysticism_ultimates:otherworldly`) are dynamically injected into `ReincarnationMenu.CONFIG.Skills.startingSkills` and `secondSkills`.
  - Also written to `config/tensura/reincarnation_config.toml`.
  - Can be rolled naturally upon first-time character creation, Character Reset Scrolls, and Skill Reset Scrolls.

### ⛏️ 4. Plunderer Mod (TensuraPlunder) Compatibility
- **Dwarf Unique Skill Spawning**:
  - Registered under `#tensura:skills/unique_skills` as well as `#mysticism_ultimates:unique_skills` and `#mysticism_ultimates:unique`.
  - Verified 100% compatible with `tensuraplunder`'s dwarf random unique skill rolling mechanism (`SkillType.UNIQUE` detection).

### ☀️ 5. True Daylight & True Nightlight Synchronization
- **Mutual Exclusivity**: True Daylight and True Nightlight cannot be active concurrently; toggling one automatically deactivates or blocks the other.
- **Time Spoofing & Sleep Support**:
  - Emits moving 4-chunk radius fields of pure sunlight (time 6,000, light 15) or midnight darkness (time 18,000, light 0).
  - True Nightlight allows sleeping in beds at any time of day and satisfies nocturnal skill conditions for skills like **Viciel** and **Laverna**.

### 🌀 6. Otherworldly Skill Rule
- **Shared Secrets**: Tamed entities receive a persistent `Otherwordly` tag. Marked entities cannot be re-targeted; bosses and minibosses are immune.
- **Closed Loop**: Instant swap positions with bonded entity.

### 🧹 7. Category 3 Placeholder Skills Cleanup
- 22 unfinished placeholder stubs (*Amaterasu, Asmodeus, Belphegor, Lucifer, Michael, Sariel, Satanael, etc.*) have been cleanly disabled from registration and evolution checks until faithful implementations are completed.

---

## 🌟 Implemented Skills Summary

### Unique Skills
- **Plunderer** (`mysticism_ultimates:plunderer`): Shadow Warrior, 1-slot Shadow Storage, Shadow Merge, and Darkness Spirit affinity.
- **Vainglory** (`mysticism_ultimates:vainglory`): 54-slot Shadow Storage, Shadow Pit soul extraction, and Army Summon.
- **Otherworldly** (`mysticism_ultimates:otherworldly`): Luck V / Hero of the Village V passives, entity taming/binding, and spatial position swapping.

### Extra Skills
- **True Daylight** (`mysticism_ultimates:true_daylight`): 4-chunk solar field, burns undead.
- **True Nightlight** (`mysticism_ultimates:true_nightlight`): 4-chunk midnight field, enables bed sleep anytime, powers nocturnal magic.

### Active Ultimate Skills (8 Fully Implemented)
| Skill | Requirements | Signature Abilities |
| :--- | :--- | :--- |
| **Antaeus** | Oppressor + Gravity Domination + 600,000 EP | Oppress, Attract Blue, Repel Red, Hollow Purple, BLEVE, Flicker |
| **Beelzebub** | Gourmet + Guardian + Multilayer Barrier + 500,000 EP | Predation, Stomach, Provide, Receive, Isolation, Corrosion, Substitution |
| **Gilgamesh** | Gatekeeper + 750,000 EP | Territory, Ea, Axe, Enuma Elish |
| **Hades** | Reaper + Merciless + 600,000 EP | Recon Mode, Attack Mode (clones), Infinite Eater (50% first kill), Fear of Insanity, Merciless |
| **Laverna** | Vainglory + Darkness Domination + Greater Darkness Spirit + 1,000,000 EP | Blitz, Shadow Garden, Euclidean Dystopia, Shadow Pit & Army Summon |
| **Tsukuyomi** | Shadow Striker + Body Double + 300,000 EP | Eye of the Moon, Insta Kill, Lightspeed Acceleration, Parallel Existence |
| **Uriel** | Infinity Prison + 1,000,000 EP | Absolute Guard, Energy Drain Immunity, 9 Law & Space modes |
| **Viciel** | Hidden Ruler + 666,666 EP | True Copy, True Paste, Darkness Conscription, Petrichor Stained Destiny, Shadow Jump |

---

## ⚙️ Configuration & Gamerules

| Gamerule | Default | Description |
| :--- | :--- | :--- |
| `mysticismUltimatesAcquisition` | `true` | Automatic Ultimate skill evolution checks on player tick |
| `mysticismUltimatesLoseUnique` | `false` | Consumes and forgets prerequisite Unique skills upon evolution |
| `mysticismUltimatesSoulQuality` | `true` | Restricts max Ultimates by player's rolled soul capacity (0–7) |
| `mysticismUltimatesPlundererDarknessSpirit` | `true` | Allows contracting Darkness spirits alongside Light spirits with Plunderer |

---

## 🛠️ Building & Compilation

Compile the project and deploy the jar with:
```powershell
powershell -ExecutionPolicy Bypass -File tools/compile.ps1
```
The compiled mod artifact is generated at `build/mysticism-ultimates-0.1.0.jar` and automatically copied to the development mod folder.
