# 階層的クラスタリングを行うスクリプト

import matplotlib.pyplot as plt
import sys
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans
from sklearn.cluster import AgglomerativeClustering
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np

args = sys.argv
if len(args) != 4:
    print("Usage: python clustering_agg.py <input_file_path> <output_file_path> <cluster_count>")
    sys.exit()

input_file_path = args[1]
output_file_path = args[2]
num_clusters = int(args[3])

df = pd.read_csv(input_file_path)

# vector列を抽出
vectors = df["vector"].values.astype('U')
#カンマ区切りを配列に変換
vectors = [list(map(float, vector.split(','))) for vector in vectors]

# 階層的クラスタリングの実行
hierarchical = AgglomerativeClustering(n_clusters=num_clusters, linkage='average')
clusters = hierarchical.fit_predict(vectors)

# クラスターの重心を計算
centroids = []
for cluster_num in range(num_clusters):
    # 各クラスタに属するベクトルを抽出
    cluster_vectors = [vectors[i] for i in range(len(vectors)) if clusters[i] == cluster_num]
    # クラスタ内のベクトルの平均を計算して重心を求める
    centroid = np.mean(cluster_vectors, axis=0)
    centroids.append(centroid)

df['Cluster'] = clusters
sorted_data = df.sort_values(by="Cluster")

similarity_matrix = cosine_similarity(centroids, vectors)
average_similarity = similarity_matrix.mean(axis=1)
similarity_order = np.argsort(-average_similarity)

output_lines = []
for cluster_num in similarity_order:
    # クラスタラベル行を追加
    cluster_similarity = average_similarity[cluster_num]
    output_lines.append([f'Cluster {cluster_num} (Avg Similarity: {cluster_similarity:.4f})', '', ''])
    
    # このクラスタに属する行を抽出
    cluster_data = sorted_data[sorted_data['Cluster'] == cluster_num]
    
    # 各行のデータを追加
    for _, row in cluster_data.iterrows():
        output_lines.append([row['commit_hash'], row['url'], row['log']])

#行リストをDataFrameに変換
output_df = pd.DataFrame(output_lines, columns=["commit_hash","url","log"])

#出力する
output_df.to_csv(output_file_path, index=False)