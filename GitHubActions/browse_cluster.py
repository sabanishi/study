# クラスタ化されたファイル群を上から見ていくためのプログラム

import pandas as pd
import webbrowser
import sys
import pyperclip

args = sys.argv
if len(args) != 2:
    print("Usage: python search_vocab.py <input_file_path>")
    sys.exit()

input_file_path = args[1]
output_file_path = "browse_memo.txt"

df = pd.read_csv(input_file_path)

# url列を抽出
commit_hashs = df["commit_hash"].values.astype('U')
urls = df["url"].values.astype('U')
logs = df["log"].values.astype('U')

is_skip_to_cluster = False
# 上から順に開く
for i in range(len(urls)):
    url = urls[i].rstrip()
    log = logs[i]
    if "Cluster" in commit_hashs[i]:
        cluster_name = df["commit_hash"].values[i]
        features = df["url"].values[i]
        #featuresから改行を削除
        features = features.replace("\n", "")
        print(f"GoTo: {cluster_name},\n{features}")
        #クラスターに含まれる要素の数を表示
        #次にurl="nan"が出てくるまでの要素数を数える
        cluster_size = 0
        for j in range(i+1,len(logs)):
            if "Cluster" in commit_hashs[j]:
                break
            cluster_size += 1
        print(f"Cluster size: {cluster_size}")
        print("\007")

        #メモに書き込む
        with open(output_file_path, mode='a') as f:
            f.write(f"{cluster_name},\n{features}\n")
            f.write(f"Cluster size: {cluster_size}\n")
            f.write("\n")
        is_skip_to_cluster = False
        continue
    
    if is_skip_to_cluster:
        continue
    webbrowser.open(url,0)
    pyperclip.copy(url)
    print(url)

    #何かしらの入力があるまで待つ
    tmp = input()
    if tmp == "s":
        # sが入力された時、次のクラスターまでスキップする
        is_skip_to_cluster = True
