SCRIPT="main.py"

#トークンが書かれたファイルを開く
API_FILE="secrets/openai_api.txt"
#ファイルが存在しない時
if [ ! -e $API_FILE ]; then
    echo "LLMARP/rest/secretsフォルダ内にopenai_api.txtを作成し、APIキーを書き込んでください"
    exit 1
fi
API_KEY=$(cat $API_FILE)

COMMAND="python3 -u $SCRIPT $API_KEY"
LOG_FILE_NAME="python_rest"

source ./logger.sh $LOG_FILE_NAME $COMMAND
