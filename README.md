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
  - Unique skills **Plunderer** (`mysticism_ultimates:plunderer`) and **Otherworldly** (`mysticism_ultimates:otherworldly`) are dynamically injected into `ReincarnationMenu.CONFIG.Skills.startingSkills` and `secondSkills`.
  - Also written to `config/tensura/reincarnation_config.toml`.
  - Can be rolled naturally upon first-time character creation, Character Reset Scrolls, and Skill Reset Scrolls.
  - **Vainglory** is explicitly **excluded** from starting and second skills because it is strictly an evolution of Plunderer.

### ⛏️ 4. Plunderer Mod (TensuraPlunder) Compatibility
- **Dwarf Unique Skill Spawning**:
  - Registered under `#tensura:skills/unique_skills` as well as `#mysticism_ultimates:unique_skills` and `#mysticism_ultimates:unique`.
  - **Vainglory** is automatically blacklisted in `TensuraPlunderConfig.spawnSkillBlacklist` (in both `tensuraplunder.json` and via runtime injection) so dwarfs can never spawn with Vainglory directly.
  - Base uniques **Plunderer** and **Otherworldly** can spawn naturally on eligible mobs (dwarfs, villagers, lizardmen).

### ☀️ 5. True Daylight & True Nightlight Synchronization
- **Mutual Exclusivity**: True Daylight and True Nightlight cannot be active concurrently; toggling one automatically deactivates or blocks the other.
- **Time Spoofing & Sleep Support**:
  - Emits moving 4-chunk radius fields of pure sunlight (time 6,000, light 15) or midnight darkness (time 18,000, light 0).
  - True Nightlight allows sleeping in beds at any time of day and satisfies nocturnal skill conditions for skills like **Viciel** and **Laverna**.

### 🌀 6. Otherworldly Skill (Dimensions & Passive Rules)
- **Acquisition**: Acquirable as a base Unique Skill through character creation or skill rerolls.
- **Passive Effect**: Toggle bestows Luck V and Hero of the Village V; usable immediately as-is.
- **Shared Secrets Dimension Prerequisite**: To cast **Shared Secrets**, the player **MUST have visited all 5 realms**:
  1. Overworld (`minecraft:overworld`)
  2. Nether (`minecraft:the_nether`)
  3. The End (`minecraft:the_end`)
  4. Hell (`tensura:hell`)
  5. Labyrinth (`tensura:labyrinth`)
  *If unvisited realms remain, the ability is canceled with a 10s cooldown and displays the missing dimensions.*
- **Closed Loop**: Binds a temporary skill into a permanent learned skill (requires mastery, 24m cooldown).
- **Mode 3: Beyond Odds (`otherworldly.beyond_odds`)**:
  - **Learning Mechanic**: Requires **100 Learn Points** (20 attempts total, +5 points per attempt, 10s cooldown). Once learned, toggling has **zero cooldown**.
  - **Effect**: Grants custom `mysticism_ultimates:gold_luck` named **Beyond Odds** with an 18×18 golden-shaded Luck icon.
  - **Virtual Fortune V**: +1 to +5 bonus items on ores and fortune-sensitive block breaks (stacks with weapon's Fortune).
  - **Foliage Drop Boost**: 5× to 10× higher drop rate for natural saplings and apples (no golden apples or nuggets).
  - **Virtual Looting V**: +1 to +5 bonus yield per mob drop (stacks with weapon's Looting).
  - **Golden Evasion**: 25% flat chance to completely negate incoming damage (5s internal cooldown, golden particles, amethyst chime).
  - **Plunder / Skill Steal Boost**: Flat **+25%** success rate boost when using Usurper, Great Sage, or Predator/Gluttony.
  - **Dwarf & Villager Job / Trade Optimization & Anti-Upcharge (`OLucked` Tag)**: Merchants marked with `OLucked` roll lowest buy cost (1), highest sell yield, 32 max uses, and 0 demand. Tagged dwarfs can **never upcharge** (demand spikes and negative reputation penalties are completely blocked from raising prices), while **low charging (discounts) remains fully active**.
  - **Chest Loot Slot Overflow & Same-Odds Drop-Hit System**:
    - Triggers on unlooted structure chests (fully compatible with Lootr).
    - Normal loot unpacks first (no common loot lost).
    - Candidate overflow slots $N = \lfloor E / 1.5 \rfloor$ (rounded down).
    - 2-second alignment timer with `✦ Otherworldly: Aligning Fates..... ✦` placeholders.
    - Scans loot table items with baseline drop rate $< 25\%$ (+15% boosted probability).
    - **Same-Odds Drop-Hit Tier Method**: If multiple items share the exact same drop probability (e.g. Diamond at 2.5% and Emerald at 2.5%, or up to 10 items), they are grouped into an equal-probability tier. Each hit rolls an RNG pick from the remaining pool and removes it. The next hit in that tier is guaranteed to drop the remaining item(s). When all items in the tier have dropped, the pool refills and resets RNG.

### 👑 7. Universal Awakening Requirement for All Ultimates
- **True Demon Lord or True Hero**: **ANY and ALL Ultimate Skills** strictly require the player to be awakened as a **True Demon Lord** (`existence.isTrueDemonLord()`) or a **True Hero** (`existence.isTrueHero()`).
- Unawakened players cannot learn or evolve into any Ultimate Skill via the Skill Menu or evolution ticks.

### 🧹 8. Category 3 Placeholder Skills Cleanup
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
