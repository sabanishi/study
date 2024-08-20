# OpenAIのEmbeddingによってCommitLogをベクトル化するスクリプト

import matplotlib.pyplot as plt
import sys
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans
from sklearn.cluster import AgglomerativeClustering
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np
from openai import OpenAI

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

client = OpenAI(api_key=api_token)
vectors = []
i = 0
for log in logs:
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

#結果をdfに追記
df["vector"] = vectors
# csvファイルに保存
df.to_csv(output_file_path, index=False)