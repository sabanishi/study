import sys

args = sys.argv

file_name_1=args[1]
file_name_2=args[2]
file_name_3=args[3]
output_file_name=args[4]

lines1 = []
lines2 = []

with open(file_name_1) as f:
    lines1 = f.readlines()

with open(file_name_2) as f:
    lines2 = f.readlines()

with open(file_name_3) as f:
    lines3 = f.readlines()

# 全てのファイルに含まれる行を取得
lines = []
for line1 in lines1:
    if line1 in lines2 and line1 in lines3:
        lines.append(line1)

# 出力
with open(output_file_name, mode='w') as f:
    for line in lines:
        f.write(line)

for line in lines:
    print(line, end='')