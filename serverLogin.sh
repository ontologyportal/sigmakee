#!/bin/bash

URL="http://userid.ern.nps.edu/"
CHECK_INTERVAL=5  # seconds

echo "Opening $URL in browser..."
xdg-open "$URL" >/dev/null 2>&1 &

echo "Monitoring for redirect..."
INITIAL_URL=$(curl -Ls -o /dev/null -w %{url_effective} "$URL")

while true; do
    CURRENT_URL=$(curl -Ls -o /dev/null -w %{url_effective} "$URL")

    if [ "$CURRENT_URL" != "$INITIAL_URL" ]; then
        echo "URL changed to: $CURRENT_URL"
        echo "Running SSH command..."
        ssh srose@172.20.44.148
        break
    fi

    sleep $CHECK_INTERVAL
done

