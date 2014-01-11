
awk -F',' '{print $4","$5","$6","$7; }' ~/ir/data/yandex_personalized/splits_days2/feats_new_test > ~/ir/data/yandex_personalized/splits_days2/feats_new_test~
paste -d',' ~/ir/data/yandex_personalized/splits_days2/feats_test ~/ir/data/yandex_personalized/splits_days2/feats_new_test~ > ~/ir/data/yandex_personalized/splits_days2/feats_new_test.txt
rm ~/ir/data/yandex_personalized/splits_days2/feats_new_test~
# awk -F',' '{print $4","$5","$6","$7; }' ~/ir/data/yandex_personalized/splits_days2/feats_new_27 > ~/ir/data/yandex_personalized/splits_days2/feats_new_27~
# paste -d',' ~/ir/data/yandex_personalized/splits_days2/feats_27 ~/ir/data/yandex_personalized/splits_days2/feats_new_27~ > ~/ir/data/yandex_personalized/splits_days2/feats_new_27.txt
# rm ~/ir/data/yandex_personalized/splits_days2/feats_new_27~
# awk -F',' '{print $3","$4","$5","$6; }' ~/ir/data/yandex_personalized/splits_days2/feats_new_26 > ~/ir/data/yandex_personalized/splits_days2/feats_new_26~
# paste -d',' ~/ir/data/yandex_personalized/splits_days2/feats_26 ~/ir/data/yandex_personalized/splits_days2/feats_new_26~ > ~/ir/data/yandex_personalized/splits_days2/feats_new_26.txt
# rm ~/ir/data/yandex_personalized/splits_days2/feats_new_26~
# awk -F',' '{print $3","$4","$5","$6; }' ~/ir/data/yandex_personalized/splits_days2/feats_new_25 > ~/ir/data/yandex_personalized/splits_days2/feats_new_25~
# paste -d',' ~/ir/data/yandex_personalized/splits_days2/feats_25 ~/ir/data/yandex_personalized/splits_days2/feats_new_25~ > ~/ir/data/yandex_personalized/splits_days2/feats_new_25.txt
# rm ~/ir/data/yandex_personalized/splits_days2/feats_new_25~
# awk -F',' '{print $3","$4","$5","$6; }' ~/ir/data/yandex_personalized/splits_days2/feats_new_24 > ~/ir/data/yandex_personalized/splits_days2/feats_new_24~
# paste -d',' ~/ir/data/yandex_personalized/splits_days2/feats_24 ~/ir/data/yandex_personalized/splits_days2/feats_new_24~ > ~/ir/data/yandex_personalized/splits_days2/feats_new_24.txt
# rm ~/ir/data/yandex_personalized/splits_days2/feats_new_24~
# awk -F',' '{print $3","$4","$5","$6; }' ~/ir/data/yandex_personalized/splits_days2/feats_new_23 > ~/ir/data/yandex_personalized/splits_days2/feats_new_23~
# paste -d',' ~/ir/data/yandex_personalized/splits_days2/feats_23 ~/ir/data/yandex_personalized/splits_days2/feats_new_23~ > ~/ir/data/yandex_personalized/splits_days2/feats_new_23.txt
# rm ~/ir/data/yandex_personalized/splits_days2/feats_new_23~
# awk -F',' '{print $3","$4","$5","$6; }' ~/ir/data/yandex_personalized/splits_days2/feats_new_22 > ~/ir/data/yandex_personalized/splits_days2/feats_new_22~
# paste -d',' ~/ir/data/yandex_personalized/splits_days2/feats_22 ~/ir/data/yandex_personalized/splits_days2/feats_new_22~ > ~/ir/data/yandex_personalized/splits_days2/feats_new_22.txt
# rm ~/ir/data/yandex_personalized/splits_days2/feats_new_22~