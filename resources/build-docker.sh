#!/bin/bash

fullfile=$(find ../zookeeper-assembly/target -type f -name  "*.tar.gz");
echo "binary: \"$fullfile\""

DISTRO_PATH=$(dirname -- "$fullfile");
echo "DISTRO_PATH: \"$DISTRO_PATH\"";
filename=$(basename -- "$fullfile");
DISTRO_NAME="${filename%.*.*}";
echo "DISTRO_NAME: \"$DISTRO_NAME\"";

rm -rf target;
mkdir target;
tar -zxf "$fullfile" -C target --strip-components=1

distro_version=${DISTRO_NAME#"apache-zookeeper-"};
distro_version=${distro_version%-bin};
echo "version: \"$distro_version\"";

docker build \
    --build-arg DISTRO_NAME=$DISTRO_NAME \
    --build-arg DISTRO_PATH=$DISTRO_PATH \
    . \
    --tag "tn/zookeeper:$distro_version";

#rm -rf target;