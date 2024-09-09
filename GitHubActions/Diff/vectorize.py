# patch生ベクトルを数値ベクトルに変換する

import pandas as pd
import json
import sys
from RawPatchCommit import RawPatchCommit
from sklearn.feature_extraction.text import TfidfVectorizer

def convert_to_stop_words(words):
    result = []
    #「 +hoge 」と「 -hoge 」の形にして返す
    for word in words:
        result.append(f' [+{word}] ')
        result.append(f' [-{word}] ')
    return result

args = sys.argv
if len(args) != 4:
    print("Usage: python vectorize.py <input_file_path> <output_file_path> <features_file_path>")
    sys.exit()

input_file_path = args[1]
output_file_path = args[2]
features_file_path = args[3]

#パッチデータをjsonから読み込み、中間状態を生成
data_list = []
with open(input_file_path, "r") as f:
    raw_data = json.load(f)["data"]
    for data in raw_data:
        commit = RawPatchCommit.from_json(data)
        for patch in commit.patch:
            file_name = patch.file_name
            for chunk in patch.chunks:
                chunk_name = chunk.name
                is_valid = chunk.is_valid
                chunk_data = chunk.data

                #positive_wordsとnegative_wordsの両方が存在するもののみを抽出する
                if is_valid:
                    data_list.append({
                        "commit_hash":commit.commit_hash,
                        "url":commit.url,
                        "file_name":file_name,
                        "chunk_name":chunk_name,
                        "raw_vector":chunk_data
                    })

df = pd.DataFrame(data_list,columns=["commit_hash","url","file_name","chunk_name","raw_vector"])
#TF-IDFのパッケージをsetupする
vectorizer = TfidfVectorizer(
    # テキストデータをTF-IDF特徴量に変換
    min_df=1,
    max_features=1000,
    analyzer='word',
    token_pattern=r"(?u)\s[\+\-]\w+\s",
    stop_words=convert_to_stop_words([
        'a', 'an', 'the', 'is', 'are', 'was', 'were', 'be', 'been', 'being', 'am', 'are', 'is',
        'all','and','as','by','chore','feat','for','from','in','more','of','on','to','update','updated','we','when','with',
        'fix','fixes','if','it','not','only','this','use',
        'off','that','ci','github','action','actions','add','added','change','changes','create','yml',
        'bot','new','so','some','try','workflow','workflows','also','now','signed','com','will','which','build',
        'test','tests','release','version','run','make','yaml',
        'reverts','revert','commit'])
)

#単語ベクトルを空白文字で結合
raw_vectors = df["raw_vector"].apply(lambda x: ' '.join(x)).values.astype('U')
tf_idfs = vectorizer.fit_transform(raw_vectors)
vectors = tf_idfs.toarray()
#配列をカンマ区切りの文字列に変換
vectors = [','.join(map(str, vector)) for vector in vectors]

#結果をdfに追記
df["vector"] = vectors

# csvファイルに保存
df.to_csv(output_file_path, index=False)

#各ベクトルに対応する単語を取得
feature_names = vectorizer.get_feature_names_out()
#それもcsvファイルに出力
pd.DataFrame(feature_names).to_csv(features_file_path, index=False)