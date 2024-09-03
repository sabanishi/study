import hdbscan
import sys
import pandas as pd

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

# HDBSCANの実行
clusterer = hdbscan.HDBSCAN(min_cluster_size=num_clusters)
clusters = clusterer.fit_predict(vectors)

print("Finished clustering")

df['Cluster'] = clusters
sorted_data = df.sort_values(by="Cluster")

output_lines = []
for cluster_num in range(num_clusters):
    # クラスタラベル行を追加
    cluster_data = sorted_data[sorted_data['Cluster'] == cluster_num]
    
    # 各行のデータを追加
    for _, row in cluster_data.iterrows():
        output_lines.append([row['commit_hash'], row['url'], row['log']])

#行リストをDataFrameに変換
output_df = pd.DataFrame(output_lines, columns=["commit_hash","url","log"])
output_df.to_csv(output_file_path, index=False)

print("Finished output")