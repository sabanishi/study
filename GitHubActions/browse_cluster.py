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

df = pd.read_csv(input_file_path)

# url列を抽出
urls = df["url"].values.astype('U')

is_skip_to_cluster = False
# 上から順に開く
for i in range(len(urls)):
    url = urls[i].rstrip()
    if url == "" or url == "nan":
        cluster_name = df["commit_hash"].values[i]
        print(f"GoTo: {cluster_name}")
        # クラスターに含まれる要素の数を表示
        #次にurl="nan"が出てくるまでの要素数を数える
        cluster_size = 0
        for j in range(i+1,len(urls)):
            if urls[j] == "" or urls[j] == "nan":
                break
            cluster_size += 1
        print(f"Cluster size: {cluster_size}")
        print("\007")
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
