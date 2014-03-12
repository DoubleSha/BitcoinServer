#!/bin/bash

find . -name "*.proto" | xargs protoc -Isrc/main/proto --java_out=src/main/java/
