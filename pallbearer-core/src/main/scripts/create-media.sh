#!/usr/bin/bash

# usage:
# sudo create-media.sh /media/thrones/pallbearer/media/gallery /home/ec2-user/drills/levels_shooting /home/ec2-user/drills/processed

set -e

# account for spaces in file names
OIFS="$IFS"
IFS=$'\n'

cp /dev/null output.csv

MEDIA_DIR=$1
ROOT_DIR=$2
PROC_DIR=$3

echo "=== looping over files ==="

# Find all files under ROOT_DIR
echo "UUID;FILENAME" >> output.csv
for file in $(find $ROOT_DIR -type f ); do
    UUID=`uuidgen`
    DIR="$MEDIA_DIR/$UUID"
    ffile="${file/$ROOT_DIR/$DIR}"
    mkdir -p $DIR
    cp "${file}" $DIR
    mv "${file}" $PROC_DIR
    echo "$UUID;\"${ffile}\"" >> output.csv
done

# reset IFS to original value
IFS="$OIFS"

set +e