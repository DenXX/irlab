#!/bin/bash

#FEATS=3760
FEATS=165
DEPTH=5
ITER=500
RATE=0.05

/home/dsavenk/ir/src/pgbrt/bin/run.sh $1 $2 $FEATS $DEPTH $ITER $RATE
/home/dsavenk/ir/src/pgbrt/bin/test <$3 >$4
#paste $3 /home/dsavenk/ir/src/pgbrt/tmp | sort -k2gr | awk '{print $1}' > /home/dsavenk/ir/src/pgbrt/tmp2
#echo "Train AUC:"
#python /home/dsavenk/ir/proj/switchpred/src/auc.py /home/dsavenk/ir/src/pgbrt/tmp2
#rm /home/dsavenk/ir/src/pgbrt/tmp*

#echo "Creating submission file"
#/home/dsavenk/ir/src/pgbrt/bin/test < $4 > $5
#paste /home/dsavenk/ir/proj/switchpred/data/working/test_25_27_session_ids.txt $5 | sort -k2gr | awk '{print $1}' > /home/dsavenk/ir/src/pgbrt/tmp2
#echo "Validation AUC:"
#python /home/dsavenk/ir/proj/switchpred/src/auc.py /home/dsavenk/ir/src/pgbrt/tmp2
#rm /home/dsavenk/ir/src/pgbrt/tmp*
