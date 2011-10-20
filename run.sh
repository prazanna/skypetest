#!/bin/bash

export PRJ="$(cd `dirname $0`; pwd)"

    
    java \
    -Xmx2048m -Xms2048m -Xss128k -XX:-PrintConcurrentLocks \
    -classpath target/classes:`cat target/classpath` \
    -Djava.util.logging.config.file=$PRJ/src/main/resources/logging.properties \
    org.skype.test.simulation.Main \
    $@