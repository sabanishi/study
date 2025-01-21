LOGGER_NAME="my_process"

path=(
  mall
  arthas
)

process (){
  COMMAND="${@:1}"
  LOG_FILE="log/$(date '+%Y_%m_%d_%H_%M')_$LOGGER_NAME.txt"
  $COMMAND | tee $LOG_FILE
}

process "./gradlew shadowJar"

for p in ${path[@]}; do
  process "java -jar build/libs/LLMARP-1.0-SNAPSHOT-all.jar -d=$p.db init"
  process "java -jar build/libs/LLMARP-1.0-SNAPSHOT-all.jar -d=$p.db extract -r=../repo/$p/.git"
done

process "java -jar build/libs/LLMARP-1.0-SNAPSHOT-all.jar -d=hal_repair.db init"
for p in ${path[@]}; do
  process "java -jar build/libs/LLMARP-1.0-SNAPSHOT-all.jar move -f=hal_repair.db -t=$p.db"
done