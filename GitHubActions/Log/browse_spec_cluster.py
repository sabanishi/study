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

while(True):
    #入力があるまで待機
    print("Input:")
    tmp = input()
    #tmpが数字である時、そのクラスターを開く
    if tmp.isdecimal():
        cluster_num = int(tmp)
        cluster_name = f"Cluster {cluster_num}"
        now_place = 0
        for i in range(len(commit_hashs)):
            if cluster_name in commit_hashs[i]:
                now_place = i
                break
        features = df["url"].values[now_place]
        if type(features) is not float:
            features = features.replace("\n", "")

        print(f"GoTo: {cluster_name}, {features}")
        

        #Cluster {clsuet_num}がある行から次に「Cluster 」が登場するまでの要素を列挙
        for i in range(now_place+1,len(commit_hashs)):
            if "Cluster" in commit_hashs[i]:
                break
            print(f"{commit_hashs[i]}")
            #URLを開く
            webbrowser.open(urls[i])
            #入力があるまで待機
            tmp = input()
            if tmp == "s":
                break
            
