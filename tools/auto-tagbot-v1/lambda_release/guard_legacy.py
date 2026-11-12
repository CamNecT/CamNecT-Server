"""Install the 4084 ingress hold while production tag writes are disabled."""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile

SITE = Path("/etc/nginx/sites-available/api.camnect.site")
MARK = "    # 4084 crawler: candidates are staged before publication"
GUARD = MARK + """
    location ~ ^/api/internal/activity-imports/konkuk-job(?:/tagged)?/?$ {
        add_header X-Camnect-Import-Mode staging always;
        return 409;
    }
"""
ANCHOR = "    # backend proxy\n    location / {"


def install():
    current = SITE.read_text()
    if GUARD in current:
        return
    if MARK in current or current.count(ANCHOR) != 1 or "server_name api.camnect.site;" not in current:
        raise RuntimeError("Unexpected live Nginx configuration")
    stat = SITE.stat()
    with tempfile.TemporaryDirectory(dir=SITE.parent) as tmp:
        backup = Path(tmp) / "previous"
        shutil.copy2(SITE, backup)
        candidate = Path(tmp) / "candidate"
        candidate.write_text(current.replace(ANCHOR, GUARD + "\n" + ANCHOR))
        os.chown(candidate, stat.st_uid, stat.st_gid)
        os.chmod(candidate, stat.st_mode)
        os.replace(candidate, SITE)
        try:
            subprocess.run(["nginx", "-t"], check=True)
            subprocess.run(["systemctl", "reload", "nginx"], check=True)
        except BaseException:
            os.replace(backup, SITE)
            subprocess.run(["nginx", "-t"], check=True)
            subprocess.run(["systemctl", "reload", "nginx"], check=True)
            raise


if __name__ == "__main__":
    install()
