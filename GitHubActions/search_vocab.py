# 特定の単語をログに含むコミットを探すスクリプト

import webbrowser
import sys
import pyperclip
import pandas as pd

args = sys.argv
if len(args) != 3:
    print("Usage: python search_vocab.py <input_file_path> <vocab>")
    sys.exit()

input_file_path = args[1]
vocab = args[2]

df = pd.read_csv(input_file_path)
df["log"] = df["log"].fillna("")

# 特定の単語を含むコミットを抽出
df = df[df["log"].str.contains(vocab)]

print(f"Amount : {len(df)}")

# 結果を1つずつ表示する
for i in range(len(df)):
    url = df["url"].values[i].rstrip()
    webbrowser.open(url,0)
    pyperclip.copy(url)
    print(url)
    input()
        