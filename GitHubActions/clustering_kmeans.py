# KMeansクラスタリングを行うスクリプト

import pandas as pd
from sklearn.cluster import KMeans
from sklearn.metrics.pairwise import cosine_similarity
from sklearn.metrics import silhouette_score
import numpy as np
import sys

args = sys.argv
if len(args) != 4:
    print("Usage: python clustering_kmeans.py <input_file_path> <output_file_path> <cluster_count>")
    sys.exit()

input_file_path = args[1]
output_file_path = args[2]
num_clusters = int(args[3])

df = pd.read_csv(input_file_path)

# vectorのラベルを読み込み
vector_labels = pd.read_csv("feature_names.csv")

# vector列を抽出
vectors = df["vector"].values.astype('U')
#カンマ区切りを配列に変換
vectors = [list(map(float, vector.split(','))) for vector in vectors]

# KMeansクラスタリングの実行
print("Clustering...")
kmeans = KMeans(n_clusters=num_clusters,random_state=999, verbose=1)
kmeans.fit(vectors)
clusters = kmeans.predict(vectors)
# クラスターの重心を取得
centroids = kmeans.cluster_centers_
print("Clustering finished")

#シルエット係数を計算する
silhouette_avg = silhouette_score(vectors, clusters)
print(f"Silhouette Score: {silhouette_avg}")

df['Cluster'] = clusters
sorted_data = df.sort_values(by="Cluster")

similarity_matrix = cosine_similarity(centroids, vectors)
average_similarity = similarity_matrix.mean(axis=1)
similarity_order = np.argsort(-average_similarity)

output_lines = []
for cluster_num in similarity_order:
    # クラスタラベル行を追加
    cluster_similarity = average_similarity[cluster_num]
    # 特徴的な単語を取得
    cluster_vector = centroids[cluster_num]
    #ベクトル内の値が大きい要素5個のインデックスを取得
    top_indices = np.argsort(-cluster_vector)[:5]
    top_words = vector_labels.iloc[top_indices].values
    output_lines.append([f"Cluster {cluster_num} (Similarity: {cluster_similarity:.2f})", top_words])
    
    # このクラスタに属する行を抽出
    cluster_data = sorted_data[sorted_data['Cluster'] == cluster_num]
    
    # 各行のデータを追加
    for _, row in cluster_data.iterrows():
        output_lines.append([row['commit_hash'], row['url'], row['log']])

#行リストをDataFrameに変換
output_df = pd.DataFrame(output_lines, columns=["commit_hash","url","log"])

#出力する
output_df.to_csv(output_file_path, index=False)