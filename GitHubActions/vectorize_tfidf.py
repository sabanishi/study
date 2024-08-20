# TF-IDF特徴量によってCommitLogをベクトル化するスクリプト

import matplotlib.pyplot as plt
import sys
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans
from sklearn.cluster import AgglomerativeClustering
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np

args = sys.argv
if len(args) != 3:
    print("Usage: python clustering.py <input_file_path> <output_file_path>")
    sys.exit()

input_file_path = args[1]
output_file_path = args[2]

df = pd.read_csv(input_file_path)

# log列を抽出
logs = df["log"].values.astype('U')

vectorizer = TfidfVectorizer(
    # テキストデータをTF-IDF特徴量に変換
    min_df=5,
    max_features=100,
    stop_words=[
        'a', 'an', 'the', 'is', 'are', 'was', 'were', 'be', 'been', 'being', 'am', 'are', 'is',
        'all','and','as','by','chore','feat','for','from','in','more','of','on','to','update','updated','we','when','with',
        'fix','fixes','if','it','not','only','this','use',
        'off','that','ci','github','action','actions','add','added','change','changes','create','yml',
        'bot','new','so','some','try','workflow','workflows','also','now','signed','com','will','which','build'],
    analyzer='word',
    )
tf_idfs = vectorizer.fit_transform(logs)

vectors = tf_idfs.toarray()
#配列をカンマ区切りの文字列に変換
vectors = [','.join(map(str, vector)) for vector in vectors]

#結果をdfに追記
df["vector"] = vectors
# csvファイルに保存
df.to_csv(output_file_path, index=False)