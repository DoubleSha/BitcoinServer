#!/bin/bash

BASEDIR=$(dirname $0)
cp -R $BASEDIR/../build/generated/java/* $BASEDIR/../src/main/java/
