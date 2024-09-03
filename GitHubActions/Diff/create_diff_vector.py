import pandas as pd
import json
import sys

def create_vector(patch):
    #patchを行ごとに分割
    lines = patch.split("\n")
    #変更された行のみを取得
    positive_lines = [line for line in lines if line.startswith("+")]
    negative_lines = [line for line in lines if line.startswith("-")]

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

    print("")
    print("")
    #positive_wordsとnegative_wordsの両方に含まれる単語を両方から削除
    i = 0
    while True:
        if i >= len(positive_words):
            break
        word = positive_words[i]
        if word in negative_words:
            print("delete pos:"+word)
            positive_words.remove(word)
            negative_words.remove(word)
        else:
            i+=1

    return_lines = []
    for word in positive_words:
        return_lines.append(f"+{word}")
    for word in negative_words:
        return_lines.append(f"-{word}")
    return return_lines

args = sys.argv
if len(args) != 5:
    print("Usage: python create_diff_vector.py <input_file_path> <json_folder> <output_file_path> <skip_to>")
    sys.exit()

input_file_path = args[1]
json_folder = args[2]
output_file_path = args[3]
first_commit = args[4]

df = pd.read_csv(input_file_path)

batch_size = 100
buffer_dict = {
    "data" : []
}

output_dict = {
    "data" : []
}

i = 0
is_skip = False
while(True):
    try:
        #全てのコミットログを取得したら終了
        if i >= len(df):
            break

        commit = df.iloc[i]
        i+=1
        commit_hash = commit["commit_hash"]
        if first_commit == "none" or commit_hash == first_commit:
            is_skip=True
        else:
            print(f"First Skip:({i}件目); {commit_hash}")
            continue

        url = commit["url"]
        #jsonファイルを取得
        with open(f"{json_folder}/{commit_hash}.json") as f:
            data = json.load(f)
            #全ての変更ファイルの情報を取得
            all_file_info = data["files"]
            json_data = {
                "commit_hash" : commit_hash,
                "url" : url,
                "patch" : []
            }
            for file_info in all_file_info:
                #ファイルの拡張子がymlまたはyamlでない場合はスキップ
                filename = file_info["filename"]
                if not filename.endswith(".yml") and not filename.endswith(".yaml"):
                    continue
                #変更情報を取得
                patch = file_info["patch"]
                vector = create_vector(patch)
                json_data["patch"].append({
                    "filename" : filename,
                    "vector" : vector
                })
            buffer_dict["data"].append(json_data)
            if len(buffer_dict["data"]) >= batch_size:
                output_dict["data"].extend(buffer_dict["data"])
                with open(output_file_path, mode='w') as f:
                    json.dump(output_dict, f, indent=4)
                buffer_dict["data"] = []

    except Exception as e:
        print(f"Error: {commit_hash}, {e}")
        continue

if len(buffer_dict["data"]) > 0:
    output_dict["data"].extend(buffer_dict["data"])
    with open(output_file_path, mode='w') as f:
        json.dump(output_dict, f, indent=4)