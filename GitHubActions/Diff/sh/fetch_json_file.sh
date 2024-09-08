SCRIPT="../fetch_json_file.py"
INPUT_FILE="../resources/workflows.csv"
OUTPUT_FILE="../resources/fetch_suceed_file_commit.csv"
OUTPUT_FOLDER="../resources/json"

FETCH_AMOUNT=10000
SHUFFLE_SEED=999

SKIP_TO="none"

#トークンが書かれたファイルを開く
TOKEN_FILE="../secrets/github_token.txt"
#ファイルが存在しない時
if [ ! -e $TOKEN_FILE ]; then
    echo "Diff/secretsフォルダ内にgithub_token.txtを作成してください"
    exit 1
fi
TOKEN=$(cat $TOKEN_FILE)

COMMAND="python3 -u $SCRIPT $TOKEN $INPUT_FILE $OUTPUT_FILE $OUTPUT_FOLDER $FETCH_AMOUNT $SHUFFLE_SEED $SKIP_TO"
LOG_FILE_NAME="fetch_json_file"

source ./logger.sh $LOG_FILE_NAME $COMMAND