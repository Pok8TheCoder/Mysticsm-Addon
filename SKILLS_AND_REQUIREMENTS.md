# Mysticism Ultimates — Skills & Acquisition Guide

Comprehensive guide to all **Unique** and **Ultimate** skills implemented in the **Mysticism Ultimates** addon (`mysticism_ultimates`), detailing their abilities, modes, and complete evolution/acquisition requirements.

---

## 1. Global Evolution Mechanics & Gamerules

To evolve and acquire Ultimate skills through the natural progression system, the player must meet the core world criteria:

| Requirement | Details |
| :--- | :--- |
| **Awakening Status** | The player must have awakened as either a **True Demon Lord** (`isTrueDemonLord`) or a **True Hero** (`isTrueHero`). |
| **EP Cost** | The player must possess sufficient **Existence Value (EP)**. When evolution succeeds, the required EP is deducted. |
| **Skill Mastery** | All prerequisite Unique, Extra, and Common skills must be at **100% Mastery**. |
| **Soul Quality** | If `mysticismUltimatesSoulQuality` is enabled (default: `true`), player's rolled soul capacity (0–7) limits active Ultimates. |
| **Unique Consumption** | If `mysticismUltimatesLoseUnique` is enabled (default: `false`), the prerequisite Unique skills are consumed/forgotten upon evolving into the Ultimate form. |

### Relevant Gamerules
- `mysticismUltimatesAcquisition` (Default: `true`): Toggles automatic Ultimate evolution checks on player tick.
- `mysticismUltimatesLoseUnique` (Default: `false`): When enabled (`true`), consumes and forgets prerequisite Unique skills upon evolution.
- `mysticismUltimatesSoulQuality` (Default: `true`): Restricts the total number of Ultimates a player can acquire by their rolled soul capacity (0 to 7).
- `mysticismUltimatesPlundererDarknessSpirit` (Default: `true`): Permits contracting Darkness spirits while possessing Light spirits if Plunderer is owned.

---

## 2. Unique & Custom Skills

### 🌑 Unique Skill: Plunderer (`mysticism_ultimates:plunderer`)
*The nascent shadow ability focused on spatial storage and spiritual affinity.*
- **Skill Type**: `UNIQUE`
- **Acquisition Cost**: `75,000 Magicules` (Learned via skill tree or Tensura menu)
- **Max Mastery**: `1,000`
- **Inherent Passive**: Allows contracting a **Darkness Spirit** even if the wielder already commands a **Light Spirit** (ordinarily prohibited in Tensura).
- **Modes**:
  1. **Shadow Warrior** (`plunderer.shadow_warrior`): Melee shadow-empowered strikes.
  2. **Shadow Storage** (`plunderer.shadow_storage`): Opens a compact 1-slot spatial pocket.
  3. **Shadow Merge** (`plunderer.shadow_merge`): Blend directly into shadows to evade detection.

---

### 👑 Unique Skill: Vainglory (`mysticism_ultimates:vainglory`)
*The advanced shadow skill that masters the extraction and retention of shadow souls.*
- **Skill Type**: `UNIQUE`
- **Acquisition Cost**: `100,000 Magicules`
- **Max Mastery**: `1,500`
- **Acquisition Type**: **Evolution Only** (*Not auto-acquirable on character creation, skill reroll, or dwarf drops*)
- **Acquisition Requirements**:
  1. Mastered **Unique Skill: Plunderer** (`mysticism_ultimates:plunderer`, 100% Mastery)
  2. Mastered **Extra Skill: Darkness Manipulation** (`tensura:darkness_manipulation` / Mysticism, 100% Mastery)
- **Modes**:
  1. **Shadow Warrior** (`vainglory.shadow_warrior`): Advanced physical/spiritual shadow strikes.
  2. **Shadow Storage** (`vainglory.shadow_storage`): Full 54-slot spatial inventory.
  3. **Shadow Merge** (`vainglory.shadow_merge`): Seamless shadow traversing.
  4. **Shadow Pit** (`vainglory.shadow_pit` — *Unlocked at 1,500 Mastery*): Extracts souls from fallen foes to recruit them into the Shadow Army.
  5. **Army Summon** (`vainglory.army_summon` — *Unlocked at 1,500 Mastery*): Deploys stored shadow subordinates into battle.

> [!IMPORTANT]
> **Evolution-Only Status**:
> Even though Vainglory is a Unique Skill, it is strictly an evolution of **Plunderer**. It is excluded from the starting skills pool, second skills pool, and random mob/dwarf plunder pools. It only unlocks when Plunderer and Darkness Manipulation are fully mastered.

---

### 🌀 Unique Skill: Otherworldly (`mysticism_ultimates:otherworldly`)
*Transcends dimensional boundaries to bond entities, manipulate positioning, and align cosmic serendipity.*
- **Skill Type**: `UNIQUE`
- **Acquisition Cost**: `100,000 Magicules`
- **Max Mastery**: `1,500`
- **Acquisition Type**: Acquirable on character creation, skill reroll, or via Skill Menu.
- **Passive Effect (When Toggled)**: Constant **Luck V** and **Hero of the Village V** refreshed every 5 seconds. Usable immediately as-is.
- **Modes**:
  1. **Shared Secrets** (`otherworldly.shared_secrets`): Strips entities of boss, mini-boss, and plunder immunities, making them completely vulnerable, nameable, and tamable.
     - **Dimension Requirement**: To cast Shared Secrets, the player **MUST have visited all 5 realms**:
       1. Overworld (`minecraft:overworld`)
       2. Nether (`minecraft:the_nether`)
       3. The End (`minecraft:the_end`)
       4. Hell (`tensura:hell`)
       5. Labyrinth (`tensura:labyrinth`)
     - If any realm has not been visited, the ability remains sealed, alerts the player to missing dimensions, and applies a 10s cooldown rather than the 24-minute cooldown.
  2. **Closed Loop** (`otherworldly.closed_loop`): Binds a temporary skill into a permanent learned skill (requires mastery, 24m cooldown).
  3. **Beyond Odds** (`otherworldly.beyond_odds` — *Active Ability & Toggle*):
     - **Learning System**: Requires **100 Learn Points** to master. Each activation attempt grants **+5 Learn Points** with a standard **10-second cooldown** between attempts (20 attempts total). Once mastered (100 points), toggling has **zero cooldown**!
     - **Active Toggle**: Bestows the custom `mysticism_ultimates:gold_luck` effect named **Beyond Odds** with an 18×18 golden-shaded luck icon.
     - **Universal Serendipity & Luck Engine**:
       - **Virtual Fortune V**: Applied to all fortune-affected block breaks and ores, granting +1 to +5 extra items (stacks with tool's Fortune enchant).
       - **Foliage Boost**: Breaking leaves grants a 5× to 10× higher drop rate for natural saplings and apples (does **not** alter the vanilla loot pool; no golden apples or golden nuggets).
       - **Virtual Looting V**: Applied to all mob kills, granting +1 to +5 bonus yield per drop (stacks with weapon's Looting enchant).
       - **Golden Evasion**: 25% flat chance to completely negate incoming damage with a 5-second internal cooldown, burst of golden particles, and resonant amethyst chime.
       - **Plunder & Skill Steal Odds Boost**: Boosts Usurper, Great Sage, and Predator/Gluttony copy/devour success chances by a flat **+25%** (capped at 100%).
       - **Dwarf & Villager Trade Maximizer & Anti-Upcharge (`OLucked` Tag)**:
         - Merchants born, employed, or interacted with near Beyond Odds receive the `"OLucked"` tag.
         - Initial trades roll the lowest buy cost (1 item), highest sell yield, 32 max uses, and 0 demand.
         - **Strict Anti-Upcharge Enforcement**: Tagged dwarfs can **never upcharge** the player under any circumstance (higher sales volume, demand spikes, and negative reputation penalties can never raise prices above base). However, **low charging (discounts, Hero of the Village, and special discounts) remains completely active and functional**.
       - **Chest Loot "Slot Overflow" & Same-Odds Drop-Hit System**:
         - Triggers upon opening an unlooted structure chest (fully compatible with Lootr instanced containers).
         - Normal chest loot unpacks first (no common loot lost).
         - Evaluates empty slots $E$: candidate overflow slots $N = \lfloor E / 2 \rfloor$ if $E > 10$, else $N = E$.
         - Displays a 2-second alignment state with locked `✦ Otherworldly: Aligning Fates..... ✦` placeholders to prevent dupe/pickup exploits.
         - Scans loot table items with baseline drop rate $< 25\%$, boosting drop chance by **+15%**.
         - **Same-Odds Drop-Hit Tier Method**: If multiple items share the exact same drop probability (e.g. Diamond at 2.5% and Emerald at 2.5%, or up to 10 items with equal odds), they are grouped into an equal-probability tier. Each hit rolls an RNG pick from the remaining pool and removes it. The next hit in that tier is guaranteed to drop the remaining item(s). When the tier is exhausted, it refills and repeats, ensuring perfectly fair, non-starving distribution across all slots.
         - Resolves with celebratory chimes, golden particles, and actionbar notice `✦ Otherworldly: Fates Aligned! ✦`.

---

### ☀️ Extra Skill: True Daylight (`mysticism_ultimates:true_daylight`)
*Projects pure midday sunlight that burns the wicked and empowers solar magic.*
- **Skill Type**: `EXTRA`
- **Acquisition Cost**: `35,000 Magicules`
- **Max Mastery**: `500`
- **Mutually Exclusive**: Cannot be activated while True Nightlight is active.
- **Effects**:
  - Emits a moving **4-chunk radius** of midday sun (light level 15).
  - Ignites undead and forces hostile nocturnal mobs to burn or flee.
  - Villagers and diurnal creatures stay alert and productive.

---

### 🌙 Extra Skill: True Nightlight (`mysticism_ultimates:true_nightlight`)
*Projects an absolute midnight field that protects nocturnal creatures and empowers dark arts.*
- **Skill Type**: `EXTRA`
- **Acquisition Cost**: `35,000 Magicules`
- **Max Mastery**: `500`
- **Mutually Exclusive**: Cannot be activated while True Daylight is active.
- **Effects**:
  - Emits a moving **4-chunk radius** of midnight darkness (light level 0).
  - Shields nocturnal mobs and undead from burning under daylight.
  - Allows the player to use a bed to sleep at any time of day.
  - Satisfies nighttime conditions for nocturnal skills (**Viciel**, **Laverna**, etc.) even during server midday.

---

## 3. Fully Implemented Ultimate Skills

The following **8 Ultimate Skills** are fully ported, functional, and equipped with dedicated active modes, animations, and sound effects:

```
                                  [ Awakening: True Demon Lord / True Hero ]
                                                      |
    +-----------------+---------------+---------------+---------------+-----------------+
    |                 |               |               |               |                 |
[ Antaeus ]     [ Beelzebub ]   [ Gilgamesh ]     [ Hades ]       [ Laverna ]      [ Tsukuyomi ]
 600,000 EP       500,000 EP      750,000 EP     600,000 EP      1,000,000 EP       300,000 EP
    |                 |               |               |               |                 |
    +-----------------+---------------+---------------+---------------+-----------------+
                                      |                               |
                                  [ Uriel ]                       [ Viciel ]
                                 1,000,000 EP                     666,666 EP
```

> [!IMPORTANT]
> **Universal Awakening Requirement**:
> **ANY and ALL Ultimate Skills** strictly require the player to be awakened as a **True Demon Lord** (`existence.isTrueDemonLord()`) or a **True Hero** (`existence.isTrueHero()`).
> Even if all prerequisite skills are 100% mastered and EP thresholds are met, unawakened players cannot acquire, learn, or evolve into any Ultimate Skill.

---

### 1. 🌍 Antaeus (`mysticism_ultimates:antaeus`)
*Titan of the Earth — Overwhelming gravitational authority and spatial compression.*
- **EP Requirement**: `600,000 EP`
- **Evolution Prerequisites** (All must be 100% Mastered):
  - **Unique**: Oppressor (`tensura:oppressor`)
  - **Extra**: Gravity Domination (`tensura:gravity_domination`)
  - **Common**: Gravity Field (`tensura:gravity_field`)
  - **Common**: Ranged Barrier (`tensura:ranged_barrier`)
- **Sacrificed on Evolution**: `Oppressor`
- **Modes**:
  1. `oppress`: Crushes surrounding entities under extreme gravitational pressure.
  2. `attract_blue`: Gravitational convergence drawing all entities toward a focal point.
  3. `repel_red`: Expulsive repulsive wave pushing threats away.
  4. `hollow_purple`: Fusion collision beam obliterating matter in its path.
  5. `bleve`: Boiling liquid expanding vapor explosion.
  6. `flicker`: Micro-spatial teleportation dashes.

---

### 👑 2. Beelzebub (`mysticism_ultimates:beelzebub`)
*Lord of Gluttony — Devours everything into an infinite stomach, isolating and decomposing all matter.*
- **EP Requirement**: `500,000 EP`
- **Evolution Prerequisites** (All must be 100% Mastered):
  - **Unique**: Gourmet (`tensura:gourmet`)
  - **Unique**: Guardian (`tensura:guardian`)
  - **Extra**: Multilayer Barrier (`tensura:multilayer_barrier`)
  - **Common**: Corrosion (`tensura:corrosion`)
- **Sacrificed on Evolution**: `Gourmet`, `Guardian`
- **Modes**:
  1. `predation`: Devours targets and absorbs their energy/stats.
  2. `stomach`: Infinite spatial inventory with preservation.
  3. `provide`: Supplies stored energy, buffs, or skills to allies.
  4. `receive`: Extracts skills and energy from subordinates.
  5. `isolation`: Traps hostile projectiles, entities, or harmful effects in a sub-dimension.
  6. `corrosion`: Decomposes target defenses and spiritual structures.
  7. `substitution`: Swaps positions or transfers damage to clones.

---

### ⚔️ 3. Gilgamesh (`mysticism_ultimates:gilgamesh`)
*King of Heroes — Commands the boundless treasury of ancient noble armaments.*
- **EP Requirement**: `750,000 EP`
- **Evolution Prerequisites** (Must be 100% Mastered):
  - **Unique**: Gatekeeper (`mysticism:gatekeeper`)
- **Sacrificed on Evolution**: `Gatekeeper`
- **Modes**:
  1. `territory`: Expands an imposing armory domain.
  2. `ea`: Manifests the Sword of Rupture (Ea).
  3. `axe`: Calls down Marduk's Cleaving Axe.
  4. `enuma_elish`: Unleashes the primordial heaven-and-earth sundering vortex.

---

### 💀 4. Hades (`mysticism_ultimates:hades`)
*Visage of Death — Absolute ruler of the underworld, reaper clones, and soul reaping.*
- **EP Requirement**: `600,000 EP`
- **Evolution Prerequisites** (All must be 100% Mastered):
  - **Unique**: Reaper (`tensura:reaper`)
  - **Unique**: Merciless (`tensura:merciless`)
  - **Common**: Poison (`tensura:poison`)
  - **Common**: Corrosion (`tensura:corrosion`)
  - **Common**: Paralysis (`tensura:paralysis`)
- **Sacrificed on Evolution**: `Reaper`, `Merciless`
- **Modes**:
  1. **Recon Mode** (`recon_mode`): Toggles 100% melee/projectile dodge and hyper-sensory perception.
  2. **Attack Mode** (`attack_mode`): Summons a squad of up to 10 combat clones. Clones scale with owner stats and despawn safely without magicule overflow.
  3. **Infinite Eater** (`infinite_eater`): Direct execution strike bypassing barriers, resistances, and invulnerability frames.
     - *First Devour*: Grants **50% of the target's total EP** on the first kill of each species (or player).
     - *Subsequent Kills*: Yields the standard Tensura EP calculation formula (`EnergyHelper.getEPGain`).
  4. **Fear of Insanity** (`fear_of_insanity`): Channeling aura inducing Fear, Weakness, and Insanity.
  5. **Merciless** (`merciless`): Consumes the soul of weakened or panicked non-boss entities within range.
     - *Target Exclusions*: Cannot harm entities with `boss`, `mini_boss`, `named`, or `otherwordly` tags.
     - *Cost*: Costs **50% of the target's EP in magicules** to execute each target.

---

### 🌌 5. Laverna (`mysticism_ultimates:laverna`)
*Shadow Monarch — Total mastery over darkness, shadow expansion, and the eternal army.*
- **EP Requirement**: `1,000,000 EP`
- **Evolution Prerequisites**:
  - **Unique**: Vainglory (`mysticism_ultimates:vainglory`, 100% Mastered)
  - **Extra**: Darkness Domination (`mysticism:darkness_domination`, 100% Mastered)
  - **Spiritual Contract**: Must command a **Greater Darkness Spirit** or higher.
- **Sacrificed on Evolution**: `Vainglory`
- **Modes**:
  1. `blitz`: Extreme high-speed shadow assault.
  2. `shadow_storage`: Expansive dark spatial vault.
  3. `shadow_merge`: Dissolve into shadows with full intangibility.
  4. `euclidean_dystopia`: Distorts space to trap foes in an inescapable shadow maze.
  5. `shadow_garden`: Spreads an overwhelming shadow territory that empowers the army.
  6. `shadow_pit`: Extracts and preserves the essence of fallen entities.
  7. `army_summon`: Calls forth the legions of the shadow monarchs.

---

### 🌕 6. Tsukuyomi (`mysticism_ultimates:tsukuyomi`)
*God of the Moon — Mastery over illusion, lunar distortion, and parallel existence.*
- **EP Requirement**: `300,000 EP`
- **Evolution Prerequisites** (All must be 100% Mastered):
  - **Unique**: Shadow Striker (`tensura:shadow_striker`)
  - **Extra**: Body Double (`tensura:body_double`)
- **Sacrificed on Evolution**: `Shadow Striker`
- **Modes**:
  1. `eye_of_the_moon`: Lunar mental domain that incapacitates targets.
  2. `insta_kill`: Swift, clean execution strike from the blind spot.
  3. `lightspeed_acceleration`: Ultra-accelerated movement and reaction speed.
  4. `parallel_existence`: Creates autonomous parallel projections sharing consciousness.

---

### 🛡️ 7. Uriel (`mysticism_ultimates:uriel`)
*Lord of Vows — The ultimate spatial shield and master of universal natural laws.*
- **EP Requirement**: `1,000,000 EP`
- **Evolution Prerequisites** (Must be 100% Mastered):
  - **Unique**: Infinity Prison (`tensura:infinity_prison`)
- **Sacrificed on Evolution**: `Infinity Prison`
- **Built-in Passives**:
  - **Absolute Guard**: Nullifies incoming spiritual attacks by converting 80% of damage into magicule cost. Grants absolute immunity to spiritual damage as long as magicules remain.
  - **Energy Drain Immunity**: Completely prevents energy or EP drain from attackers (unless the attacker wields Susanoo or possesses $\ge 8\times$ the wielder's EP).
- **Modes** (9 Dedicated Modes):
  1. **Imprison** (`infinity_prison`): Seals targets inside an impenetrable spatial cage.
  2. **Imaginary Space** (`imaginary_space`): Accesses an infinite pocket dimension for storage and containment.
  3. **Molecular Manipulation** (`law_manipulation`): Controls elemental heat, cold, and kinetic laws.
  4. **Gravity Manipulation** (`gravity_manipulation`): Adjusts gravitational vectors and weights.
  5. **Universal Barrier** (`universal_barrier`): High-tier multilayer barrier encompassing an area.
  6. **Absolute Severance** (`absolute_severance`): Launches spatial severance blades bypassing barriers and defenses.
  7. **Fatal Imprison** (`fatal_imprison`): Compresses a spatial prison inward, crushing trapped victims.
  8. **Absolute Guard** (`absolute_guard`): Interactive manual barrier reinforcing absolute defense.
  9. **Spatial Domination** (`spatial_domination`): Instant spatial folding and instant teleportation.

---

### 🎭 8. Viciel (`mysticism_ultimates:viciel`)
*Lord of the Depths — Master of fate, identity theft, and skill appropriation.*
- **EP Requirement**: `666,666 EP`
- **Evolution Prerequisites** (Must be 100% Mastered):
  - **Unique**: Hidden Ruler (`mysticism:hidden_ruler`)
- **Sacrificed on Evolution**: `Hidden Ruler`
- **Modes**:
  1. `true_copy`: Copies appearance, attributes, and capabilities of targeted beings.
  2. `true_paste`: Assumes copied identity and abilities.
  3. `darkness_conscription`: Binds foes in dark chains and subdues their will.
  4. `petrichor_stained_destiny`: Alters probability and rewrites fateful lethal blows.
  5. `shadow_jump`: Instant leaps through darkness and shadows.

---

## 4. Quick Evolution Reference Table

| Ultimate Skill | Prerequisite Unique(s) | Prerequisite Extra & Common Skills | Special Conditions | EP Cost |
| :--- | :--- | :--- | :--- | :--- |
| **Antaeus** | Oppressor | Gravity Domination, Gravity Field, Ranged Barrier | True Demon Lord / Hero | `600,000 EP` |
| **Beelzebub** | Gourmet, Guardian | Multilayer Barrier, Corrosion | True Demon Lord / Hero | `500,000 EP` |
| **Gilgamesh** | Gatekeeper | — | True Demon Lord / Hero | `750,000 EP` |
| **Hades** | Reaper, Merciless | Poison, Corrosion, Paralysis | True Demon Lord / Hero | `600,000 EP` |
| **Laverna** | Vainglory | Darkness Domination | Greater Darkness Spirit | `1,000,000 EP` |
| **Tsukuyomi** | Shadow Striker | Body Double | True Demon Lord / Hero | `300,000 EP` |
| **Uriel** | Infinity Prison | — | True Demon Lord / Hero | `1,000,000 EP` |
| **Viciel** | Hidden Ruler | — | True Demon Lord / Hero | `666,666 EP` |

---

## 5. Temporarily Disabled Placeholders (Category 3)

The following skills are stubs awaiting accurate, faithful ports from the original 1.19.2 Mysticism mod, and remain cleanly disabled to preserve gameplay integrity:
*Amaterasu, Ame no Uzume, Antithesis, Asmodeus, Belphegor, Bushyasta, Demeter, Dionysus, Galileo, Ignis, Invictus, Lucifer, Mephisto, Michael, Oizys, Pernida, Sandalphon, Sariel, Satanael, Sephirot, Takemikazuchi, and Xezbeth.*
