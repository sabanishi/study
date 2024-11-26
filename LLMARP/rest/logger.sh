if [ $# -lt 2 ]; then
    echo "Usage: logger.sh LOGGER_NAME COMMAND"
    exit 1
fi

LOGGER_NAME=$1
COMMAND="${@:2}"

LOG_FILE="log/$(date '+%Y_%m_%d_%H_%M')_$LOGGER_NAME.txt"

#コマンドを実行
$COMMAND | tee $LOG_FILE