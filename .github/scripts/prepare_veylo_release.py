import hashlib
import re
import shutil
from pathlib import Path


def prepare(source: Path, destination: Path) -> list[Path]:
    expected = {"universal", "arm64-v8a", "armeabi-v7a", "x86", "x86_64"}
    found = {}
    for apk in source.glob("*.apk"):
        match = re.fullmatch(r"app-github-(.+)-release\.apk", apk.name)
        if match and match[1] in expected:
            found[match[1]] = apk
    missing = expected - found.keys()
    if missing:
        raise ValueError(f"Missing signed release APKs: {', '.join(sorted(missing))}")
    destination.mkdir(parents=True, exist_ok=False)
    outputs = []
    checksums = []
    for abi, apk in sorted(found.items()):
        name = "Veylo.apk" if abi == "universal" else f"veylo-{abi}.apk"
        target = destination / name
        shutil.copyfile(apk, target)
        digest = hashlib.sha256(target.read_bytes()).hexdigest()
        checksums.append(f"{digest}  {name}")
        outputs.append(target)
    (destination / "SHA256SUMS").write_text("\n".join(checksums) + "\n", encoding="utf-8")
    return outputs


if __name__ == "__main__":
    prepare(Path("app/build/outputs/apk/github/release"), Path("dist"))
