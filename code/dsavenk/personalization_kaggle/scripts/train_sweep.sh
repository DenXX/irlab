#!/bin/bash

for iter in 200 300 400 500 700
do
    for subsample in 0.3 0.5 0.7 1.0
    do
        for height in 3 4 5
        do
            for rate in 0.02 0.05 0.1 0.2
            do
                eval "python train.py ~/ir/data/yandex_personalized/splits_days2/feats_27_head ~/ir/data/yandex_personalized/splits_days2/sweep/model_27_"$iter"_"$subsample"_"$height"_"$rate"_ls ~/ir/data/yandex_personalized/splits_days2/feats_27_tail ~/ir/data/yandex_personalized/splits_days2/sweep/prediction_27_"$iter"_"$subsample"_"$height"_"$rate" $iter $subsample $height $rate ls > /dev/null &"
            done
            wait
            for rate in 0.02 0.05 0.1 0.2
            do
                echo "NDCG for iter=$iter subsample=$subsample height=$height rate=$rate"
                eval "python ndcg.py ~/ir/data/yandex_personalized/splits_days2/feats_27_tail ~/ir/data/yandex_personalized/splits_days2/sweep/prediction_27_"$iter"_"$subsample"_"$height"_"$rate" "
            done
        done
    done
done

# python train.py ~/ir/data/yandex_personalized/splits_days2/feats_27_head ~/ir/data/yandex_personalized/splits_days2/model_27_200_0.5_5_0.1_ls ~/ir/data/yandex_personalized/splits_days2/feats_test ~/ir/data/yandex_personalized/splits_days2/prediction_test_200_0.5_5_0.1_ls 200 0.5 5 0.1 ls