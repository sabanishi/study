# Gitからコミットログを取得してきて、jsonファイルに保存する

import pandas as pd
import requests
import time
import sys
import json

def get_commit_json(commit_url,token) -> (str, str):
    # GitHubのAPIエンドポイントを構築
    url = commit_url.replace("https://github.com/", "https://api.github.com/repos/").replace("/commit/", "/commits/")

    headers = {"Authorization": f"token {token}"}
    # APIリクエストを送信
    response = requests.get(url, headers=headers)
    # レスポンスのJSONデータを取得
    data = response.json()

    # コミットメッセージを取得
    log = data['commit']['message']

    return (data, log)

args = sys.argv
if len(args) != 6:
    print("Usage: python fetch_json_log.py <token> <input_file_path> <output_file_path> <output_folder_path> <skip_to>")
    sys.exit()

token = args[1]
input_file_path = args[2]
output_file_path = args[3]
output_folder_path = args[4]
first_commit = args[5]

#取ってくるコミットの数
fetch_amount = 10000
shuffle_seed = 9999

print("読み込み開始")
df = pd.read_csv(input_file_path)
print("読み込み完了")

#ファイルをシャッフル
df = df.sample(frac=1,random_state=shuffle_seed)
output_df = pd.DataFrame(columns=["commit_hash","url","log"])
buffer = []

i = 0
success_count = 0
is_skip = False
batch_size = 100
while(True):
    #一定数のコミットログを取得したら終了
    if success_count >= fetch_amount or i >= len(df):
        break

    commit = df.iloc[i]
    i+=1
    commit_hash = commit["commit_hash"]
    if is_skip==False:
        if first_commit == "none" or commit_hash == first_commit:
            is_skip=True
        else:
            print(f"First Skip:({i}件目); {commit_hash}")
            continue

    url = "https://github.com/"+commit["repository"]+"/commit/"+commit_hash
    #コミットログを取得
    while(True):
        try:
            #Committerがbotの場合はスキップ
            if "bot" in commit["committer_name"]:
                print(f"Skip(Bot Commit): {commit_hash}")
                break
            now_time = time.time()
            (data,log) = get_commit_json(url,token)
            #コミットログに「Merge」または「merge」または「#」が含まれている場合はスキップ
            if "Merge" in log or "merge" in log or "#" in log:
                print(f"Skip(Merge Commit): {commit_hash}")
                break

            #jsonファイルを保存
            json_path = output_folder_path +"/"+ commit_hash + ".json"
            with open(json_path, mode='w') as f:
                json.dump(data, f, indent=4)
            
            buffer.append({
                "commit_hash": commit_hash,
                "url": url,
                "log": log
            })

            if len(buffer) >= batch_size:
                output_df = pd.concat([output_df, pd.DataFrame(buffer)],ignore_index=True)
                output_df.to_csv(output_file_path,index=False)
                buffer.clear()
            
            success_count += 1
            print(f"Success({success_count}件目): {commit_hash}")

            # 0.8秒待機
            elapsed_time = time.time() - now_time
            if elapsed_time < 0.8:
                time.sleep(0.8-elapsed_time)
                break
        except Exception as e:
            #エラーが発生した場合は1分待機
                print(f"Error: {e}")
                time.sleep(60)
                break

if buffer:
    output_df = pd.concat([output_df, pd.DataFrame(buffer)],ignore_index=True)
    output_df.to_csv(output_file_path,index=False)