# TF-IDF特徴量によってCommitLogをベクトル化するスクリプト

import sys
import pandas as pd
from sklearn.feature_extraction.text import TfidfVectorizer

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
    max_features=1000,
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

#各ベクトルに対応する単語を取得
feature_names = vectorizer.get_feature_names_out()

#それをcsvファイルに出力
pd.DataFrame(feature_names).to_csv("feature_names.csv", index=False)