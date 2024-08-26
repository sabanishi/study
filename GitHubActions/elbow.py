#エルボー方によるクラスタ数の決定

import pandas as pd
from sklearn.cluster import KMeans
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np
import sys
import matplotlib.pyplot as plt

args = sys.argv
if len(args) != 4:
    print("Usage: python clustering_kmeans.py <input_file_path> <output_file_path> <cluster_count>")
    sys.exit()

input_file_path = args[1]
output_file_path = args[2]
num_clusters = int(args[3])

df = pd.read_csv(input_file_path)

# vector列を抽出
vectors = df["vector"].values.astype('U')
#カンマ区切りを配列に変換
vectors = [list(map(float, vector.split(','))) for vector in vectors]

max_clusters = 10000

wcss = []

#1~10000クラスタまで1000刻みでKMeansを実行し、WCSSを計算
for i in range(1000, max_clusters, 1000):
    kmeans = KMeans(n_clusters=i,random_state=999, verbose=1)
    kmeans.fit(vectors)
    wcss.append(kmeans.inertia_)
    print(f"KMeans: {i}/{max_clusters}")

#WCSSをプロット
plt.plot(range(1, max_clusters, 100), wcss)
plt.title('Elbow Method')
plt.xlabel('Number of clusters')
plt.ylabel('WCSS')
plt.show()