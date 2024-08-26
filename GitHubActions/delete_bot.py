#botによるコミットを削除する]

import sys
import pandas as pd


args = sys.argv
if len(args) != 4:
    print("Usage: python delete_bot.py <input_file_path> <workflow_file_path> <output_file_path>")
    sys.exit()

input_file_path = args[1]
workflow_file_path = args[2]
output_file_path = args[3]

df = pd.read_csv(input_file_path)
workdlow_df = pd.read_csv(workflow_file_path)

output_df = pd.DataFrame()
len_df = len(df)
# botによるコミットを削除
for i, row in df.iterrows():
    is_bot = False
    commit_hash = row["commit_hash"]
    #同じcommit_hashを持つ行をwordkflow_dfから取得
    workflow = workdlow_df[workdlow_df["commit_hash"] == commit_hash]
    for _, workflow_row in workflow.iterrows():
        #botによるコミットかどうかを判定
        if "bot" in workflow_row["committer_name"]:
            is_bot = True
            break
    if not is_bot:
        output_df = pd.concat([output_df, row])
    print(f"{i+1}/{len_df}")

output_df.to_csv(output_file_path, index=False)