#!/bin/sh
set -eu
release_path=$(pwd -P)
case "$release_path" in /opt/camnect-auto-tagbot/releases/*) ;; *) exit 2 ;; esac
test -f "$release_path/server_worker.py"
getent group camnect-tagbot >/dev/null || groupadd --system camnect-tagbot
id camnect-tagbot >/dev/null 2>&1 || useradd --system --gid camnect-tagbot --no-create-home --home-dir /nonexistent --shell /usr/sbin/nologin camnect-tagbot
install -d -m 2750 -o root -g camnect-tagbot /var/lib/camnect-tag-input
cd /opt/camnect-auto-tagbot
ln -sfn "$release_path" next
mv -Tf next current
for unit in camnect-auto-tagbot.service camnect-tag-imports.service camnect-auto-tagbot.timer; do
    install -m 0644 "$release_path/server/$unit" "/etc/systemd/system/$unit"
done
systemctl daemon-reload
systemctl enable --now camnect-auto-tagbot.timer
systemctl start camnect-tag-imports.service
