
#!/bin/bash

cd target

if [ "$#" -ne 2 ]; then
    echo "Usage: ./roseau.sh pathV1 pathV2"
    exit 1
fi

java -jar roseau-0.0.1-SNAPSHOT-jar-with-dependencies.jar "$1" "$2"

