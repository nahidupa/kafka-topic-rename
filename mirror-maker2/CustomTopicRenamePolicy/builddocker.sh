#!/bin/bash
docker buildx build CustomTopicRenamePolicy  --platform "linux/amd64"  -t nahidupa/custom-mirrormaker:0.0.3
docker push nahidupa/custom-mirrormaker:0.0.3
docker push nahidupa/custom-mirrormaker:latest
