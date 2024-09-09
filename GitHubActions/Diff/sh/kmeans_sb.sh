SCRIPT="../kmeans.py"
INPUT_FILE="../resources/sandbox/diff_vector_num.csv"
OUTPUT_FILE="../resources/sandbox/result.csv"
FEATURE_FILE="../resources/sandbox/features.csv"
NUM_CLUSTERS=2

COMMAND="python3 -u $SCRIPT $INPUT_FILE $OUTPUT_FILE $FEATURE_FILE $NUM_CLUSTERS"
LOG_FILE_NAME="kmeans"

source ./logger.sh $LOG_FILE_NAME $COMMAND