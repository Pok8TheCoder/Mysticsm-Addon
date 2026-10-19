# Changelog - Mysticism Ultimates

All notable changes to the **Mysticism Ultimates** addon are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0] - 2026-10-19

### 🚀 Initial Release & 1.21.1 Port
- **Legacy 1.19.2 Port**: Successfully ported and modernized the iconic "Difficult" Ultimate Skills from Mysticism 1.1.1 to Minecraft 1.21.1 NeoForge on top of Mysticism 2.2.1.
- **8 Fully Implemented Ultimate Skills**:
  - **Antaeus**: Gravitational mastery (*Oppress, Attract Blue, Repel Red, Hollow Purple, BLEVE, Flicker*).
  - **Beelzebub**: Gluttonous authority (*Predation, Stomach, Provide, Receive, Isolation, Corrosion, Substitution*).
  - **Gilgamesh**: Vault of kings (*Territory, Sword of Rupture Ea, Enuma Elish*).
  - **Hades**: Monarch of the underworld (*Infinite Eater, Attack & Recon Clones, Fear of Insanity, Merciless*).
  - **Laverna**: Phantom shadow empress (*Blitz, Shadow Garden, Euclidean Dystopia, Shadow Pit & Army Summon*).
  - **Tsukuyomi**: Lunar divinity (*Eye of the Moon, Insta Kill, Lightspeed Acceleration, Parallel Existence*).
  - **Uriel**: Lord of Vows (*Absolute Guard, Energy Drain Immunity, 9 Law & Spatial modes*).
  - **Viciel**: Sovereign of mimicry (*True Copy, True Paste, Darkness Conscription, Petrichor Stained Destiny*).
- **Unique & Extra Skills**:
  - **Plunderer**: Shadow storage (1-slot), Shadow Merge, Shadow Warrior summoning, Darkness Spirit affinity.
  - **Vainglory**: 54-slot spatial inventory, Shadow Pit soul extraction, and Army Summon.
  - **True Daylight & True Nightlight**: Mutually exclusive 4-chunk sunlight and midnight aura fields supporting bed sleeping anytime.

---

### ✨ Major Features & Custom Additions

#### 🌀 Otherworldly Unique Skill & Beyond Odds
- **3 Dedicated Modes**:
  - **Mode 1 - Shared Secrets**: Strips any target entity of boss protection, mind control immunity, and plunder resistance, making them vulnerable, nameable, and tamable. Requires having visited all 5 realms (*Overworld, Nether, End, Hell, Labyrinth*).
  - **Mode 2 - Closed Loop**: Binds temporary skills permanently into the player's repertoire (mastery required, 24m cooldown).
  - **Mode 3 - Beyond Odds**: Learned through meditation (100 Learn Points across 20 attempts with 10s cooldown; zero cooldown toggle once mastered).
- **Beyond Odds Mechanics**:
  - **Virtual Fortune V & Looting V**: Adds +1 to +5 bonus yield per ore and mob drop, stacking with weapon enchantments.
  - **Foliage Drop Boost**: 5× to 10× higher sapling and natural apple drop rates from leaves.
  - **Golden Evasion**: 25% flat chance to completely negate incoming damage with custom chime and particles (5s internal cooldown).
  - **Skill Plunder Boost**: Flat **+25% success rate** when using *Usurper*, *Great Sage*, or *Predator/Gluttony*.
  - **Smart Chest Overflow**: Converts unopened chest empty slots into bonus rare loot rolls ($\lceil E / 1.5 \rceil$ candidate slots) with a 2-second alignment animation.
  - **Same-Odds Drop-Hit System**: Equal-probability rare loot items (e.g., Diamond & Emerald at 2.5%) are placed in an equal-odds tier that guarantees each drops in turn without RNG starvation.
  - **Merchant Anti-Upcharge & Fair Trade**: Tagged Dwarfs and Villagers can never spike prices above base from demand or negative reputation. Low-charge discounts are capped to at most 2 below base to prevent 1-coin economic collapses.
  - **Artifacts Integration**: Bridges the *Artifacts* mod's Villager Hat (`artifacts:villager_reputation`) directly into Tensura Dwarf reputation and trade discounts.
  - **Authentic Visual Badge**: Custom four-leaf clover artwork framed by Mysticism’s authentic 244-pixel gold boundary from *Cryogenic Cessation* over a warm obsidian backdrop (`#120f0a`).

#### 💀 Hades Complete Overhaul
- **First Devour Mechanic**: Devouring a mob species or player for the first time grants **50% of their total EP** permanently split between Max Magicule and Max Aura.
- **Infinite Eater Execution**: Guaranteed execution with complete resistance and barrier bypass (`10.0f`), invulnerability cooldown reset (`invulnerableTime = 0`), and fallback lethal execution.
- **Magicule Duplication Exploit Fix**: Clamped clone magicule restoration to natural attribute maximums to eliminate unbounded doubling bugs.
- **Merciless Mode Refinement**: Channels lethal soul consumption costing 50% of the target's EP in magicules, with strict immunities protecting named bosses.

#### 🛡️ Uriel 1.21.1 Faithfulness Overhaul
- **Absolute Guard Passive**: Converts 80% of incoming spiritual damage to magicule cost, granting spiritual damage immunity while magicules remain.
- **Energy Drain Immunity Passive**: Defends against energy/EP drain unless the attacker wields *Susanoo* or has $\ge 8\times$ the wielder's EP.
- **9 Dedicated Modes**: *Infinity Prison, Imaginary Space, Law Manipulation, Gravity Manipulation, Universal Barrier, Absolute Severance, Fatal Imprison, Absolute Guard,* and *Spatial Domination*.

---

### ⚖️ Balancing & Progression

- **Universal Awakening Prerequisite**: **All Ultimate Skills** now strictly require the player to be awakened as a **True Demon Lord** (`isTrueDemonLord`) or a **True Hero** (`isTrueHero`).
- **Character Creation & Reroll Integration**:
  - Dynamically injected **Plunderer** and **Otherworldly** into `ReincarnationMenu` starting and secondary skill pools.
  - **Vainglory** is explicitly blacklisted from starting pools, preserving it as an evolution-only reward.
- **TensuraPlunder Mod Spawning**:
  - Base uniques **Plunderer** and **Otherworldly** can spawn naturally on eligible mobs (dwarfs, villagers, lizardmen).
  - **Vainglory** is automatically blacklisted in `TensuraPlunderConfig.spawnSkillBlacklist` via runtime injection.
- **Cleaned Placeholder Stubs**: Cleanly disabled 22 unfinished placeholder stubs (*Amaterasu, Asmodeus, Belphegor, Lucifer, Michael, Sariel, Satanael, etc.*) until faithful mechanics are implemented.

---

### 🐛 Bug & Crash Fixes

- **Network Protocol Error Fix**: Resolved client entity join crashes caused by custom S2C packet mismatches and packet size handling.
- **Mod Loading Crash Fix**: Fixed premature `ReincarnationMenu` classloading in constructors that crashed game startup.
- **Chest Alignment Trigger Fix**: Corrected chest opening detection so the 2-second alignment timer only triggers on unlooted structure chests (fully compatible with Lootr).
- **Dwarf Upcharge / Discount Fix**: Prevented demand spikes from creating exorbitant trade costs while ensuring discount math respects base cost minimums.
