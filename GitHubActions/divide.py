import pandas as pd

input_file_path = "resources/commit_log_no_merge.csv"
output_file_path = "resources/commit_log_divided.csv"

df = pd.read_csv(input_file_path)

#上位10件だけを取得
df = df.head(10)

# 出力する
df.to_csv(output_file_path, index=False)