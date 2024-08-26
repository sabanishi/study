# GitHubからコミットログを取得してきて、テーブルに追加するスクリプト

import pandas as pd
import requests
import time
import sys

def get_commit_title(commit_url,token):
    # GitHubのAPIエンドポイントを構築
    url = commit_url.replace("https://github.com/", "https://api.github.com/repos/").replace("/commit/", "/commits/")

    is_success = False
    try:
        headers = {"Authorization": f"token {token}"}

        # APIリクエストを送信
        response = requests.get(url, headers=headers)
    
        # レスポンスのJSONデータを取得
        data = response.json()
        # コミットメッセージを取得
        commit_title = data['commit']['message']
        is_success = True
    except Exception as e:
        commit_title = ""
    
    return is_success, commit_title

args = sys.argv
if len(args) != 5:
    print("Usage: python fetch_commit_log.py <token> <input_file_path> <output_file_path> <skip_to>")
    sys.exit()

token = args[1]
input_file_path = args[2]
output_file_path = args[3]
first_commit = args[4]

#取ってくるコミットの数
fetch_amount = 10000000
shuffle_seed = 9999
batch_size = 100

df = pd.read_csv(input_file_path)
print("読み込み完了")

#ファイルをシャッフル
df = df.sample(frac=1,random_state=shuffle_seed)
output_df = pd.DataFrame(columns=["commit_hash","url","log"])
buffer = []

i = 0
success_count = 0
is_skip = False
while(True):
    #一定数のコミットログを取得したら終了
    if success_count >= fetch_amount or i >= len(df):
        break

    commit = df.iloc[i]
    i+=1
    commit_hash = commit["commit_hash"]
    if is_skip==False:
        print(f"skip({i}件目); {commit_hash}")
        if commit_hash == first_commit:
            is_skip=True
        continue

    url = "https://github.com/"+commit["repository"]+"/commit/"+commit_hash
    #コミットログを取得
    error_count = 0
    while(True):
        try:
            now_time = time.time()
            is_success, log = get_commit_title(url,token)
            if is_success:
                #コミットログに「Merge」または「merge」が含まれている場合はスキップ
                if "Merge" in log or "merge" in log:
                    print(f"Skip: {commit_hash}")
                    break

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
            else:
                print(f"Error: {commit_hash}")
                error_count += 1
                time.sleep(60)
                #1回やってダメだったら諦める
                if error_count >= 1:
                    break
        except Exception as e:
            print(e)
            break

if buffer:
    output_df = pd.concat([output_df, pd.DataFrame(buffer)],ignore_index=True)
    output_df.to_csv(output_file_path,index=False)