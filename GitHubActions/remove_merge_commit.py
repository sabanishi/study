import pandas as pd

input_file = "resources/commit_log.csv"
output_file = "resources/commit_log_no_merge.csv"

df = pd.read_csv(input_file)

# log列を文字列型に変換
df["log"] = df["log"].astype(str)

# 「#」を含む行を削除
df = df[~df["log"].str.contains("#")]

# CSVファイルに出力
df.to_csv(output_file, index=False)
