# クラスタ化されたファイル群を上から見ていくためのプログラム

import pandas as pd
import webbrowser
import sys
import pyperclip

args = sys.argv
if len(args) != 3:
    print("Usage: python browse_cluster.py <input_file_path> <output_file_path>")
    sys.exit()

input_file_path = args[1]
output_file_path = args[2]

df = pd.read_csv(input_file_path)

# url列を抽出
commit_hashs = df["name"].values.astype('U')
urls = df["url"].values.astype('U')

is_skip_to_cluster = False
skip_next_cluster = False
next_cluster_num = 0
# 上から順に開く
for i in range(len(urls)):
    url = urls[i].rstrip()
    name = commit_hashs[i]
    if "Cluster" in name:
        if skip_next_cluster:
            if next_cluster_num == int(name.split(" ")[1]):
                skip_next_cluster = False
            else:
                continue
        features = df["features"].values[i]
        #featuresがfloatでない時
        if type(features) is not float:
            features = features.replace("\n", "")
        print(f"GoTo: {name},\n{features}")
        #クラスターに含まれる要素の数を表示
        cluster_size = 0
        for j in range(i+1,len(commit_hashs)):
            if "Cluster" in commit_hashs[j]:
                break
            cluster_size += 1
        print(f"Cluster size: {cluster_size}")
        print("\007")

        #メモに書き込む
        with open(output_file_path, mode='a') as f:
            f.write(f"{name}\n")
            f.write(f"Cluster size: {cluster_size}\n")
            f.write(f"{features}\n")
            f.write("\n")
        is_skip_to_cluster = False
        skip_next_cluster = False
        continue
    
    if is_skip_to_cluster or skip_next_cluster:
        continue
    webbrowser.open(url,0)
    pyperclip.copy(url)
    print(f"GoTo: {name}:{url}")

    #何かしらの入力があるまで待つ
    tmp = input()
    if tmp == "s":
        # sが入力された時、次のクラスターまでスキップする
        is_skip_to_cluster = True
    elif tmp.isdecimal():
        # 数字が入力された時、そのクラスターまでスキップする
        next_cluster_num = int(tmp)
        is_skip_to_cluster = True
        skip_next_cluster = True