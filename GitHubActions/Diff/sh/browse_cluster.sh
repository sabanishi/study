SCRIPT="../browse_cluster.py"
INPUT_FILE="../resources/result.csv"
OUTPUT_FILE="../resources/browse_memo.txt"

COMMAND="python3 -u $SCRIPT $INPUT_FILE $OUTPUT_FILE"
LOG_FILE_NAME="browse_cluster"

source ./logger.sh $LOG_FILE_NAME $COMMAND