import pandas as pd

input_file_path1 = "resources/commit_log_max.csv"
input_file_path2 = "resources/commit_log_max_2.csv"
output_file_path = "resources/merged_commit_log.csv"

df1 = pd.read_csv(input_file_path1)
df2 = pd.read_csv(input_file_path2)

# 2つのDataFrameを結合
merged_df = pd.concat([df1, df2])

# 結合したDataFrameをCSVファイルに出力
merged_df.to_csv(output_file_path, index=False)