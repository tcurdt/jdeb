#!/bin/bash
###############################################################################
#
# Update debian repository indexes.
#
# It actually makes maven repositories expose a debian index.
#
# It also create a "virtual" stable debian repository in which is selected only
# stable releases (filter milestonnes and release candidates).
#
# Requirements:
# * This script is based on dpkg-scanpackages. It is provided by dpkg-dev package.
# * The script expect to find (configurable)
# ** a "releases" folder containing a maven repository with the deployed
#    releases
# ** a "snapshots" folder containing a maven repository with the deployed
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

  dpkg-scanpackages -m $SNAPSHOTS_REP /dev/null > $SNAPSHOTS_REP/Packages.tmp && mv -f $SNAPSHOTS_REP/Packages.tmp $SNAPSHOTS_REP/Packages
  gzip -9c $SNAPSHOTS_REP/Packages > $SNAPSHOTS_REP/Packages.gz.tmp && mv -f $SNAPSHOTS_REP/Packages.gz.tmp $SNAPSHOTS_REP/Packages.gz

  rm -rf $SNAPSHOTS_REP/Release $SNAPSHOTS_REP/Release.gpg
  apt-ftparchive -c=$SNAPSHOTS_REP/Release.conf release $SNAPSHOTS_REP > $SNAPSHOTS_REP/Release
  gpg -abs --default-key 0398E391 -o $SNAPSHOTS_REP/Release.gpg $SNAPSHOTS_REP/Release
fi

## releases
if [ -d $RELEASES_REP ]; then
  echo "Generates releases index"

  dpkg-scanpackages -m $RELEASES_REP /dev/null > $RELEASES_REP/Packages.tmp && mv -f $RELEASES_REP/Packages.tmp $RELEASES_REP/Packages
  gzip -9c $RELEASES_REP/Packages > $RELEASES_REP/Packages.gz.tmp && mv -f $RELEASES_REP/Packages.gz.tmp $RELEASES_REP/Packages.gz

  rm -rf $RELEASES_REP/Release $RELEASES_REP/Release.gpg
  apt-ftparchive -c=$RELEASES_REP/Release.conf release $RELEASES_REP > $RELEASES_REP/Release
  gpg -abs --default-key 0398E391 -o $RELEASES_REP/Release.gpg $RELEASES_REP/Release
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

  dpkg-scanpackages -m $RELEASES_REP /dev/null > "$ROOT_REP/$STABLE_REP/Packages.tmp" && mv -f "$ROOT_REP/$STABLE_REP/Packages.tmp" "$ROOT_REP/$STABLE_REP/Packages"
  gzip -9c "$ROOT_REP/$STABLE_REP/Packages" > "$ROOT_REP/$STABLE_REP/Packages.gz.tmp" && mv -f "$ROOT_REP/$STABLE_REP/Packages.gz.tmp" "$ROOT_REP/$STABLE_REP/Packages.gz"

  cd "$ROOT_REP"

  rm -rf $STABLE_REP/Release $STABLE_REP/Release.gpg
  apt-ftparchive -c=$STABLE_REP/Release.conf release $STABLE_REP > $STABLE_REP/Release
  gpg -abs --default-key 0398E391 -o $STABLE_REP/Release.gpg $STABLE_REP/Release
fi
