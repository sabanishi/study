# OpenAIのEmbeddingによってCommitLogをベクトル化するスクリプト

import sys
import pandas as pd
from openai import OpenAI
import re

stop_words = [
    'a', 'an', 'the', 'is', 'are', 'was', 'were', 'be', 'been', 'being', 'am', 'are', 'is',
    'all','and','as','by','chore','feat','for','from','in','more','of','on','to','update','updated','we','when','with',
    'fix','fixes','if','it','not','only','this','use',
    'off','that','ci','github','action','actions','add','added','change','changes','create','yml',
    'bot','new','so','some','try','workflow','workflows','also','now','signed','com','will','which','build'
]

def remove_stop_words(text, stop_words):
    # 単語ごとに分割してストップワードを除去
    words = re.findall(r'\b\w+\b', text.lower())
    filtered_words = [word for word in words if word not in stop_words]
    return ' '.join(filtered_words)

args = sys.argv
if len(args) != 4:
    print("Usage: python clustering.py <input_file_path> <output_file_path> <api_token>")
    sys.exit()

input_file_path = args[1]
output_file_path = args[2]
api_token = args[3]

df = pd.read_csv(input_file_path)

# log列を抽出
logs = df["log"].values.astype('U')
# stop wordsを除去
logs = [remove_stop_words(log, stop_words) for log in logs]

client = OpenAI(api_key=api_token)
vectors = []
i = 0
for log in logs:
    try:
        response = client.embeddings.create(
            input = log,
            model="text-embedding-3-small"
        )
        embedding = response.data[0].embedding
        #配列を文字列に変換
        embedding = ",".join(map(str, embedding))
        vectors.append(embedding)
        i += 1
        print(f"{i}/{len(logs)}終了")
    except Exception as e:
        print(f"{i}/{len(logs)}失敗")
        print(e)
        vectors.append("")

#結果をdfに追記
df["vector"] = vectors

# vector列が空の行を削除
df = df[df["vector"] != ""]

# csvファイルに保存
df.to_csv(output_file_path, index=False)