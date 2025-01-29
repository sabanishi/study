import sys
import os

args = sys.argv

folder_name_1=args[1]
file_n_1 = args[2]
folder_name_2=args[3]
file_n_2 = args[4]

output_file_name=args[5]

print(folder_name_1)
print(file_n_1)
print(folder_name_2)
print(file_n_2)

lines1 = []
lines2 = []

#file_n_1をint型に変換
n_1 = int(file_n_1)
n_2 = int(file_n_2)

# ファイルの読み込み
for i in range(0, n_1+1):
    file_name = folder_name_1 + '/' + str(i) + '_diff.txt'
    with open(file_name) as f:
        lines1.append(f.readlines())

for i in range(0, n_2+1):
    file_name = folder_name_2 + '/' + str(i) + '_diff.txt'
    with open(file_name) as f:
        lines2.append(f.readlines()[1:])

# line1に含まれ、line2に含まれない行を取得
lines = []
for line1 in lines1:
    #2行目以降を比較する
    line_n = line1[1:]
    if line_n not in lines2:
        lines.append(line1)

# 出力
with open(output_file_name, mode='w') as f:
    for line in lines:
        f.write(''.join(line))
    f.write('\n')

for line in lines:
    print(line, end='\n')