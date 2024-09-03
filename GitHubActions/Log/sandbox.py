import pandas as pandas

input_file = "resources/commit_log_no_bot.csv"

df = pandas.read_csv(input_file)

print(len(df))