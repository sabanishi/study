process (){
  path="${@:1}"
  python watch_rq2.py $path/match_all/match_log.csv $path/all.txt  
  python watch_rq2.py $path/match_none/match_log.csv $path/none.txt  
  python watch_rq2.py $path/match_my/match_log.csv $path/my.txt  

  python watch_rq2_2.py $path/none.txt $path/all.txt $path/none_all.txt
  python watch_rq2_2.py $path/all.txt $path/none.txt $path/all_none.txt
  python watch_rq2_2.py $path/my.txt $path/all.txt $path/my_all.txt
  python watch_rq2_2.py $path/all.txt $path/my.txt $path/all_my.txt
  python watch_rq2_2.py $path/none.txt $path/my.txt $path/none_my.txt
  python watch_rq2_2.py $path/my.txt $path/none.txt $path/my_none.txt
}

process "hutool_3"