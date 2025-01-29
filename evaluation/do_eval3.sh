processes (){
  folderes=$1
  file1=$2
  num1=$3
  file2=$4
  num2=$5
  python watch_rq2_4.py "$folderes/match_$file1" "$num1" "$folderes/match_$file2" "$num2" "$folderes/{$file1}_{$file2}.txt"
  python watch_rq2_4.py "$folderes/match_$file2" "$num2" "$folderes/match_$file1" "$num1" "$folderes/{$file2}_{$file1}.txt"
}

processes junit_2 all 479 none 445
processes junit_2 all 479 my 476
processes junit_2 none 445 my 476