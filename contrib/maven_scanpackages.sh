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

cd "$ROOT_REP"

## snapshots
if [ -d snapshots ]; then
  echo "Generates snapshots index"

  dpkg-scanpackages snapshots /dev/null | gzip -9c > snapshots/Packages.gz.tmp && mv -f snapshots/Packages.gz.tmp snapshots/Packages.gz
fi

## releases
if [ -d releases ]; then
  echo "Generates releases index"

  dpkg-scanpackages releases /dev/null | gzip -9c > releases/Packages.gz.tmp && mv -f releases/Packages.gz.tmp releases/Packages.gz
fi

## stable
if [ -d stable ]; then
  echo "Generates stable index"

  rm -rf /tmp/maven_scanpackages
  mkdir -p /tmp/maven_scanpackages/releases

  function link_package ()
  {
    basepath=`dirname $0`
    cd result && mkdir -p $basepath && cd ..
    fullpath=`readlink -f $0`
    ln -sf $fullpath "/tmp/maven_scanpackages/$basepath"
  }

  for i in $(find "$ROOT_REP/releases" -name "*.[0-9][0-9].deb" ) ; do
    link_package $i
  done

  for i in $(find "$ROOT_REP/releases" -name "*.[0-9].deb" ) ; do
    link_package $i
  done

  cd /tmp/xwiki_scanpackages/
  dpkg-scanpackages releases /dev/null | gzip -9c > "$ROOT_REP/stable/Packages.gz.tmp" && mv -f "$ROOT_REP/stable/Packages.gz.tmp" "$ROOT_REP/stable/Packages.gz"
fi
