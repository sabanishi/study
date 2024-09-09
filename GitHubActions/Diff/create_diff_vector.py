import pandas as pd
import json
import sys
import os
import traceback
from RawPatchCommit import RawPatchCommit, RawPatchData, ChunkData

#チャンク毎の変更情報をベクトル化したものを作成する
def create_chunk_data(chunk,is_debug) -> ChunkData:
    positive_lines = [line for line in chunk if line.startswith("+")]
    negative_lines = [line for line in chunk if line.startswith("-")]
    #「\s」がある場所で分割
    positive_words = []
    for line in positive_lines:
        words = line.split()
        #文頭の「+」の文字を削除
        if len(words) > 0:
            words.remove(words[0])
        positive_words.extend(words)
    negative_words = []
    for line in negative_lines:
        words = line.split()
        #文頭の「-」を削除
        if len(words) > 0:
            words.remove(words[0])
        negative_words.extend(words)

    #positive_wordsとnegative_wordsの両方に含まれる単語を両方から削除
    i = 0
    while True:
        if i >= len(positive_words):
            break
        word = positive_words[i]
        if word in negative_words:
            positive_words.remove(word)
            negative_words.remove(word)
        else:
            i+=1

    #ベクトルを作成
    #positive_wordには「+」を、negative_wordには「-」を付与する
    returu_vector = []
    for word in positive_words:
        returu_vector.append(f"+{word}")
    for word in negative_words:
        returu_vector.append(f"-{word}")

    #positive_word,negative_wordのいずれかが空の場合は無効なデータとする
    is_valid = len(positive_words) > 0 and len(negative_words) > 0
    
    if is_debug:
        print("Chunk:")
        for line in chunk:
            print(line)
    name = chunk[0]
    chunk_data = ChunkData(name,is_valid,returu_vector)
    return chunk_data

#ファイルのパッチ情報から、チャンク毎の変更情報をベクトル化したものを作成する
def create_chunk_list(patch,is_debug=False) ->list[list[str]]:
    #patchを行ごとに分割
    returu_vector = patch.split("\n")
    #チャンク毎に分割
    chunks = []
    for line in returu_vector:
        if line.startswith("@@"):
            chunks.append([])
        chunks[-1].append(line)

    #各チャンクに対して、変更された部分のみを取り出したベクトルを作成する
    result = []
    for chunk in chunks:
        chuk_data = create_chunk_data(chunk,is_debug)
        result.append(chuk_data)
    
    return result

args = sys.argv
if len(args) != 5 and len(args) != 6:
    print("Usage: python create_diff_vector.py <input_file_path> <json_folder> <output_file_path> <skip_to>")
    sys.exit()

input_file_path = args[1]
json_folder = os.path.abspath(args[2])
output_file_path = args[3]
first_commit = args[4]

is_debug = False
if len(args) == 6:
    is_debug = True

df = pd.read_csv(input_file_path)

batch_size = 100
buffer_list = []

output_dict = {
    "data" : []
}

i = 0
is_skip = False
length = len(df)
while(True):
    try:
        #全てのコミットログを取得したら終了
        if i >= len(df):
            break

        commit = df.iloc[i]
        i+=1
        commit_hash = commit["commit_hash"]
        if is_skip == False:
            if first_commit == "none" or commit_hash == first_commit:
                is_skip=True
            else:
                print(f"First Skip:({i}件目); {commit_hash}")
                continue

        url = commit["url"]
        #jsonファイルを取得
        file_path = os.path.join(json_folder, f"{commit_hash}.json")
        with open(file_path,mode='r') as f:
            data = json.load(f)
            #全ての変更ファイルの情報を取得
            all_file_info = data["files"]
            commit_data = RawPatchCommit(commit_hash,url)
            for file_info in all_file_info:
                #ファイルの拡張子がymlまたはyamlでない場合はスキップ
                filename = file_info["filename"]
                if not filename.endswith(".yml") and not filename.endswith(".yaml"):
                    continue
                #変更情報を取得
                patch = file_info["patch"]
                chunks = create_chunk_list(patch,is_debug)
                commit_data.add_patch(RawPatchData(filename, chunks))
            print(f"Success({i}/{length}): {commit_hash}")
            buffer_list.append(commit_data.to_json())
            if len(buffer_list) >= batch_size:
                output_dict["data"].extend(buffer_list)
                with open(output_file_path, mode='w') as f:
                    json.dump(output_dict, f, indent=4)
                buffer_list = []

    except Exception as e:
        print(traceback.format_exc())
        continue

if len(buffer_list) > 0:
    output_dict["data"].extend(buffer_list)
    with open(output_file_path, mode='w') as f:
        json.dump(output_dict, f, indent=4)