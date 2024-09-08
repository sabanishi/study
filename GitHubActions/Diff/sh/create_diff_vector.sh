SCRIPT="../create_diff_vector.py"
INPUT_FILE="../resources/fetch_suceed_file_commit.csv"
INPUT_FOLDER="../resources/json"
OUTPUT_FILE="../resources/diff_vector.json"
SKIP_TO="none"

COMMAND="python3 -u $SCRIPT $INPUT_FILE $INPUT_FOLDER $OUTPUT_FILE $SKIP_TO"
LOG_FILE_NAME="create_diff_vector"

source ./logger.sh $LOG_FILE_NAME $COMMAND