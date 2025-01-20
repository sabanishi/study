import pandas as pd
import sys

class Tuple:
  def __init__(self,hash):
    self.hash = hash
    self.count = 1
  
  def add_count(self):
    self.count += 1
  
  def get_hash(self):
    return self.hash

args = sys.argv

file_name=args[1]
output_file_name=args[2]

df = pd.read_csv(file_name)

# 2列目の値を抽出
hashes = df.iloc[:,2].values

list = []
# どのhashが何個あるかをカウント
for i in range(len(hashes)):
  hash = hashes[i]
  is_exist = False
  for j in range(len(list)):
    if list[j].get_hash() == hash:
      list[j].add_count()
      is_exist = True
      break
  
  if not is_exist:
    list.append(Tuple(hash))

# 結果を数順にソート
list.sort(key=lambda x: x.count, reverse=True)

for i in range(len(list)):
  print(f"{list[i].get_hash()}:{list[i].count}")

# 結果をファイルに書き込む
with open(output_file_name, mode='w') as f:
  for i in range(len(list)):
    f.write(f"{list[i].get_hash()}:{list[i].count}\n")