#!/usr/bin/bash -xv

# usage:
#   cd $THRONES_HOME/pallbearer/src/main/scripts
#   sudo ./process-media.sh data/output.csv <YES|NO>
# --note output.csv is from create-media.sh; do not get rid of this!
# pass "YES" as cleanup/second argument if you want it to clear files first; this is destructive
# pass "NO" or nothing to process incrementally

cleanup_dir() {
    DIR=$1
    pushd "$DIR" || exit 0
    rm -f input.mp4 240.mp4 360.mp4 480.mp4 720.mp4 1080.mp4
    if [ -d output ]; then
        rm -rf output
    fi
    popd
}


process_file() {
    DIR=$1
    FILE=$2
    pushd "$DIR" || exit 0
      if [[ ! -f input.mp4 ]]; then
          /usr/local/bin/ffmpeg -nostdin -y -i "$FILE" -vcodec h264 -acodec mp3 input.mp4 >> ffmpeg.log 2>&1
      fi
      if [[ ! -f still-frame.jpg ]]; then
          /usr/local/bin/ffmpeg -nostdin -y -i input.mp4 -ss 00:00:02 -frames:v 1 -q:v 2 still-frame.jpg >> ffmpeg.log 2>&1
      fi
      if [[ ! -f 1080.mp4 ]]; then
          /usr/local/bin/ffmpeg -nostdin -y -i input.mp4 -c:v h264 -crf 22 -tune film -profile:v main -level:v 4.0 -maxrate 5000k -bufsize 10000k -r 25 -keyint_\
min 25 -g 50 -sc_threshold 0 -c:a aac -ar 44100 -b:a 128k -ac 2 -pix_fmt yuv420p -movflags +faststart 1080.mp4 -s 1280x720 -c:v h264 -crf 24 -tune film -\
profile:v main -level:v 4.0 -maxrate 2500k -bufsize 5000k -r 25 -keyint_min 25 -g 50 -sc_threshold 0 -c:a aac -ar 44100 -b:a 128k -ac 2 -pix_fmt yuv420p \
-movflags +faststart 720.mp4 -s 854x480 -c:v h264 -crf 30 -tune film -profile:v main -level:v 4.0 -maxrate 1250k -bufsize 2500k -r 25 -keyint_min 25 -g 5\
0 -sc_threshold 0 -c:a aac -ar 44100 -b:a 96k -ac 2 -pix_fmt yuv420p -movflags +faststart 480.mp4 -s 640x360 -c:v h264 -crf 33 -tune film -profile:v main\
 -level:v 4.0 -maxrate 900k -bufsize 1800k -r 25 -keyint_min 25 -g 50 -sc_threshold 0 -c:a aac -ar 44100 -b:a 96k -ac 2 -pix_fmt yuv420p -movflags +fasts\
tart 360.mp4 -s 320x240 -c:v h264 -crf 36 -tune film -profile:v main -level:v 4.0 -maxrate 625k -bufsize 1250k -r 25 -keyint_min 25 -g 50 -sc_threshold 0\
 -c:a aac -ar 22050 -b:a 64k -ac 1 -pix_fmt yuv420p -movflags +faststart 240.mp4 >> ffmpeg.log 2>&1
      fi
      if [[ ! -d output ]]; then
          /usr/local/bento4/bin/mp4hls 240.mp4 360.mp4 480.mp4 720.mp4 1080.mp4 --verbose --segment-duration 6 --output-single-file >> bento.log 2>&1
      fi
    popd
}

set -e

# account for spaces in file names
INPUT_FILE=$1
CLEANUP="${2:-NO}"
OIFS="$IFS"
IFS=$'\n'
HOME_DIR=`pwd`

echo "=== looping over files ==="
while IFS=";" read -r uuid file; do
    ffile=${file//\"/}
    DIR="$(dirname "${ffile}")"
    FILE="$(basename "${ffile}")"

    if [[ "${CLEANUP}" == "YES" ]]; then
        cleanup_dir $DIR
    fi
    process_file $DIR $FILE
done < $INPUT_FILE

# reset IFS to original value
IFS="$OIFS"

set +e