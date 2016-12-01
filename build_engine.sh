#!/bin/sh
# This Shell Script will build and use the latest jMonkeyEngine git version, so there might be some undiscovered engine bugs, watch out!
# Also if you want to revert to releases and bintray builds, you need to uninstall them from your local maven repo...
echo "Downloading the Engine, this may take some time"
git clone -b v3.1 --single-branch --depth 1 http://github.com/jMonkeyEngine/jMonkeyEngine/ engine # single-branch requires git > 1.7.10, if you see an error, just leave it out.
git checkout tag/v3.1.0-beta2

cd engine
#echo "Patching the Engine...."
#patch -s -N -p 1 < ../patches/FixHWSkinningSerialization.diff

echo "Building the Engine and installing them to your local maven repo...."
./gradlew install # Depends on jarJavadoc, jarSourcecode, assemble, dist etc.

cd ../