#!/bin/bash

DEFAULT_PREF="$HOME/.config/chromium/Default/Preferences"
if [ -f "$DEFAULT_PREF" ]; then
    echo "Patching: $DEFAULT_PREF ..."
    # https://superuser.com/questions/461035/disable-google-chrome-session-restore-functionality
    sed -i 's/"exited_cleanly":false/"exited_cleanly":true/' "$DEFAULT_PREF"
    sed -i 's/"exit_type":"Crashed"/"exit_type":"None"/' "$DEFAULT_PREF"
fi

SNAP_PREF=$HOME/snap/chromium/common/chromium/Default/Preferences
if [ -f "$SNAP_PREF" ]; then
    echo "Patching: $SNAP_PREF ..."
    # https://superuser.com/questions/461035/disable-google-chrome-session-restore-functionality
    sed -i 's/"exited_cleanly":false/"exited_cleanly":true/' "$SNAP_PREF"
    sed -i 's/"exit_type":"Crashed"/"exit_type":"None"/' "$SNAP_PREF"
fi

# with the snap version of chromium the kiosk mode did not work
# instead of calling chromium-browser use flatpack run ...
flatpak run org.chromium.Chromium --noerrors  --noerrdialogs \
                 --disable-restore-session-state --disable-features=InfiniteSessionRestore \
                 --disable-session-crashed-bubble --disable-infobars --disable-translate --disable-sync \
                 --no-first-run --start-maximized --disable-features=BookmarksBar \
                 --autoplay-policy=no-user-gesture-required --enable-kiosk-mode --kiosk "$@" > chrome.out 2>&1
