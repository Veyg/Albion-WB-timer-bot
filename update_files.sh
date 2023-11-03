#!/bin/bash
set -e

NEW_VERSION=$1

# Update VERSION file
echo $NEW_VERSION > VERSION

# Update pom.xml with the new version
mvn versions:set -DnewVersion=$NEW_VERSION

# Update Dockerfile to use the new jar file name
sed -i '' "s/albionwbtimer-.*\.jar/albionwbtimer-$NEW_VERSION.jar/" Dockerfile
