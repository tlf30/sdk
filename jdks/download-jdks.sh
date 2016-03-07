#!/bin/bash
#(c) jmonkeyengine.org
#Author MeFisto94
set -e # Quit on Error

jdk_version="8u74"
jdk_build_version="b02"
platforms=( "linux-x64.tar.gz" "linux-i586.tar.gz" "windows-i586.exe" "windows-x64.exe" "macosx-x64.dmg" )

function install_xar {
    # This is needed to open Mac OS .pkg files on Linux... NEED: apt-get install xml2-dev
    wget -q https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/xar/xar-1.5.2.tar.gz
    tar xf xar-1.5.2.tar.gz
    cd xar-1.5.2
    ./configure -q
    make -s
    cd ../
}

function install_seven_zip {
    # This is due to not having root privilegs for apt-get
    if [ -x "$(command -v 7z)" ]; then
        return 0
    fi

    echo "> Installing 7zip"

    if [ -x "7zip/bin/7z" ]; then
        echo ">> Found cached 7zip, adjusting path"
        cd 7zip/bin
        PATH=`pwd`:$PATH
        cd ../../
        return 0
    fi

    echo ">> Compiling 7zip from source"
    mkdir -p 7zip/bin
    mkdir -p 7zip/lib
    cd 7zip
    wget -q http://downloads.sourceforge.net/project/p7zip/p7zip/15.09/p7zip_15.09_src_all.tar.bz2
    tar xf p7zip*
    rm *.bz2
    cd p7zip*
    make all3 > /dev/null
    ./install.sh ../bin ../lib /dev/null /dev/null
    #mv -v bin/ ../
    cd ../
    rm -rf p7zip*
    cd bin
    PATH=`pwd`:$PATH
    cd ../lib
    PATH=`pwd`:$PATH
    cd ../../
}

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
    else # Linux
        #mkdir mnt
        7z x ../jdk-macosx-x64.dmg > /dev/null
        7z x 4.hfs > /dev/null
        #sudo mount -t hfsplus -o loop 4.hfs mnt
        install_xar
        ./xar-1.5.2/src/xar -xf JDK*/JDK*.pkg
        #sudo umount mnt
    fi

    cd jdk1*.pkg
    cat Payload | gunzip -dc | cpio -i
    mkdir -p Contents/jdk/
    cd Contents/Home
    # FROM HERE: build-osx-zip.sh by normen
    cp -r . ../jdk
    zip -9 -r -y -q ../../../../jdk-macosx.zip ../jdk
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
    7z x -o$1 "jdk-$1.exe" > /dev/null
    unzip -qq $1/tools.zip -d $1/
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
    tar -xf "../jdk-$1.tar.gz"
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
    name="jdk-$1.$3"

    if [ -f "$name" ]; then
        echo "< Already existing, SKIPPING."
    else
        # ./build-package.sh $1 $2 # We do it manually now
        unzipsfxname="unzipsfx/unzipsfx-$1"
        if [ ! -f "$unzipsfxname" ]; then
            echo "No unzipsfx for platform $1 found at $unzipsfxname, cannot continue"
            exit 1
        fi


        echo ">> Creating SFX JDK package $name for $1 with source $2."

        if [ -f "$2jre/lib/rt.jar" ]; then # Already packed?
            pack200 -J-Xmx1024m $2jre/lib/rt.jar.pack.gz $2jre/lib/rt.jar
            rm -rf $2jre/lib/rt.jar
        fi

        echo ">>> Zipping JDK"
        zip -9 -qry jdk_tmp_sfx.zip $2
        echo ">>> Building SFX"
        cat $unzipsfxname jdk_tmp_sfx.zip > $name
        chmod +x $name
        rm -rf jdk_tmp_sfx.zip
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
        curl -L  -s -o local/$jdk_version-$jdk_build_version/jdk-$platform http://download.oracle.com/otn-pub/java/jdk/$jdk_version-$jdk_build_version/jdk-$jdk_version-$platform --cookie "gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" #--progress-bar
    fi
done

#cd local/$jdk_version-$jdk_build_version
#install_seven_zip # see travis' apt addon
#cd ../../
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