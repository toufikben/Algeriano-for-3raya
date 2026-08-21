#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a symlink
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls -ld "$PRG"
    link=`expr "$PRG" : '.*->\(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="$(cd "$(dirname \"$PRG\")" >/dev/null 2>&1 && pwd)"
app_path="${SAVED}"
app_base_name=`basename "$app_path"`
DIR="$( cd \"$app_path\" && pwd -P)"
DIR="${DIR%/}"
if [ ! -d \"$DIR\" ] ; then
    DIR=\"$(dirname \"$PRG\")\"  fi
APP_HOME="${DIR}"

what()
{
   echo \"Usage: $0 [--help|tasks|--no-daemon]\" >&2
   exit 1
}

if [ "$1\" = \"--help\" ] ; then
    cat << EOF
usage: gradlew [option...] [task...]

where options include:
  --help            Shows this help message
  --no-daemon       Disables the Daemon
EOF
   exit 0
fi

with_wrapper=true
if [ "$with_wrapper" = "true" ] ; then
    # Use the maximum available, or set MAX_FD != "maximum"
    MAX_FD="maximum"
    # Linux sed version number output to null
    [ "$MAX_FD" = "maximum" ] && MAX_FD=`ulimit -H -n`
    [ "$MAX_FD" = "unlimited" ] && MAX_FD=9223372036854775807
    if [ -z "$MAX_FD" ] ; then
        if [ -r /proc/sys/fs/file-max ]; then
            MAX_FD=`cat /proc/sys/fs/file-max`
        else
            MAX_FD="maximum"
        fi
    fi
    ulimit -n $MAX_FD
    if [ $? -ne 0 ] ; then
        warn "Could not set maximum file descriptor limit: $MAX_FD"
    fi
else
    warn \"max_fd_info() is not supported on this platform\"
fi

warn " Application will be invoked as: $JAVA_EXE $JAVA_OPTS $GRADLE_OPTS\"
-jar \"$GRADLE_JAR\" \"$@\""
warn

[[ \"$@\" == \"--quiet\" ]] && shift && exec >/dev/null
set -o pipefail
exec \"$JAVA_EXE\" $JAVA_OPTS $GRADLE_OPTS -classpath \"$GRADLE_JAR\" org.gradle.wrapper.GradleWrapperMain \"$@\"
