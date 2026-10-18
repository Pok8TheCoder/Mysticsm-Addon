import json
import zipfile

old = json.loads(zipfile.ZipFile(r"d:\Minecraft\Pok8Works\Mysticsm\trmysticism-1.1.1.jar").read("assets/trmysticism/lang/en_us.json").decode("utf-8"))

# our id -> old lang skill id
ids = {
    "amaterasu": "amaterasu",
    "ame_no_uzume": "ame_no_uzume_no_mikoto",
    "antaeus": "antaeus",
    "antithesis": "antithesis",
    "asmodeus": "asmodeus",
    "beelzebub": "beelzebub",
    "belphegor": "belphegor",
    "bushyasta": "bushyasta",
    "dionysus": "dionysus",
    "galileo": "galileo",
    "gilgamesh": "gilgamesh",
    "hades": "hades",
    "ignis": "ignis",
    "invictus": "invictus",
    "laverna": "laverna",
    "lucifer": "lucifer",
    "mephisto": "mephisto",
    "michael": "michael",
    "oizys": "oizys",
    "pernida": "pernida",
    "sandalphon": "sandalphon",
    "sariel": "sariel",
    "satanael": "satanael",
    "sephirot": "sephirot",
    "takemikazuchi": "takemikazuchi",
    "tsukuyomi": "tsukuyomi",
    "uriel": "uriel",
    "viciel": "viciel",
    "xezbeth": "xezbeth",
    "demeter": "demeter",
}

modes = {
    "amaterasu": ["haze_black_flame", "movement_communication", "prominence_acceleration", "targeting_communication", "will_control"],
    "ame_no_uzume": ["performance", "encore", "spotlight", "ornate_star"],
    "antaeus": ["oppress", "attract_blue", "repel_red", "hollow_purple", "bleve", "flicker"],
    "antithesis": ["redirect", "malice_exchange", "vital_shift", "absolute_antithesis"],
    "asmodeus": ["drain", "invigorate", "death_blessing", "memory_end_requiem", "angel_of_death"],
    "beelzebub": ["predation", "stomach", "provide", "receive", "isolation", "corrosion", "substitution"],
    "belphegor": ["rest", "deprive", "fallen_hypno", "fallen_strike", "fallen_thanatos", "fallen_catastrophe"],
    "bushyasta": ["inaction", "stagnate", "stillness_of_ones_heart", "stasis_coat", "time_manipulation"],
    "dionysus": ["destroyer_haki", "dimension_hopper", "turn_null"],
    "galileo": ["enhanced_danger_detection", "enhanced_treasure_detection", "eye_of_the_allfather", "multidimensional_barrier"],
    "gilgamesh": ["territory", "ea", "axe", "enuma_elish"],
    "hades": ["attack_mode", "recon_mode", "fear_of_insanity", "infinite_eater"],
    "ignis": ["disassemble", "stone", "fuse", "explosion_dash", "bites_the_dust"],
    "invictus": ["conquest", "relentless_echo", "glorious_verdict", "kaisers_dominion", "invincible_victory"],
    "laverna": ["blitz", "shadow_merge", "shadow_garden", "shadow_storage", "euclidean_dystopia"],
    "lucifer": ["akasha", "arroganz"],
    "mephisto": ["checkpoint", "dream_end", "fantasy_world", "life_domination"],
    "michael": ["true_echo_counter", "true_echo_reflection", "regalia_dominion", "ultimate_dominion", "armageddon"],
    "oizys": ["gust", "baneful_requiem", "turbulent_force", "circus_show"],
    "pernida": ["adaptive_replication"],
    "sandalphon": ["assassination", "dispel", "eraser", "judgement", "necrosis", "remove"],
    "sariel": ["backup", "fortitude", "return", "sacred_haki"],
    "satanael": ["breeder", "enrage", "stampede"],
    "sephirot": ["checkpoint", "dream_eater", "hypnosis", "life_domination", "restore_life"],
    "takemikazuchi": ["channel"],
    "tsukuyomi": ["eye_of_the_moon", "insta_kill", "lightspeed_acceleration", "parallel_existence"],
    "uriel": ["absolute_severance", "fatal_imprison", "gravity_manipulation", "imaginary_space", "infinity_prison", "insulated_prison", "law_manipulation", "nova_break", "universal_barrier"],
    "viciel": ["true_copy", "true_paste", "darkness_conscription", "petrichor_stained_destiny", "shadow_jump"],
    "xezbeth": ["deception", "disguise", "display"],
    "demeter": ["unfinished"],
}

overrides = {
    ("antithesis", "redirect"): "trmysticism.skill.mode.inverse.redirect",
    ("antithesis", "malice_exchange"): "trmysticism.skill.mode.inverse.malice_exchange",
    ("michael", "regalia_dominion"): "trmysticism.skill.mode.michael.regalia.dominion",
    ("michael", "ultimate_dominion"): "trmysticism.skill.mode.michael.ultimate.dominion",
}

fallback_modes = {
    ("takemikazuchi", "channel"): "Channel",
    ("michael", "armageddon"): "Armageddon",
    ("demeter", "unfinished"): "Unfinished",
    ("asmodeus", "angel_of_death"): "Angel of Death",
}

blockers = {
    "amaterasu": "Needs subordinate communication and the black-flame effect, which Mysticism 2.2.1 does not register.",
    "ame_no_uzume": "Needs the idol-mode client packet and the spotlight effect.",
    "antaeus": "Needs the limitless barrier entity, attraction projectiles, and its menu.",
    "antithesis": "Inverse, the predecessor unique, is not in Mysticism 2.2.1. Redirect also needs the old spatial exchange.",
    "asmodeus": "The 1.1.1 evolution map has no predecessor. Revive and drain need subordinate packets.",
    "beelzebub": "Needs the stomach menu and predation storage. Fox Ablaze Ultimate already has its own Beelzebub.",
    "belphegor": "Needs stored-magicule packets and the fallen effects.",
    "bushyasta": "Needs InactionHandler and the multilayer barrier.",
    "dionysus": "Needs dimension hopping and Turn Null chunk removal.",
    "galileo": "Needs the detection toggles and a petrify projectile.",
    "gilgamesh": "Needs the territory entity, the gate entity, and weapon storage.",
    "hades": "Needs the clone entity and the stacking death debuffs.",
    "ignis": "Needs Bites the Dust rewind, the fuse mining path, and bomb entities.",
    "invictus": "Needs the verdict packets and the Kaiser Dominion heal effect.",
    "laverna": "Vainglory, the predecessor unique, is not in Mysticism 2.2.1. Shadow Garden needs a subordinate entity.",
    "lucifer": "The 1.1.1 evolution map has no predecessor. Akasha needs its menu and screen.",
    "mephisto": "Evolves from this addon's Sephirot. Fantasy World, the dimension it pulls players into, is not in Mysticism 2.2.1.",
    "michael": "The 1.1.1 evolution map has no predecessor. Echo reflection needs packets and shadow subordinates.",
    "oizys": "Needs the gust projectile and the grab.",
    "pernida": "Compulsor, the predecessor unique, is not in Mysticism 2.2.1. Only Adaptive Replication had a translation.",
    "sandalphon": "Needs the assassination projectile and the necrosis effect.",
    "sariel": "Needs the backup entity and a return dimension.",
    "satanael": "Needs the wrath packets. Fox Ablaze Ultimate already has its own Satanael.",
    "sephirot": "Needs the lingering-soul entity. Its dream world was Fantasy World, which 2.2.1 does not have.",
    "takemikazuchi": "Needs the lightspeed dash and teleport packet. Mysticism 1.1.1 shipped no mode translations for this skill.",
    "tsukuyomi": "Needs the parallel-existence entity and the mind-control packet.",
    "uriel": "Needs the prison block entity and the barrier. Fox Ablaze Ultimate already has its own Uriel.",
    "viciel": "Needs the copy menu, mind-control packets, and a replacement for Fantasy World.",
    "xezbeth": "Needs the disguise screen and the deception entity.",
    "demeter": "The 1.1.1 class is an empty stub. It has no modes and no evolution predecessor.",
}

out = {
    "mysticism_ultimates.skill.not_implemented": "%s / %s is registered only. %s",
    "mysticism_ultimates.skill.ultimate_upgrade": "Unique skill lost. Ultimate gained: %s",
    "mysticism_ultimates.command.soul_quality.get": "%s soul quality %s, ultimates used %s",
    "mysticism_ultimates.command.soul_quality.set": "Set %s soul quality to %s",
}

missing = []
for our, old_id in ids.items():
    name = old.get(f"trmysticism.skill.{old_id}", our)
    desc = old.get(f"trmysticism.skill.{old_id}.description", "")
    note = " First pass: the mode list is registered. The 1.1.1 effects are not ported yet."
    out[f"mysticism_ultimates.skill.{our}"] = name
    out[f"mysticism_ultimates.skill.{our}.description"] = (desc + note).strip()
    out[f"mysticism_ultimates.blocker.{our}"] = blockers[our]
    for mode in modes[our]:
        key = overrides.get((our, mode), f"trmysticism.skill.mode.{old_id}.{mode}")
        label = old.get(key) or fallback_modes.get((our, mode))
        if not label:
            missing.append(key)
            label = mode
        out[f"mysticism_ultimates.skill.mode.{our}.{mode}"] = label

path = r"d:\Minecraft\mysticism-ultimates\src\main\resources\assets\mysticism_ultimates\lang\en_us.json"
with open(path, "w", encoding="utf-8", newline="\n") as f:
    json.dump(out, f, ensure_ascii=False, indent=2)
    f.write("\n")
print("keys", len(out))
print("missing", missing)
