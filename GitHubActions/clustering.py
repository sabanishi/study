# コミットログを基にコミットをクラスタリングするスクリプト

import sys
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.cluster import KMeans

args = sys.argv
if len(args) != 3:
    print("Usage: python clustering.py <input_file_path> <output_file_path>")
    sys.exit()

input_file_path = args[1]
output_file_path = args[2]

df = pd.read_csv(input_file_path)

# log列を抽出
logs = df["log"].values

# テキストデータをTF-IDF特徴量に変換
vectorizer = TfidfVectorizer(
    min_df=5,
    max_features=1000,
    stop_words=['a', 'an', 'the', 'is', 'are', 'was', 'were', 'be', 'been', 'being', 'am', 'are', 'is'],
    analyzer='word',
    )

# KMeansクラスタリングの実行
tf_idfs = vectorizer.fit_transform(logs)
num_clusters = 5
kmeans = KMeans(n_clusters=num_clusters,random_state=999)
kmeans.fit(tf_idfs)

clusters = kmeans.predict(tf_idfs)
df['Cluster'] = clusters
sorted_data = df.sort_values(by="Cluster")

output_lines = []
for cluster_num in range(num_clusters):
    # クラスタラベル行を追加
    output_lines.append([f'Cluster {cluster_num}', '', ''])
    
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