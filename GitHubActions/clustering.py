# コミットログを基にコミットをクラスタリングするスクリプト

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

# マジックナンバーの設定
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
num_clusters = 120
is_hierarchic = True
tf_idfs = vectorizer.fit_transform(logs)

# エルボー法によるクラスタ数の推定
'''
max_clusters = 100
# WCSSを格納するリスト
wcss = []

# 1からmax_clustersまでのクラスタ数でKMeansを実行
for i in range(1, max_clusters + 1):
    kmeans = KMeans(n_clusters=i*10, random_state=999)
    kmeans.fit(tf_idfs)
    wcss.append(kmeans.inertia_)
    print(f"KMeans: {i*10}/{max_clusters*10}")

# WCSSをプロットしてエルボーグラフを表示
plt.figure(figsize=(10, 6))
plt.plot(range(1, max_clusters + 1), wcss, marker='o', linestyle='--')
plt.title('Elbow Method')
plt.xlabel('Number of clusters')
plt.ylabel('WCSS')
plt.show()
'''

clusters = []
if is_hierarchic == True:
    # 階層的クラスタリングの実行
    hierarchical = AgglomerativeClustering(n_clusters=num_clusters, linkage='average')
    clusters = hierarchical.fit_predict(tf_idfs.toarray())
    # クラスターの重心を計算
    centroids = []
    for i in range(num_clusters):
        cluster_data = tf_idfs[clusters == i]
        if cluster_data.shape[0] > 0:
            centroids.append(cluster_data.mean(axis=0))
        else:
            centroids.append(np.zeros(tf_idfs.shape[1]))
    centroids = np.array(centroids).reshape(num_clusters, -1)
else:
    # KMeansクラスタリングの実行
    kmeans = KMeans(n_clusters=num_clusters,random_state=999)
    kmeans.fit(tf_idfs)
    clusters = kmeans.predict(tf_idfs)
    # クラスターの重心を取得
    centroids = kmeans.cluster_centers_

df['Cluster'] = clusters
sorted_data = df.sort_values(by="Cluster")

similarity_matrix = cosine_similarity(centroids, tf_idfs)
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

# 特徴量を出力する
tf_pd = pd.DataFrame(tf_idfs.toarray())
tf_pd.columns = vectorizer.get_feature_names_out()
print(tf_pd.columns.to_list())