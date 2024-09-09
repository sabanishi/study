SCRIPT="../create_diff_vector.py"
INPUT_FILE="../resources/sandbox/fetch_suceed_file_commit.csv"
INPUT_FOLDER="../resources/json"
OUTPUT_FILE="../resources/sandbox/diff_vector.json"
SKIP_TO="none"

COMMAND="python3 -u $SCRIPT $INPUT_FILE $INPUT_FOLDER $OUTPUT_FILE $SKIP_TO True"
LOG_FILE_NAME="create_diff_spec"

source ./logger.sh $LOG_FILE_NAME $COMMAND