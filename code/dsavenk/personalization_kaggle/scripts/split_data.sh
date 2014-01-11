
# # test 27
# awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) == 27) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/train_27
# awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) != 27) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/test_27~
# ./prepare_test ~/ir/data/yandex_personalized/splits_days/train_27 ~/ir/data/yandex_personalized/splits_days/test_27~ ~/ir/data/yandex_personalized/splits_days/test_27 &

# # test 26
# awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) == 26) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/train_26
# awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) != 26) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/test_26~
# ./prepare_test ~/ir/data/yandex_personalized/splits_days/train_26 ~/ir/data/yandex_personalized/splits_days/test_26~ ~/ir/data/yandex_personalized/splits_days/test_26 &

# # test 24
# awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) == 24) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/train_24
# awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) != 24) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/test_24~
# ./prepare_test ~/ir/data/yandex_personalized/splits_days/train_24 ~/ir/data/yandex_personalized/splits_days/test_24~ ~/ir/data/yandex_personalized/splits_days/test_24 &

# # test 23
# awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) == 23) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/train_23
# awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) != 23) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/test_23~
# ./prepare_test ~/ir/data/yandex_personalized/splits_days/train_23 ~/ir/data/yandex_personalized/splits_days/test_23~ ~/ir/data/yandex_personalized/splits_days/test_23 &

# # test 22
# awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) == 22) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/train_22
# awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) != 22) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/test_22~
# ./prepare_test ~/ir/data/yandex_personalized/splits_days/train_22 ~/ir/data/yandex_personalized/splits_days/test_22~ ~/ir/data/yandex_personalized/splits_days/test_22 &

# # test 21
# awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) == 21) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/train_21
# awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) != 21) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/test_21~
# ./prepare_test ~/ir/data/yandex_personalized/splits_days/train_21 ~/ir/data/yandex_personalized/splits_days/test_21~ ~/ir/data/yandex_personalized/splits_days/test_21 &

# test 25
# awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) == 25) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/train_25
awk -F'\t' 'BEGIN{skip = 0;} {if ($2 == "M") {if (int($3) != 25) skip = 1; else skip = 0;} if (skip==0) print $0;}' ~/ir/data/yandex_personalized/train > ~/ir/data/yandex_personalized/splits_days/test_25~
./prepare_test ~/ir/data/yandex_personalized/splits_days/train_25 ~/ir/data/yandex_personalized/splits_days/test_25~ ~/ir/data/yandex_personalized/splits_days/test_25
rm ~/ir/data/yandex_personalized/splits_days/test_25~
