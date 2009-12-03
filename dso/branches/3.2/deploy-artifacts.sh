#!/bin/bash

root=`dirname $0`
cd $root/code/base

./tcbuild clean deploy_artifacts api_dir=/shares/monkeyoutput/api maven.repo=file:///shares/maven2 maven.repositoryId=kong maven.snapshot=true 

