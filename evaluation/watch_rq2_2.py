import sys

args = sys.argv

file_name_1=args[1]
file_name_2=args[2]
output_file_name=args[3]

lines1 = []
lines2 = []

with open(file_name_1) as f:
    lines1 = f.readlines()

with open(file_name_2) as f:
    lines2 = f.readlines()

# lines1に含まれており、lines2に含まれていない行を抽出
lines = [line for line in lines1 if line not in lines2]

with open(output_file_name, mode='w') as f:
    f.writelines(lines)

for line in lines:
    print(line, end='')