#!/usr/bin/env bash
#
set -euo pipefail

new_name="${NEW_NAME:-}"
user_name="${USER_NAME:-}"
user_email="${USER_EMAIL:-}"
dry_run=${DRY_RUN:-0}

if [ -z "$new_name" ]; then
    read -p "What is the new name: " new_name
fi
new_name_caps="$(echo $new_name | tr 'a-z' 'A-Z')"

echo "Renaming 'webapp-stub' -> '$new_name' …"

echo "Rename directories…"
for d in $(find modules -type d -name "webappstub"); do
    target="$(dirname "$d")/$new_name"
    echo "mv $d -> $target"
    if [ $dry_run -eq 0 ]; then
        mv "$d" "$target"
    fi
done

echo "Rename files…"
for f in $(find modules -type f -iname "*webappstub*"); do
    name="$(basename "$f")"
    target_name=$(echo "$name" | sed "s/webappstub/$new_name/g")
    target_name=$(echo "$target_name" | sed "s/Webappstub/${new_name^}/g")
    target_name=$(echo "$target_name" | sed "s/WEBAPPSTUB/$new_name_caps/g")
    target="$(dirname "$f")/$target_name"
    if [ "$f" != "$target" ]; then
        echo "Move $f -> $target"
        mv "$f" "$target"
    fi
done

echo "Fix source files…"
for f in $(find modules -type f -name "*.scala"); do
    echo "Fix $f"
    if [ $dry_run -eq 0 ]; then
        sed -i -e "s/webappstub/$new_name/g" "$f"
        sed -i -e "s/Webappstub/${new_name^}/g" "$f"
        sed -i -e "s/WEBAPPSTUB/$new_name_caps/g" "$f"
    fi
done

echo "Fix build.sbt"
if [ $dry_run -eq 0 ]; then
    sed -i -e "s/webappstub/$new_name/g" build.sbt
fi

echo "Fix scalafix.conf"
if [ $dry_run -eq 0 ]; then
    sed -i -e "s/webappstub/$new_name/g" .scalafix.conf
fi

echo "Fix flake.nix"
cnt_name="${new_name:0:5}dev"
vm_name="${new_name}vm"
if [ $dry_run -eq 0 ]; then
    sed -i -e "s/wasdev/$cnt_name/g" flake.nix
    sed -i -e "s/wasvm/$vm_name/g" flake.nix
    sed -i -e "s/webappstub/$new_name/g" flake.nix
    sed -i -e "s/Webappstub/${new_name^}/g" flake.nix
    sed -i -e "s/WEBAPPSTUB/$new_name_caps/g" flake.nix
fi

read -p "Re-initialize git? (y/n) " reinit_git
if [ "$reinit_git" == "y" ]; then
    if [ -z "$user_name" ]; then
        read -p "Your git name: " user_name
    fi
    if [ -z "$user_email" ]; then
        read -p "Your git email: " user_email
    fi
    echo "Setup git for $user_name <$user_email>"

    rm -rf .git/
    git init
    git branch -M main
    git config user.name "$user_name"
    git config user.email "$user_email"
    git add .
    git rm --cached init.sh
    git commit -am 'hello world'
fi
