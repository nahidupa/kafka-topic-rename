#!/bin/bash
docker buildx build .  --platform "linux/amd64"  -t nahidupa/kaf:0.0.3
docker push nahidupa/kaf:0.0.3


