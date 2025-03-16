processes (){
  file1=$2
  num1=$3
  file2=$4
  num2=$5

  echo "$file1"
  echo "$num1"
  echo "$file2"
  echo "$num2"

  python watch_rq2.py "$match_$file1" "$num1" "$folderes/match_$file2" "$num2" "$folderes/{$file1}_{$file2}.txt"
  python watch_rq2.py "$match_$file2" "$num2" "$folderes/match_$file1" "$num1" "$folderes/{$file2}_{$file1}.txt"
}

count_file(){
  file=$2
  count=$(find "$file" -type f | wc -l)
  echo "$count"
}

a_count=(($(count_file "match_all")-1)/4)
n_count=(($(count_file "match_none")-1)/4)
m_count=(($(count_file "match_my")-1)/4)

processes all a_count none n_count
processes all a_count my m_count
processes none n_count my m_count