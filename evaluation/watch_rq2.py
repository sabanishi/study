import pandas as pd

class Tuple:
  def __init__(self,hash):
    self.hash = hash
    self.count = 1
  
  def add_count(self):
    self.count += 1
  
  def get_hash(self):
    return self.hash

file_name='match_folder/match/match_log.csv'

df = pd.read_csv(file_name)

# 2列目の値を抽出
hashes = df["commit"].values.astype('U')

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