#!/bin/bash
#(c) jmonkeyengine.org
#Author MeFisto94
set -e # Quit on Error

jdk_version="8u74"
jdk_build_version="b02"
platforms=( "linux-x64.tar.gz" "linux-i586.tar.gz" "windows-i586.exe" "windows-x64.exe" "macosx-x64.dmg" )

function unpack_mac_jdk {
    echo "> Extracting the Mac JDK..."
    cd local/$jdk_version-$jdk_build_version/

    if [ -f ../../jdk-macosx.zip ];
    then
        echo "< Already existing, SKIPPING."
        cd ../../
        return 0
    fi

    mkdir -p MacOS
    cd MacOS

    # MacOS
    if [ "$(uname)" == "Darwin" ]; then
        hdiutil attach ../jdk-macosx-x64.dmg
        xar -xf /Volumes/JDK*/JDK*.pkg
        hdiutil detach /Volumes/JDK*
    else # Linux (NOT TESTED!)
        mkdir mnt
        mount -t hfsplus -o loop ../jdk-macosx-x64.dmg mnt
        xar -xf mnt/JDK*.pkg
        umount mnt
    fi

    cd jdk1*.pkg
    cat Payload | gunzip -dc | cpio -i
    mkdir -p Contents/jdk/
    cd Contents/Home
    # FROM HERE: build-osx-zip.sh by normen
    cp -r . ../jdk
    zip -9 -r -y ../../../../jdk-macosx.zip ../jdk
    cd ../../../../
    rm -rf MacOS/
    cd ../../
}

function unpack_windows {
    echo "> Extracting the JDK for $1"
    cd local/$jdk_version-$jdk_build_version/

    if [ -d $1 ];
    then
        echo "< Already existing, SKIPPING."
        cd ../../
    return 0
    fi

    mkdir -p $1
    7z x -o$1 "jdk-$1.exe"
    unzip $1/tools.zip -d $1/
    rm $1/tools.zip

    find $1 -type f \( -name "*.exe" -o -name "*.dll" \) -exec chmod u+rwx {} \; # Make them executable

    find $1 -type f -name "*.pack" | while read eachFile; do
        echo ">> Unpacking $eachFile ...";
        unpack200 $eachFile ${eachFile%.pack}.jar;
        rm $eachFile;
    done

    cd ../../
}

function unpack_linux {
    echo "> Extracting the JDK for $1"
    cd local/$jdk_version-$jdk_build_version/

    if [ -d $1 ];
    then
        echo "< Already existing, SKIPPING."
        cd ../../
        return 0
    fi

    mkdir -p $1
    cd $1
    tar -xvf "../jdk-$1.tar.gz"
    cd jdk1*
    mv * ../
    cd ../
    rm -rf jdk1*
    cd ../../../
}

function build_mac_jdk {
    if [ -f local/$jdk_version-$jdk_build_version/jdk-macosx.zip ];
    then
        rm -f jdk-macosx.zip
        mv -f local/$jdk_version-$jdk_build_version/jdk-macosx.zip .
    fi # Already packed
}

function exec_build_package {
    echo "> Building Package for $1"

    if [ -f "jdk-$1.$3" ]; then
        echo "< Already existing, SKIPPING."
    else
        sh build-package.sh $1 $2
    fi
}

mkdir -p local/$jdk_version-$jdk_build_version

for platform in ${platforms[@]}
do
    echo "> Downloading the JDK for $platform"

    if [ -f local/$jdk_version-$jdk_build_version/jdk-$platform ];
    then
        # rm -f local/$jdk_version-$jdk_build_version/jdk-$platform
        echo "< Already existing, SKIPPING."
    else
        curl -L --progress-bar -o local/$jdk_version-$jdk_build_version/jdk-$platform http://download.oracle.com/otn-pub/java/jdk/$jdk_version-$jdk_build_version/jdk-$jdk_version-$platform --cookie "gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie"
    fi
done

unpack_mac_jdk
build_mac_jdk
unpack_windows windows-i586
exec_build_package windows-x86 local/$jdk_version-$jdk_build_version/windows-i586/ exe
unpack_windows windows-x64
exec_build_package windows-x64 local/$jdk_version-$jdk_build_version/windows-x64/ exe
unpack_linux linux-i586
exec_build_package linux-x86 local/$jdk_version-$jdk_build_version/linux-i586/ bin
unpack_linux linux-x64
exec_build_package linux-x64 local/$jdk_version-$jdk_build_version/linux-x64/ bin