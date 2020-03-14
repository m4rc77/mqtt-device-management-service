#!/bin/bash

# https://superuser.com/questions/461035/disable-google-chrome-session-restore-functionality
sed -i 's/"exited_cleanly":false/"exited_cleanly":true/' ~/.config/chromium/Default/Preferences
sed -i 's/"exit_type":"Crashed"/"exit_type":"None"/' ~/.config/chromium/Default/Preferences

chromium-browser --noerrors --disable-restore-session-state --disable-features=InfiniteSessionRestore --disable-session-crashed-bubble --disable-infobars --disable-translate --disable-sync --autoplay-policy=no-user-gesture-required --enable-kiosk-mode --kiosk "$@" > chrome.out 2>&1
