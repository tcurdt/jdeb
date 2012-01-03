#!/bin/bash
###############################################################################
#
# Update debian repository indexes
#
# It actually makes maven repositories expose a debian index.
#
# It also create a "virtual" stable debian repository in which is selected only
# stable releases (filter milestonnes and release candidates).
#
# Requirements:
# * This script is based on scna_packages. It is provided by dpkg-dev.
# * The script expect to find
# ** a "releases" folder containing a maven repository with the deployed
#    releases
# ** a "snapshots" dolder containing a maven repository with the deployed
#    snaphots
# ** make sure the "stable" folder exists if you want a filtered stable debian
#    repository
#
# Setup:
# You need to set the $ROOT_REP variable to where your maven repositories are
# located.
#
###############################################################################

ROOT_REP=/home/maven/public_html
SNAPSHOTS_REP="snapshots"
RELEASES_REP="releases"
STABLE_REP="stable"

cd "$ROOT_REP"

## snapshots
if [ -d $SNAPSHOTS_REP ]; then
  echo "Generates snapshots index"

  dpkg-scanpackages -m $SNAPSHOTS_REP /dev/null | gzip -9c > $SNAPSHOTS_REP/Packages.gz.tmp && mv -f $SNAPSHOTS_REP/Packages.gz.tmp $SNAPSHOTS_REP/Packages.gz
fi

## releases
if [ -d $RELEASES_REP ]; then
  echo "Generates releases index"

  dpkg-scanpackages -m $RELEASES_REP /dev/null | gzip -9c > $RELEASES_REP/Packages.gz.tmp && mv -f $RELEASES_REP/Packages.gz.tmp $RELEASES_REP/Packages.gz
fi

## stable
if [ -d stable ]; then
  echo "Generates stable index"

  rm -rf /tmp/stable_scanpackages
  mkdir -p /tmp/stable_scanpackages/$RELEASES_REP

  function link_package ()
  {
    basepath=`dirname $1`
    basepath=${basepath##$ROOT_REP/}
    basepath=${basepath%*/}
    mkdir -p $basepath
    fullpath=`readlink -f $1`
    ln -sf $fullpath "/tmp/stable_scanpackages/$basepath"
  }

  cd /tmp/stable_scanpackages/

  for i in $(find "$ROOT_REP/$RELEASES_REP" -name "*.[0-9][0-9].deb" ) ; do
    link_package $i
  done

  for i in $(find "$ROOT_REP/$RELEASES_REP" -name "*.[0-9].deb" ) ; do
    link_package $i
  done

  dpkg-scanpackages -m $RELEASES_REP /dev/null | gzip -9c > "$ROOT_REP/$STABLE_REP/Packages.gz.tmp" && mv -f "$ROOT_REP/$STABLE_REP/Packages.gz.tmp" "$ROOT_REP/$STABLE_REP/Packages.gz"
fi