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
workflow_df = pd.read_csv(workflow_file_path)

output_df = pd.DataFrame()
catch_df = pd.DataFrame()
catch = 0
len_df = len(df)

# botによるコミットを削除
for i, row in df.iterrows():
    is_bot = False
    commit_hash = row["commit_hash"]
    # 同じcommit_hashを持つ行をworkflow_dfから取得
    workflow = workflow_df[workflow_df["commit_hash"] == commit_hash]
    for _, workflow_row in workflow.iterrows():
        # botによるコミットかどうかを判定
        if "bot" in workflow_row["committer_name"]:
            is_bot = True
            break
    if not is_bot:
        catch_df = pd.concat([catch_df, pd.DataFrame(row).T])
        catch += 1
        if catch >= 100:
            catch = 0
            output_df = pd.concat([output_df, catch_df])
            catch_df = pd.DataFrame()
        print(f"Add:{i+1}/{len_df}")
    else:
        print(f"Delete:{i+1}/{len_df}")  

# 残りのデータを追加
output_df = pd.concat([output_df, catch_df])
output_df.to_csv(output_file_path, index=False)
