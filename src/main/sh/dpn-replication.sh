#!/bin/sh

### BEGIN INIT INFO
# Provides:      dpn-replication
# Default-Start: 3 5
# Default-Stop:  0 1 2 6
# Description:   Start the DPN-Chronopolis replication service
### END INIT INFO

# User to execute as
CHRON_USER="chronopolis"

REPL_JAR="/usr/lib/dpn/dpn-replication.jar"
REPL_PID_FILE="/var/run/dpn-replication.pid"

# Set the location which holds our application.properties
# SPRING_CONFIG_NAME="application.yml"
SPRING_CONFIG_LOCATION="/etc/dpn/"

JAVA_BIN=/usr/bin/java
JAVA_CMD="$JAVA_BIN -jar $REPL_JAR"
PARAMS="--spring.config.location=$SPRING_CONFIG_LOCATION &"

. /etc/init.d/functions

RETVAL=0
COUNTDOWN=1

case "$1" in
    start)
    echo "Starting the dpn replication service"
    daemon --user "$CHRON_USER" --pidfile "$REPL_PID_FILE" $JAVA_CMD $PARAMS > /dev/null 2>&1
    RETVAL=$?

    echo "Waiting for startup to complete..."
    while [ $COUNTDOWN -gt 0 ]; do
        sleep 8 # This seems to be the minimum amount of time to get consistent results
        let COUNTDOWN=0
    done

    RUNNING=1
    # This from the jenkins init script... slightly modified for our use
    if [ $RETVAL -eq 0 ]; then
        # Create a pipe to read from so we can still alter $RUNNING
        if [ ! -p check_pipe ]; then
            mkfifo check_pipe
        fi

        /bin/ps hww -u "$CHRON_USER" -o sess,ppid,pid,cmd > check_pipe &
        while read sess ppid pid cmd; do
            [ $ppid -eq 1 ] || continue
            echo "$cmd" | grep $REPL_JAR > /dev/null
            [ $? -eq 0 ] || continue
            echo $pid > $REPL_PID_FILE
            let RUNNING=0
            echo $RUNNING
        done < check_pipe
    fi

    if [ $RUNNING -eq 0 ]; then
        success
    else
        failure
    fi

    RETVAL=$RUNNING
    ;;
    stop)
    echo "Stopping the dpn replication service"
    killproc replication
    ;;
    restart)
    $0 stop
    $0 start
    ;;
    status)
        status replication
        RETVAL=$?
    ;;
esac

exit $RETVAL
