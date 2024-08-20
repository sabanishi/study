import pandas as pd

input_file = "resources/commit_log.csv"

df = pd.read_csv(input_file)

# log列を抽出
logs = df["log"].values.astype('U')

# logsの中に「#」を含む行を抽出
logs_hashtag = [log for log in logs if "#" in log]

# 数を出力する
print(f"全体: {len(logs)}, #を含む: {len(logs_hashtag)}")