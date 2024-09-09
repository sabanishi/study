SCRIPT="../vectorize.py"
INPUT_FILE="../resources/sandbox/diff_vector.json"
OUTPUT_FILE="../resources/sandbox/diff_vector_num.csv"
FEATURE_FILE="../resources/sandbox/features.csv"

COMMAND="python3 -u $SCRIPT $INPUT_FILE $OUTPUT_FILE $FEATURE_FILE"
LOG_FILE_NAME="vectorize_sb"

source ./logger.sh $LOG_FILE_NAME $COMMAND