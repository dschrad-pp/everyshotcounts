#!/usr/bin/bash -xv

# usage:
#   cd $THRONES_MEDIA/gallery
#   sudo ./fix-media.sh

find . -type f -name -print0 master.m3u8 | xargs -0 perl -pi -e 's/^.*?\i\-frame.*?$//gi'