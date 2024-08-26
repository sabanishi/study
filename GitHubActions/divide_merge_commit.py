import pandas as pd

input_file_path = "resources/merged_commit_log.csv"
output_file_path = "resources/commit_log_no_merge.csv"

df = pd.read_csv(input_file_path)
df["log"] = df["log"].astype(str)
# 同じcommit_hashを持つ行を削除
df = df.drop_duplicates(subset="commit_hash")

# log中に「#」が含まれる行を削除
df = df[~df["log"].str.contains("#",regex=False)]

# CSVファイルに出力
df.to_csv(output_file_path, index=False)

print(f"現在の行数: {len(df)}")