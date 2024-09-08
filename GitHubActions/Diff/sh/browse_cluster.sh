SCRIPT="../browse_cluster.py"
INPUT_FILE="../resources/result.csv"
OUTPUT_FILE="../resources/browse_memo.txt"
SKIP_TO="none"

COMMAND="python3 -u $SCRIPT $INPUT_FILE $OUTPUT_FILE $SKIP_TO"
LOG_FILE_NAME="browse_cluster"

source ./logger.sh $LOG_FILE_NAME $COMMAND