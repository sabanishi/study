SCRIPT="../kmeans.py"
INPUT_FILE="../resources/diff_vector_num.csv"
OUTPUT_FILE="../resources/result.csv"
FEATURE_FILE="../resources/features.csv"
NUM_CLUSTERS=100

COMMAND="python3 -u $SCRIPT $INPUT_FILE $OUTPUT_FILE $FEATURE_FILE $NUM_CLUSTERS"
LOG_FILE_NAME="kmeans"

source ./logger.sh $LOG_FILE_NAME $COMMAND