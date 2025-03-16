SCRIPT="main.py"

API_KEY=$1

COMMAND="python3 -u $SCRIPT $API_KEY"
LOG_FILE_NAME="python_rest"

source ./logger.sh $LOG_FILE_NAME $COMMAND
