#!/usr/bin/env just --justfile

default:
    @just --list

clean:
    ./gradlew clean

appid := 'toys.compy'
ide_appid := appid + '.ide'
player_appid := appid + '.player'

# java package name

package := 'org.love2d.android'
timestamp := `date +"%Y_%m_%d-%H_%M"`
COMPYDIR := '../../compy/src'
ADB := 'adb '

package-legacy:
    rm -rf app/src/embed/assets/*
    7z a app/src/embed/assets/game.love {{COMPYDIR}}/* -xr'!.git/' -xr'!.vscode/' -xr'!host.lua'

package:
    rm -rf app/src/embed/assets/*
    rsync -aAXH --exclude={'.git/*','**/.vscode/*','**/.history/*'} {{COMPYDIR}}/  app/src/embed/assets/

package-bundled:
    rm -rf app/src/embed/assets/*
    rsync -aAXH {{COMPYDIR}}/../dist/bundled/ app/src/embed/assets/

build flavor dbg='debug':
    #!/usr/bin/env -S bash
    TASK=
    APKDIR=
    REL='TODO'
    F="{{ flavor }}"
    case ${F,,} in
      'ide')
        TASK=IDE
        APKDIR=./app/build/outputs/apk/embed"$TASK"/{{ dbg }}
        true ;;
      'player')
        TASK=Player
        APKDIR=./app/build/outputs/apk/embed"$TASK"/{{ dbg }}
        true ;;
      *)
        echo unknown flavor
        exit
        ;;
    esac
    ./gradlew assembleEmbed"$TASK"
    ls -lh "$APKDIR"/app-*.apk || ls -lhR ./app/build/outputs/apk/embed"$TASK"/{{dbg}}

build-all dbg='debug': package
    ./gradlew assembleDebug

watch-build flavor='all':
    ./gradlew assembleDebug --continuous

install flavor dbg='debug':
    #!/usr/bin/env -S bash
    FL=
    APK=
    GRANT=0
    REL='TODO'
    F="{{ flavor }}"
    case ${F,,} in
      'ide')
        FL=${F^^}
        APK=./app/build/outputs/apk/embed"$FL"/{{ dbg }}/app-embed-"$FL"-{{ dbg }}.apk
        GRANT=1
        true ;;
      'player')
        FL='Player'
        APK=./app/build/outputs/apk/embed"$FL"/{{ dbg }}/app-embed-${F}-{{ dbg }}.apk
        true ;;
      *)
        echo unknown flavor
        exit
        ;;
    esac
    {{ ADB }} install "$APK"
    [ $GRANT -gt 0 ] && {
      {{ ADB }} shell appops set --uid toys.compy.ide MANAGE_EXTERNAL_STORAGE allow
    } || true

start flavor:
    #!/usr/bin/env -S bash -e
    ACT=
    F="{{ flavor }}"
    case ${F,,} in
      'ide')
        ACT='CompyActivity'
        true ;;
      'player')
        ACT='ProjectSelector'
        true ;;
      *)
        echo unknown flavor
        exit
        ;;
    esac
    {{ ADB }} shell am start-activity -n {{ appid }}.${F}/{{ package }}.${ACT}

logtail:
    {{ADB}} logcat -T 10 | grep -E '(LOVE|SDL|GameActivity|Selector|Compy)'

dev-b flavor: (build flavor) (install flavor) (start flavor) logtail

dev flavor: (install flavor) (start flavor) logtail

startlog flavor: (start flavor) logtail

###################################

zip-apps:
    7z a /tmp/apk_{{timestamp}} app/build/outputs/apk/embed*/debug/*.apk
    ls -lh /tmp/apk_{{timestamp}}

pm-ls:
    @{{ADB}} shell pm list packages | grep '.' | cut -d ':' -f 2

pm-un pkgid:
    @{{ADB}} shell pm uninstall {{pkgid}}

pm-un-f flavor:
    #!/usr/bin/env -S bash
    F="{{flavor}}"
    {{ADB}} shell pm uninstall {{appid}}.${F,,}

pm-purge:
    for app in $(just pm-ls | grep compy) ; do just pm-un $app ; done

import? 'local.just'
