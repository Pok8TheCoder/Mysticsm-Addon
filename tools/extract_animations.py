import json
import zipfile
from pathlib import Path

src = zipfile.ZipFile(r"d:\Minecraft\Pok8Works\Mysticsm\trmysticism-1.1.1.jar")
out = Path(r"d:\Minecraft\mysticism-ultimates\src\main\resources\assets\mysticism_ultimates")
keep = (
    "assets/trmysticism/animations/",
    "assets/trmysticism/geo/",
    "assets/trmysticism/textures/entity/",
    "assets/trmysticism/textures/item/ea.png",
    "assets/trmysticism/textures/item/axe.png",
)
for name in src.namelist():
    if name.endswith("/") or not name.startswith(keep) and name not in keep:
        if not any(name.startswith(prefix) for prefix in keep if prefix.endswith("/")) and name not in keep:
            continue
    if name.endswith("/"):
        continue
    data = src.read(name)
    rel = name.split("assets/trmysticism/", 1)[1]
    target = out / rel
    target.parent.mkdir(parents=True, exist_ok=True)
    if name.endswith(".json"):
        text = data.decode("utf-8").replace("trmysticism:", "mysticism_ultimates:")
        target.write_text(text, encoding="utf-8", newline="\n")
    else:
        target.write_bytes(data)
    print(rel)
